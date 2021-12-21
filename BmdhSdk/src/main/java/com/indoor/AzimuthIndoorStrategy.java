package com.indoor;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.google.gson.Gson;
import com.indoor.data.DataInjection;
import com.indoor.data.SDKRepository;
import com.indoor.data.entity.author.AuthorData;
import com.indoor.data.http.NetworkUtil;
import com.indoor.data.http.ResultCodeUtils;
import com.indoor.position.IPSMeasurement;
import com.indoor.position.IndoorPositionService;
import com.indoor.utils.KLog;
import com.indoor.utils.RxEncryptTool;
import com.indoor.utils.RxFileUtils;
import com.indoor.utils.Utils;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@RequiresApi(api = Build.VERSION_CODES.O)
public class AzimuthIndoorStrategy {
    private static final String TAG = "BmdhIndoorStrategy";

    private static final int MIN_FILENAME_LENGTH = 8;
    private static final int AREA_ID_LENGTH = 38;//合法的区域id的长度
    private static final boolean IS_NEED_SCRIPT = true;
    private volatile static String areaID = "14710233974744268811639554282911310115";
    private static final String FOLDER_NAME_MAPDATA = "mapconfig";
    private static String MAPCONFIG_FOLDER_PATH;
    //    private static String CONFIG_ASSET_NAME = "440312";
    private Context mContext;
    private volatile boolean mBound = false;
    private volatile boolean mVerifySucess = true;
    private volatile boolean mIsIndoor = false;
    private volatile boolean mIsOffLine = true;
    private volatile String mCureentSetAreaId = "0";//当前接收到的区域ID
    private IPSMeasurement.Callback mCallback;
    private MapConfigData mMapConfigNet;
    private SDKRepository mSdkRepository;
    private IPSMeasurement mRecordIpsMeasurement = null;
    private IndoorPositionService mIndoorPositionService = null;
    private IAzimuthNaviManager.INaviIndoorStateChangeListener mINaviIndoorStateChangeListener = null;

    public AzimuthIndoorStrategy(Context context) {
        mContext = context;
        MAPCONFIG_FOLDER_PATH = getMapConfigPath();
        RxFileUtils.createOrExistsDir(MAPCONFIG_FOLDER_PATH);
        mSdkRepository = DataInjection.provideDemoRepository(context);
    }

    public boolean ismBound() {
        return mBound;
    }

    public void setmBound(boolean mBound) {
        this.mBound = mBound;
    }

    public boolean ismIsIndoor() {
        return mIsIndoor;
    }

    public void setmIsIndoor(boolean mIsIndoor) {
        this.mIsIndoor = mIsIndoor;
    }

    public void setIsOffLine(boolean isOffLine) {
        this.mIsOffLine = isOffLine;
    }

    /**
     * 判断区域ID是否合法
     *
     * @param areaId
     * @return
     */
    public boolean isAreaIdLegal(String areaId) {
        if (TextUtils.isEmpty(areaId)) {
            return false;
        }
        return areaId.length() == AREA_ID_LENGTH;
    }


    public void setIndoorOrOutdoorChangedListener(@NotNull IAzimuthNaviManager.INaviIndoorStateChangeListener listener) {
        mINaviIndoorStateChangeListener = listener;
    }

    public void verifySDK(IAzimuthNaviManager.IInitSDKListener iInitSDKListener) {
        String key = mSdkRepository.getApiKey();
        KLog.d(TAG, " APIkey == " + key);
        String packageName = mSdkRepository.getPackageName();
        String shaCode = mSdkRepository.getShaCode();
        KLog.e(TAG, "shaCode is: " + shaCode);
        if (TextUtils.isEmpty(key) || TextUtils.isEmpty(packageName) || TextUtils.isEmpty(shaCode)) {
            KLog.e(TAG, "apikey or packageName or shaCode cannot be null");
            iInitSDKListener.initFailed("apikey or packageName or shaCode cannot be null");
            mVerifySucess = false;
            return;
        }
        String apikey = RxEncryptTool.encryptMD5ToString(packageName + shaCode, mSdkRepository.getSalt());
        KLog.d(TAG, "apikey is " + apikey);
        if (!key.equals(apikey)) {
            KLog.e(TAG, "apikey is Error");
            iInitSDKListener.initFailed("apikey is Error");
            mVerifySucess = false;
            return;
        }
        mSdkRepository.set3DesSalt();
        if (!NetworkUtil.isNetworkAvailable(Utils.getContext())) {
            iInitSDKListener.initSuccess(ResultCodeUtils.RESULTCODE.SUCCESS);
            mVerifySucess = true;
        } else {
            AuthorData authorData = new AuthorData();
            authorData.setPackageName(packageName);
            authorData.setShaCode(shaCode);
            authorData.setApiKey(key);
            mSdkRepository.setAuthorData(authorData);
            mSdkRepository.verrifySDK(authorData, new IAzimuthNaviManager.IInitSDKListener() {
                @Override
                public void initStart() {
                    iInitSDKListener.initStart();
                }

                @Override
                public void initSuccess(String code) {
                    iInitSDKListener.initSuccess(code);
                    if (!ResultCodeUtils.isRequestOptionSuccess(code)) {
                        setIsOffLine(true);
                    } else {
                        setIsOffLine(false);
                    }
                    mVerifySucess = true;
                }

                @Override
                public void initFailed(String message) {
                    iInitSDKListener.initFailed(message);
                    mVerifySucess = false;
                }
            });
        }


    }

    public MapConfigData getMapConfigDate(Context context, String areaId) {
        MapConfigData result = null;
        if (TextUtils.isEmpty(areaId) || areaId.length() < MIN_FILENAME_LENGTH) {
            KLog.e(TAG, "getMapConfig failed,TextUtils.isEmpty(areaId)||areaId.length()<8...");
            return null;
        }
        if (context == null) {
            KLog.e(TAG, "getMapConfig failed,you should init SDK first...");
            return null;
        }
        try {
            String areaFilePath = "";
            String fileName = "";
            copyAndDeleteMapConfig();
            File fileAreaCode = getAreaCodeLocalFile(areaId);
            boolean isHasLocalFile = false;
            if (fileAreaCode != null) {
                isHasLocalFile = true;
                areaFilePath = fileAreaCode.getPath();
                fileName = fileAreaCode.getName();
            }
            if (!isHasLocalFile) {
                KLog.e(TAG, "getMapConfig failed,no such config file:" + fileName);
                return null;
            }
            result = readMapConfigData(areaFilePath, areaId);
            return result;
        } catch (Exception e) {
            KLog.e(TAG, e.getMessage());
        }
        return result;
    }

    /**
     * 删掉非法的配置文件
     *
     * @param fileArray
     */
    private List<File> deleteIllegalLocalFiles(List<File> fileArray) {
        List<File> result = fileArray;
        for (File file : fileArray) {
            if (isFileNameLegal(file.getName())) {
                RxFileUtils.deleteFile(file);
                result.remove(file);
            }
        }
        return result;
    }

    /**
     * 配置文件名是否合法
     *
     * @param fileName
     * @return
     */
    private boolean isFileNameLegal(String fileName) {
        return !(TextUtils.isEmpty(getFileAreaCode(fileName)) || getFileAreaVersion(fileName) == 0);
    }

    private void copyAndDeleteMapConfig() {
        List<File> localFiles = RxFileUtils.listFilesInDir(MAPCONFIG_FOLDER_PATH);
        List<String> assetFileNames = RxFileUtils.getAssertsFiles(Utils.getContext());
        for (String fileName : assetFileNames) {
            if (fileName.length() < MIN_FILENAME_LENGTH) {
                continue;
            }
            String areaFilePath = MAPCONFIG_FOLDER_PATH + File.separator + fileName;
            if (localFiles == null || localFiles.size() == 0 || isNeedCopyAreaCodeFile(new File(areaFilePath))) {
                InputStream in = null;
                try {
                    in = mContext.getResources().getAssets().open(fileName);
                    RxFileUtils.copyFile(in, new File(areaFilePath));
                } catch (IOException e) {
//                    e.printStackTrace();
                    if (in == null) {
                        KLog.e(TAG, "getMapConfig failed,no such asset file:" + e.getMessage());
                        return;
                    }
                }

            }
        }
    }

    private boolean isNeedCopyAreaCodeFile(File compareFile) {
        boolean result = false;
        List<File> localFiles = RxFileUtils.listFilesInDir(MAPCONFIG_FOLDER_PATH);
        for (File f : localFiles) {
            if (!isFileNameLegal(f.getName())) {
                KLog.e(TAG, "local file name is illegal,so delete...");
                RxFileUtils.deleteFile(f);
                continue;
            }
            if (getFileAreaCode(f.getName()).equals(getFileAreaCode(compareFile.getName()))
                    && getFileAreaVersion(f.getName()) < getFileAreaVersion(compareFile.getName())) {
                RxFileUtils.deleteFile(f);
                result = true;
                break;
            }
        }
        return result;
    }

    private String getFileAreaCode(String fileName) {
        if (TextUtils.isEmpty(fileName) || fileName.split("_").length != 2 || fileName.split("_")[0].length() != 6) {
            return "";
        }
        return fileName.split("_")[0];
    }

    private int getFileAreaVersion(String fileName) {
        if (TextUtils.isEmpty(fileName) || fileName.split("_").length != 2) {
            return 0;
        }
        int version = 0;
        try {
            version = Integer.parseInt(fileName.split("_")[1]);
        } catch (Exception e) {
            KLog.e(TAG, "getFileAreaVersion fileName is " + fileName + ",Exception:" + e.getMessage());
            return 0;
        }
        return version;

    }

    private File getAreaCodeLocalFile(String areaId) {
        File result = null;
        List<File> localFiles = RxFileUtils.listFilesInDir(MAPCONFIG_FOLDER_PATH);
        File tempFile = null;
        for (File file : localFiles) {
            if (!isFileNameLegal(file.getName())) {
                KLog.e(TAG, "getAreaCodeLocalFile,local file name is illegal,so delete...");
                RxFileUtils.deleteFile(file);
                continue;
            }
            if (file.getName().startsWith(getAreaCode(areaId))) {
                if (tempFile == null) {
                    tempFile = file;
                    result = file;
                } else {
                    if (getFileAreaVersion(file.getName()) > getFileAreaVersion(tempFile.getName())) {
                        result = file;
                        tempFile.delete();
                    } else {
                        result = tempFile;
                        file.delete();
                    }
                }
            }
        }
        return result;
    }


    /**
     * 以行为单位读取文件，常用于读面向行的格式化文件
     */
    public MapConfigData readMapConfigData(String filePath, String areaId) {
        MapConfigData result = null;
        File file = new File(filePath);
        BufferedReader reader = null;
        Gson gson = new Gson();
        try {
            System.out.println("以行为单位读取文件内容，一次读一整行：");
            reader = new BufferedReader(new FileReader(file));
            String tempString = null;
            int line = 1;
            // 一次读入一行，直到读入null为文件结束
            while ((tempString = reader.readLine()) != null) {
                // 显示行号
                System.out.println("line?????????????????????????????????? " + line + ": " + tempString);
                String jsonStr = tempString;
                MapConfigData temp = gson.fromJson(jsonStr, MapConfigData.class);
                if (temp != null && temp.getProjectAreaId().equals(areaId)) {
                    result = temp;
                    break;
                }
                line++;
            }

            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                }
            }
        }
        return result;
    }

    /**
     * 销毁当前SDK资源
     */
    public void clearDataAndExit() {
        KLog.d(TAG, "clearDataAndExit...");
        mSdkRepository.saveUserActionDataToDB(mRecordIpsMeasurement);
        mSdkRepository.destroyInstance();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void setAreaId(String areaId) {
        initAreaConfig(areaId);
    }

    /**
     * 启动室内定位服务
     *
     * @param callback
     */
    public void startIndoorSdkLocate(IPSMeasurement.Callback callback) {
        if (!isAuthorSucess()) {
            Log.e(TAG, "verify failed,startIndoorSdkLocate failed...");
            return;
        }
        mCallback = callback;
        Intent intent = new Intent(mContext, IndoorPositionService.class);
        boolean status = mContext.bindService(intent, connection, Context.BIND_AUTO_CREATE);
        KLog.d(TAG, "Service start status is " + status + ";mapConfigNet is null==" + (mMapConfigNet == null));
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void initAreaConfig(String areaId) {
        if (!isAreaIdLegal(areaId)) {
            KLog.e(TAG, "AreaId is error...");
            return;
        }
        mSdkRepository.saveAreaId(areaId);
//        mapConfig = getMapConfig(mContext, getAreaCode(areaId));
        mMapConfigNet = getMapConfigDate(mContext, areaId);//For Net Test,I will use it later
        if (mMapConfigNet == null) {
            KLog.e(TAG, "MapConfig is null,please ensure that the SDK is operating properly according to the ,areaId is " + areaId);
            return;
        } else {
            KLog.d(TAG, "initAreaConfig success...");
            if (mIndoorPositionService != null) {
                mIndoorPositionService.setIpsDefualtAreaId(areaID);
            }
            areaID = areaId;
            updateMapConfig(areaId);
        }
    }

    public static String getDefaultAreaId() {
        return areaID;
    }

    /**
     * 更新当前AreaId对应的区域配置文件
     */
    public void refreshCurrentAreaConfig() {
        if (!isAreaIdLegal(mCureentSetAreaId)) {
            KLog.e(TAG, "mCureentSetAreaId is illegal,no need to update...");
            return;
        }
        refreshAreaConfig(mCureentSetAreaId, new IAzimuthNaviManager.IUpdateAreaConfigListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void updateSuccess() {
                initAreaConfig(mCureentSetAreaId);
            }

            @Override
            public void updateError(Throwable e) {
            }

            @Override
            public void updateFailed(String msg) {
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void updateMapConfig(String areaId) {
        if (!mCureentSetAreaId.equals(areaId) || mMapConfigNet == null || !mMapConfigNet.getProjectAreaId().equals(areaId)) {
            KLog.d(TAG, "updateMapConfig and refreshAreaConfig, need initAreaConfig,areaId is " + areaId);
            mCureentSetAreaId = areaId;
            initAreaConfig(areaId);
            refreshCurrentAreaConfig();
        }
        if (mMapConfigNet == null) {
            KLog.e(TAG, "mapConfig==null,data err,Please make sure you have access to indoor map information...");
            return;
        }
        KLog.d(TAG, "updateMapConfig...");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && mIndoorPositionService != null) {
            mIndoorPositionService.setInfoAndStartup(mMapConfigNet, mSdkRepository);
        } else {
            KLog.e(TAG, "current OS Version is too low or IndoorPositionService has not started,Version is " + Build.VERSION.SDK_INT);
        }
    }

    private final ServiceConnection connection = new ServiceConnection() {

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            KLog.e(TAG, "Service connected!");
            IndoorPositionService.LocalBinder binder = (IndoorPositionService.LocalBinder) service;
            mIndoorPositionService = binder.getService();
            AtomicInteger i = new AtomicInteger();
            mIndoorPositionService.setIpsDefualtAreaId(areaID);
            if (mMapConfigNet == null) {
                mIndoorPositionService.setInfoAndStartup(new MapConfigData(), AzimuthIndoorStrategy.this.mSdkRepository);//第一次调用启动定时器需要
            } else {
                KLog.d(TAG, "mapConfigNet != null,so set it");
                mIndoorPositionService.setInfoAndStartup(mMapConfigNet, AzimuthIndoorStrategy.this.mSdkRepository);
            }

            mIndoorPositionService.register(measurement -> {
                i.getAndIncrement();
                String text = "\n result" +
                        "\nx," + measurement.getX() + "," +
                        "y," + measurement.getY() + "\n" +
                        "z=" + measurement.getZ() + "\n" +
                        "vx=" + measurement.getVx() + "\n" +
                        "vy=" + measurement.getVy() + "\n" +
                        "state=" + measurement.getVz() + "\n" +
                        "mapID=" + measurement.getMapID() + "\n" +
                        "Mode=" + measurement.getMode() + "\n" + "定位次数:" + i + "\n" + measurement.getText();
                if (mRecordIpsMeasurement == null) {
                    mSdkRepository.saveUserActionDataToDB(measurement);
                    KLog.d(TAG, "saveUserActionDataToDB,first Record after initSDK...");
                } else {
                    if(!mRecordIpsMeasurement.getMapID().equals(measurement.getMapID())){
                        mSdkRepository.saveUserActionDataToDB(measurement);
                        KLog.d(TAG, "saveUserActionDataToDB,new indoor map...");
                    }
                    if (!mRecordIpsMeasurement.mode.equals(measurement.getMode())) {
                        mINaviIndoorStateChangeListener.onIndoorOrOutdoorChanged(measurement.getMode());
                        mSdkRepository.saveUserActionDataToDB(measurement);
                        KLog.d(TAG, "saveUserActionDataToDB,!mIpsMeasurement.mode.equals(measurement.getMode())");
                    }
                }

                if (mINaviIndoorStateChangeListener != null) {
                    if (mRecordIpsMeasurement == null) {
                        mINaviIndoorStateChangeListener.onIndoorOrOutdoorChanged(measurement.getMode());
                    } else {
                        if (!mRecordIpsMeasurement.mode.equals(measurement.getMode())) {
                            mINaviIndoorStateChangeListener.onIndoorOrOutdoorChanged(measurement.getMode());
                        }
                    }
                }
                mRecordIpsMeasurement = measurement;
                KLog.i(TAG, "result is " + text);

                if (mMapConfigNet != null) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (mCallback != null) {
                            mCallback.onReceive(measurement);
                        }
                    });
                } else {
                    KLog.i(TAG, "mapConfigNet is null,cannot post msg");
                }

//                updateMapConfig(measurement.getMapID(), indoorPositionService);  现在设备端还不能返回areid，都是客户端传入的areid,故更新无意义
            });
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    private boolean isAuthorSucess() {
        if ((mIsOffLine && !mVerifySucess) || (!mIsOffLine && TextUtils.isEmpty(mSdkRepository.getToken()))) {
            Log.e(TAG, "verify failed...");
            return false;
        }
        return true;
    }

    public void refreshAreaConfig(String areaId, IAzimuthNaviManager.IUpdateAreaConfigListener iUpdateAreaConfigListener) {
        if (!isAuthorSucess()) {
            KLog.e(TAG, "refreshAreaConfig failed,... verify failed");
            return;
        }
        //TODO
        //TODO 更新区文件 getAreaCodeLocalFile(areaId).getName();  if(getAreaCodeLocalFile(areaId)==null){return getAareCode(areaId)}
        //mSdkRepository.refreshAreaConfig(new ProjectAreaData(areaId, mapConfig.getVersionNum()), iUpdateAreaConfigListener, true);
    }


    public static String getMapConfigPath() {
        return Utils.getContext().getExternalFilesDir(null) + File.separator + FOLDER_NAME_MAPDATA;
    }

    public static String getAreaCode(String areaId) {
        if (TextUtils.isEmpty(areaId)) {
            return "";
        }
        return areaId.substring(areaId.length() - 6);
    }
}

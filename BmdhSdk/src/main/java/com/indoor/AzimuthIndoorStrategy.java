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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class AzimuthIndoorStrategy {
    private static final String TAG = "BmdhIndoorStrategy";

    private static final boolean IS_NEED_SCRIPT = true;
    private static final String FOLDER_NAME_MAPDATA = "mapconfig";
    private static String MAPCONFIG_FOLDER_PATH;
//    private static String CONFIG_ASSET_NAME = "440312";
    private Context mContext;
    private volatile boolean mBound = false;
    private volatile boolean mVerifySucess = true;
    private volatile boolean mIsIndoor = false;
    private volatile boolean isOffLine = true;
    private IPSMeasurement.Callback mCallback;
    private MapConfigData mapConfigNet;
    private SDKRepository mSdkRepository;
    private IPSMeasurement ipsMeasurement = null;
    private IAzimuthNaviManager.INaviIndoorStateChangeListener iNaviIndoorStateChangeListener = null;

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
        this.isOffLine = isOffLine;
    }


    public void setIndoorOrOutdoorChangedListener(IAzimuthNaviManager.INaviIndoorStateChangeListener listener) {
        iNaviIndoorStateChangeListener = listener;
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
//        String mw = "方位角数据科技有限公司Az#!";
//        String miw = RxEncryptTool.encrypt3DES2Base64(mw, SALT);
//        KLog.d(TAG, "miw is " + miw);
//        String desStr = RxEncryptTool.decryptBase64_3DES(miw, SALT);
//        KLog.d(TAG, "desStr is " + desStr);
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
        if (TextUtils.isEmpty(areaId) || areaId.length() < 6) {
            KLog.e(TAG, "getMapConfig failed,TextUtils.isEmpty(areaId)||areaId.length()<6...");
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
            File fileAreaCode=getAreaCodeLocalFile(areaId);
            boolean isHasLocalFile = false;
            if(fileAreaCode!=null){
                isHasLocalFile = true;
                areaFilePath = fileAreaCode.getPath();
                fileName = fileAreaCode.getName();
            }
            if(!isHasLocalFile){
                KLog.e(TAG, "getMapConfig failed,no such config file:" + fileName);
                return null;
            }
            result=readMapConfigData(areaFilePath, areaId);
            return result;
        } catch (Exception e) {
            KLog.e(TAG, e.getMessage());
        }
        return result;
    }

    private void copyAndDeleteMapConfig(){
        List<File> localFiles = RxFileUtils.listFilesInDir(MAPCONFIG_FOLDER_PATH);
        List<String> assetFileNames = RxFileUtils.getAssertsFiles(Utils.getContext());
        for(String fileName:assetFileNames){
            if(fileName.length()<8){
                continue;
            }
             String  areaFilePath = MAPCONFIG_FOLDER_PATH + File.separator + fileName;
            if(localFiles==null||localFiles.size()==0|| isNeedCopyAreaCodeFile(new File(areaFilePath))){
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

    private boolean isNeedCopyAreaCodeFile(File compareFile){
        boolean result=false;
        List<File> localFiles = RxFileUtils.listFilesInDir(MAPCONFIG_FOLDER_PATH);
        for(File f:localFiles){
            if(getFileAreaCode(f.getName()).equals(getFileAreaCode(compareFile.getName()))
                    &&getFileAreaVersion(f.getName())<getFileAreaVersion(compareFile.getName())){
                RxFileUtils.deleteFile(f);
                result=true;
                break;
            }
        }
        return result;
    }

    private String getFileAreaCode(String fileName){
        if (TextUtils.isEmpty(fileName)) {
            return "";
        }
        return fileName.split("_")[0];
    }

    private int getFileAreaVersion(String fileName){
        if (TextUtils.isEmpty(fileName)|| fileName.split("_").length<2) {
            return 0;
        }
        return Integer.parseInt(fileName.split("_")[1]);
    }

    private File getAreaCodeLocalFile(String areaId){
        File result = null;
        List<File> localFiles = RxFileUtils.listFilesInDir(MAPCONFIG_FOLDER_PATH);
        for (File file : localFiles) {
            if (file.getName().startsWith(getAreaCode(areaId))) {
                result=file;
                break;
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
    public void clearData() {
        mSdkRepository.destroyInstance();
    }

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
        KLog.d(TAG, "Service start status is " + status + ";mapConfigNet is null==" + (mapConfigNet == null));
    }

    private void initAreaConfig(String areaId) {
        mSdkRepository.saveAreaId(areaId);
//        mapConfig = getMapConfig(mContext, getAreaCode(areaId));
        mapConfigNet = getMapConfigDate(mContext, areaId);//For Net Test,I will use it later
        if (mapConfigNet == null) {
            KLog.e(TAG, "MapConfig is null,please ensure that the SDK is operating properly according to the steps");
            return;
        }
    }

    private void updateMapConfig(String areaId, IndoorPositionService indoorPositionService) {
        if (mapConfigNet == null || !mapConfigNet.getProjectAreaId().equals(areaId)) {
            KLog.d(TAG,"updateMapConfig and refreshAreaConfig, need initAreaConfig,areaId is "+areaId);
            initAreaConfig(areaId);
            refreshAreaConfig(areaId, new IAzimuthNaviManager.IUpdateAreaConfigListener() {
                @Override
                public void updateSuccess() {
                    initAreaConfig(areaId);
                }

                @Override
                public void updateError(Throwable e) {
                }

                @Override
                public void updateFailed(String msg) {
                }
            });
        }
        if (mapConfigNet == null) {
            KLog.e(TAG, "mapConfig==null,data err,Please make sure you have access to indoor map information...");
            return;
        }
        KLog.d(TAG, "updateMapConfig...");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            indoorPositionService.setInfoAndStartup(mapConfigNet, mSdkRepository);
        } else {
            KLog.e(TAG, "currentVersion is too low, is " + Build.VERSION.SDK_INT);
        }
    }

    private final ServiceConnection connection = new ServiceConnection() {

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            KLog.e(TAG, "Service connected!");
            IndoorPositionService.LocalBinder binder = (IndoorPositionService.LocalBinder) service;
            IndoorPositionService indoorPositionService = binder.getService();
            AtomicInteger i = new AtomicInteger();
            indoorPositionService.setInfoAndStartup(new MapConfigData(),AzimuthIndoorStrategy.this.mSdkRepository);
            indoorPositionService.register(measurement -> {
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
                if (iNaviIndoorStateChangeListener != null) {
                    if (ipsMeasurement == null) {
                        iNaviIndoorStateChangeListener.onIndoorOrOutdoorChanged(measurement.getMode());
                    } else {
                        if (!ipsMeasurement.mode.equals(measurement.getMode())) {
                            iNaviIndoorStateChangeListener.onIndoorOrOutdoorChanged(measurement.getMode());
                        }
                    }
                }

                KLog.i(TAG, "result is " + text);

                if(mapConfigNet != null){
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (mCallback != null) {
                            mCallback.onReceive(measurement);
                        }
                    });
                }else{
                    KLog.i(TAG, "mapConfigNet is null,cannot post msg");
                }

                updateMapConfig(measurement.getMapID(), indoorPositionService);
            });
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    private boolean isAuthorSucess() {
        if ((isOffLine && !mVerifySucess) || (!isOffLine && TextUtils.isEmpty(mSdkRepository.getToken()))) {
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
        MapConfigData mapConfig = getMapConfigDate(mContext, areaId);
        if (mapConfig == null) {
            KLog.e(TAG, "refreshAreaConfig failed ,mapConfig is null,areaId is " + areaId);
            return;
        }

        //TODO 更新区文件 getAreaCodeLocalFile(areaId).getName();
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

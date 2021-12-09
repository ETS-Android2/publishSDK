package com.indoor;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
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
import com.indoor.data.entity.projectareo.ProjectAreaData;
import com.indoor.data.http.HttpStatus;
import com.indoor.position.IPSMeasurement;
import com.indoor.position.IndoorPositionService;
import com.indoor.utils.KLog;
import com.indoor.utils.RxAppUtils;
import com.indoor.utils.RxEncryptTool;
import com.indoor.utils.RxFileTool;
import com.indoor.utils.RxFileUtils;
import com.indoor.utils.Utils;

import java.io.File;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;

public class AzimuthIndoorStrategy {
    private static final String TAG = "BmdhIndoorStrategy";
    private static final String META_DATA = "com.bmdh.indoorsdk.API_KEY";
    private static final String FOLDER_NAME_MAPDATA="mapconfig";
    private static final boolean isOffLine=true;
    private static String MAPCONFIG_FOLDER_PATH;
    private static String CONFIG_ASSET_NAME = "indoor_data";
    private Context mContext;
    private boolean mBound = false;
    private boolean mVerifySucess = false;
    private boolean mIsIndoor = false;
    private IPSMeasurement.Callback mCallback;
    private MapConfig mapConfig;
    private String currentMapConfigID = "";
    private String currentSetMapID ="";
    private MapConfig.DataConfigDTO mCurrentConfig;
    private SDKRepository sdkRepository;
    private IPSMeasurement ipsMeasurement=null;
    private IAzimuthNaviManager.INaviIndoorStateChangeListener iNaviIndoorStateChangeListener=null;

    public AzimuthIndoorStrategy(Context context) {
        mContext = context;
        MAPCONFIG_FOLDER_PATH =getMapConfigPath();
        RxFileUtils.createOrExistsDir(MAPCONFIG_FOLDER_PATH);
        sdkRepository= DataInjection.provideDemoRepository(context);
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


    public void setIndoorOrOutdoorChangedListener(IAzimuthNaviManager.INaviIndoorStateChangeListener listener){
        iNaviIndoorStateChangeListener=listener;
    }

    public void verifySDK(IAzimuthNaviManager.IInitSDKListener iInitSDKListener) {
//        ApplicationInfo appInfo = null;
//        String key = "";
//        try {
//            appInfo = mContext.getPackageManager().getApplicationInfo(mContext.getPackageName(), PackageManager.GET_META_DATA);
//            key =appInfo.metaData.getString(META_DATA);
//        } catch (Exception e) {
//            KLog.e(TAG, "get meta-data error:"+e.getMessage());
//            return;
//        }
//        KLog.d(TAG, " APIkey == " + key);

//        //TODO 网络请求，获取认证结果
//        String packageName=mContext.getPackageName();
//        String shaCode=RxAppUtils.getAppSignatureSHA1(mContext);
//        if(TextUtils.isEmpty(key)||TextUtils.isEmpty(packageName)||TextUtils.isEmpty(shaCode)){
//            KLog.e(TAG, "apikey or packageName or shaCode cannot be null");
//            iInitSDKListener.initFailed( HttpStatus.STATUS_INIT_FAILED,"apikey or packageName or shaCode cannot be null");
//            mVerifySucess=false;
//            return;
//        }
//
//        if(!key.equals(RxEncryptTool.encryptMD5ToString(shaCode+packageName,key))){
//            KLog.e(TAG, "apikey is Error");
//            iInitSDKListener.initFailed(HttpStatus.STATUS_INIT_FAILED,"apikey is Error");
//            mVerifySucess=false;
//            return;
//        }
        if(isOffLine){
            iInitSDKListener.initSuccess();
            mVerifySucess=true;
        }
//        else{
//            AuthorData authorData=new AuthorData();
//            authorData.setPackageName(packageName);
//            authorData.setShaCode(shaCode);
//            authorData.setApiKey(key);
//            sdkRepository.verrifySDK(authorData, new IAzimuthNaviManager.IInitSDKListener() {
//                @Override
//                public void onAuthResult(int code, String message) {
//                    iInitSDKListener.onAuthResult(code,message);
//                }
//
//                @Override
//                public void initStart() {
//                    iInitSDKListener.initStart();
//                }
//
//                @Override
//                public void initSuccess() {
//                    iInitSDKListener.initSuccess();
//                    mVerifySucess=true;
//                }
//
//                @Override
//                public void initFailed(int code, String message) {
//                    iInitSDKListener.initFailed(code,message);
//                    mVerifySucess=false;
//                }
//            });
//        }


    }

    public MapConfig getMapConfig(Context context, String areaId) {
        MapConfig result = null;
        if(TextUtils.isEmpty(areaId)||areaId.length()<6){
            KLog.e(TAG,"getMapConfig failed,TextUtils.isEmpty(areaId)||areaId.length()<6...");
            return null;
        }
        String jsonStr="";
        Gson gson = new Gson();
        if(context==null){
            KLog.e(TAG,"getMapConfig failed,you should init SDK first...");
            return null;
        }
        try {
            String areaFilePath=MAPCONFIG_FOLDER_PATH+File.separator+getAreaCode(areaId);
            if (!RxFileUtils.isFileExists(areaFilePath)) {
                InputStream in = context.getResources().getAssets().open(CONFIG_ASSET_NAME);
                if(in==null){
                    KLog.e(TAG,"getMapConfig failed,no such asset file:"+CONFIG_ASSET_NAME);
                    return null;
                }
                RxFileUtils.copyFile(in, new File(areaFilePath));
            }
            jsonStr = RxFileUtils.readFile2String(areaFilePath, "UTF-8");
            result=gson.fromJson(jsonStr, MapConfig.class);
            return result;
        } catch (Exception e) {
            KLog.e(TAG,e.getMessage());
        }
        return result;
    }

    /**
     * 销毁当前SDK资源
     */
    public void clearData(){
       sdkRepository.destroyInstance();
    }
    /**
     * 启动室内定位服务
     *
     * @param callback
     */
    public void startIndoorSdkLocate(String mapID, IPSMeasurement.Callback callback) {
        if(!isOffLine&&(!mVerifySucess|| TextUtils.isEmpty(sdkRepository.getToken()))){
            Log.e(TAG,"verify failed...");
            return;
        }
        currentSetMapID = mapID;
        mapConfig = getMapConfig(mContext, "indoor_data");
        if(mapConfig==null){
            KLog.e(TAG,"MapConfig is null,please ensure that the SDK is operating properly according to the steps");
            return;
        }
        mCallback = callback;
        Intent intent = new Intent(mContext, IndoorPositionService.class);
        boolean status = mContext.bindService(intent, connection, Context.BIND_AUTO_CREATE);
        KLog.d(TAG, "Service start status is " + status+";mapConfig is null=="+(mapConfig == null));
    }

    private void updateMapConfig(String mapID, IndoorPositionService indoorPositionService) {

        if (mapConfig == null) {
            KLog.e(TAG, "mapConfig==null,data err...");
            return;
        }
        if (currentMapConfigID.equals(mapID)) {
            return;
        }
        KLog.d(TAG, "updateMapConfig...");
        currentMapConfigID = mapID;
        mCurrentConfig = mapConfig.getDataConfig().get(0);
        for (MapConfig.DataConfigDTO dataConfig : mapConfig.getDataConfig()) {
            if (dataConfig.getMapid().equals(currentMapConfigID)) {
                mCurrentConfig = dataConfig;
                KLog.d(TAG, "updateMapConfig mCurrentConfig...");
                break;
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            indoorPositionService.setInfoAndStartup(mCurrentConfig);
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
            updateMapConfig(currentSetMapID, indoorPositionService);
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
                if(iNaviIndoorStateChangeListener!=null){
                    if(ipsMeasurement==null){
                       iNaviIndoorStateChangeListener.onIndoorOrOutdoorChanged(measurement.getMode());
                    }else{
                        if(!ipsMeasurement.mode.equals(measurement.getMode())){
                            iNaviIndoorStateChangeListener.onIndoorOrOutdoorChanged(measurement.getMode());
                        }
                    }
                }

                KLog.i(TAG, "result is "+text);
                updateMapConfig(measurement.getMapID(), indoorPositionService);
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (mCallback != null) {
                        mCallback.onReceive(measurement);
                    }
                });
            });
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    private MapConfig.DataConfigDTO getDataConfig(String mapid,MapConfig useMapConfig){
        if(TextUtils.isEmpty(mapid)||useMapConfig==null||mapid.length()<6){
            KLog.e(TAG, "getDataConfig failed,extUtils.isEmpty(mapid)||useMapConfig==null||mapid.length()<6");
            return null;
        }
        MapConfig.DataConfigDTO result=null;
        for (MapConfig.DataConfigDTO dataConfig : useMapConfig.getDataConfig()) {
            if (dataConfig.getMapid().equals(currentMapConfigID)) {
                result = dataConfig;
                KLog.d(TAG, "getDataConfig...");
                break;
            }
        }
        return result;
    }

    public void refreshAreaConfig(String areaId) {
        MapConfig mapConfig=getMapConfig(mContext,areaId);
        MapConfig.DataConfigDTO dataConfig=getDataConfig(areaId,mapConfig);
        if(dataConfig==null){
            KLog.e(TAG,"refreshAreaConfig failed ,dataConfig is null,areaId is "+areaId);
            return;
        }
        sdkRepository.refreshAreaConfig(new ProjectAreaData(areaId,dataConfig.getVersionNum()));
    }

    public static String getMapConfigPath(){
        return Utils.getContext().getExternalFilesDir(null)+File.separator+FOLDER_NAME_MAPDATA;
    }

    public static String getAreaCode(String areaId) {
        if(TextUtils.isEmpty(areaId)){
            return "";
        }
        return areaId.substring(areaId.length() - 6);
    }
}

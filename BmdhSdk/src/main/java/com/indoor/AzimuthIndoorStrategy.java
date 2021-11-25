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
import com.indoor.position.IPSMeasurement;
import com.indoor.position.IndoorPositionService;
import com.indoor.utils.RxAppUtils;
import com.indoor.utils.RxFileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicInteger;

public class AzimuthIndoorStrategy {
    private static final String TAG = "BmdhIndoorStrategy";
    private static final String META_DATA = "com.bmdh.indoorsdk.API_KEY";
    private static String MAPCONFIG_PATH;
    private static String CONFIG_NAME = "indoor_data";
    private Context mContext;
    private boolean mBound = false;
    private boolean mVerifySucess = false;
    private boolean mIsIndoor = false;
    private IPSMeasurement.Callback mCallback;
    private MapConfig mapConfig;
    private long currentMapConfigID = 0;
    private long currentSetMapID =0;
    private MapConfig.DataConfigDTO mCurrentConfig;
//    private SDKRepository sdkRepository;
    private IPSMeasurement ipsMeasurement=null;
    private IAzimuthNaviManager.INaviIndoorStateChangeListener iNaviIndoorStateChangeListener=null;

    public AzimuthIndoorStrategy(Context context) {
        mContext = context;
        MAPCONFIG_PATH = context.getExternalCacheDir().getAbsolutePath() + File.separator + CONFIG_NAME;
//        sdkRepository= DataInjection.provideDemoRepository(context);
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
        ApplicationInfo appInfo = null;
        String key = "";
        try {
            appInfo = mContext.getPackageManager().getApplicationInfo(mContext.getPackageName(), PackageManager.GET_META_DATA);
            key =appInfo.metaData.getString(META_DATA);
        } catch (Exception e) {
            Log.e(TAG, "get meta-data error:", e);
            return;
        }
        Log.d(TAG, " APIkey == " + key);
        if(TextUtils.isEmpty(key)){
            Log.e(TAG, "apikey cannot be null");
        }
        //TODO 网络请求，获取认证结果
        AuthorData authorData=new AuthorData();
        authorData.setPackageName(mContext.getPackageName());
        authorData.setShaCode(RxAppUtils.getAppSignatureSHA1(mContext));
        authorData.setAuthCode(key);
//        sdkRepository.verrifySDK(authorData,iInitSDKListener);
        mVerifySucess=true;
    }

    public String getMapConfig(Context context, String fileName) {
        String Result = "";
        try {
            if (!RxFileUtils.isFileExists(MAPCONFIG_PATH)) {
                InputStream in = context.getResources().getAssets().open(CONFIG_NAME);
                InputStreamReader inputReader = new InputStreamReader(in);
                RxFileUtils.copyFile(in, new File(MAPCONFIG_PATH));
                Result = RxFileUtils.readFile2String(MAPCONFIG_PATH, "UTF-8");

            }
            Result = RxFileUtils.readFile2String(MAPCONFIG_PATH, "UTF-8");
            return Result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Result;
    }

    /**
     * 销毁当前SDK资源
     */
    public void clearData(){
//       sdkRepository.destroyInstance();
    }
    /**
     * 启动室内定位服务
     *
     * @param callback
     */
    public void startIndoorSdkLocate(long mapID, IPSMeasurement.Callback callback) {
//        if(!mVerifySucess&& TextUtils.isEmpty(sdkRepository.getToken())){
//            Log.e(TAG,"verify failed...");
//            return;
//        }
        currentSetMapID = mapID;
        Gson gson = new Gson();
        mapConfig = gson.fromJson(getMapConfig(mContext, "indoor_data"), MapConfig.class);
        mCallback = callback;
        Intent intent = new Intent(mContext, IndoorPositionService.class);
        boolean status = mContext.bindService(intent, connection, Context.BIND_AUTO_CREATE);
        Log.d(TAG, "Service start status is " + status+";mapConfig is null=="+(mapConfig == null));
    }

    private void updateMapConfig(long mapID, IndoorPositionService indoorPositionService) {

        if (mapConfig == null) {
            Log.e(TAG, "mapConfig==null,data err...");
            return;
        }
        if (currentMapConfigID==mapID) {
            return;
        }
        Log.d(TAG, "updateMapConfig...");
        currentMapConfigID = mapID;
        mCurrentConfig = mapConfig.getDataConfig().get(0);
        for (MapConfig.DataConfigDTO dataConfig : mapConfig.getDataConfig()) {
            if (dataConfig.getMapid()==currentMapConfigID) {
                mCurrentConfig = dataConfig;
                Log.d(TAG, "updateMapConfig mCurrentConfig...");
                break;
            }
        }
        Log.d(TAG, "mCurrentConfig is null == "+(mCurrentConfig==null));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            indoorPositionService.setInfoAndStartup(mCurrentConfig);
        } else {
            Log.e(TAG, "currentVersion is too low, is " + Build.VERSION.SDK_INT);
        }
        Log.d(TAG, "mCurrentConfig is null == " + (mCurrentConfig == null));
    }

    private final ServiceConnection connection = new ServiceConnection() {

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            Log.e(TAG, "Service connected!");
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

                Log.i(TAG, "result is "+text);
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
}

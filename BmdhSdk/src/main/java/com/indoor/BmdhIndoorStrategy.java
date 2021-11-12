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
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.google.gson.Gson;
import com.indoor.position.IPSMeasurement;
import com.indoor.position.IndoorPositionService;
import com.indoor.utils.RxFileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicInteger;

public class BmdhIndoorStrategy {
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
    private String currentMapConfigID = "";
    private String currentSetMapID = "";
    private MapConfig.DataConfigDTO mCurrentConfig;

    public BmdhIndoorStrategy(Context context) {
        mContext = context;
        MAPCONFIG_PATH = context.getExternalCacheDir().getAbsolutePath() + File.separator + CONFIG_NAME;
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

    public void verifyAPIKey() {
        ApplicationInfo appInfo = null;
        String key = "";
        try {
            appInfo = mContext.getPackageManager().getApplicationInfo(mContext.getPackageName(), PackageManager.GET_META_DATA);
            appInfo.metaData.getString(META_DATA);
        } catch (Exception e) {
            Log.e(TAG, "get meta-data error:", e);
        }
        Log.d(TAG, " APIkey == " + key);
        //TODO 网络请求，获取认证结果
    }

    public String getMapConfig(Context context, String fileName) {
        String Result = "";
        try {
            if (RxFileUtils.isFileExists(MAPCONFIG_PATH)) {
                Result = RxFileUtils.readFile2String(MAPCONFIG_PATH, "UTF-8");
            } else {
                InputStream in = context.getResources().getAssets().open(CONFIG_NAME);
                InputStreamReader inputReader = new InputStreamReader(in);
                RxFileUtils.copyFile(in, new File(MAPCONFIG_PATH));
                BufferedReader bufReader = new BufferedReader(inputReader);
                String line = "";
                while ((line = bufReader.readLine()) != null)
                    Result += line;
            }
            return Result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Result;
    }


    /**
     * 启动室内定位服务
     *
     * @param callback
     */
    public void startIndoorSdkLocate(String mapID, IPSMeasurement.Callback callback) {
//        if(!mVerifySucess){
//            Log.e(TAG,"认证不通过，请确保APIKey正确");
//            return;
//        }
        currentSetMapID = mapID;
        Gson gson = new Gson();
        mapConfig = gson.fromJson(getMapConfig(mContext, "indoor_data"), MapConfig.class);
        mCallback = callback;
        Intent intent = new Intent(mContext, IndoorPositionService.class);
        boolean status = mContext.bindService(intent, connection, Context.BIND_AUTO_CREATE);
        Log.d(TAG, "Service start status is " + status);
    }

    private void updateMapConfig(String mapID, IndoorPositionService indoorPositionService) {

        if (mapConfig == null) {
            Log.e(TAG, "mapConfig==null,data err...");
            return;
        }
        if (currentMapConfigID.equals(mapID)) {
            return;
        }
        Log.d(TAG, "updateMapConfig...");
        currentMapConfigID = mapID;
        mCurrentConfig = mapConfig.getDataConfig().get(0);
        for (MapConfig.DataConfigDTO dataConfig : mapConfig.getDataConfig()) {
            if (dataConfig.getMapid().equals(currentMapConfigID)) {
                mCurrentConfig = dataConfig;
                break;
            }
        }
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
            Log.d("ServiceConnection", "Service connected!");
            Log.d("ServiceConnection", "Service connected!");
            IndoorPositionService.LocalBinder binder = (IndoorPositionService.LocalBinder) service;
            IndoorPositionService indoorPositionService = binder.getService();
            AtomicInteger i = new AtomicInteger();
            updateMapConfig(currentSetMapID + "", indoorPositionService);
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
                Log.i("result", text);
                updateMapConfig(measurement.getMapID() + "", indoorPositionService);
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

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

import com.indoor.position.IPSMeasurement;
import com.indoor.position.IndoorPositionService;

public class BmdhIndoorStrategy {
    private static final String TAG = "BmdhIndoorStrategy";
    private static final String META_DATA = "com.bmdh.indoorsdk.API_KEY";
    private Context mContext;
    private boolean mBound = false;
    private boolean mVerifySucess = false;
    private boolean mIsIndoor = false;
    private IPSMeasurement.Callback mCallback;

    public BmdhIndoorStrategy(Context context) {
        mContext = context;
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

    /**
     * 启动室内定位服务
     *
     * @param callback
     */
    public void startIndoorSdkLocate(IPSMeasurement.Callback callback) {
//        if(!mVerifySucess){
//            Log.e(TAG,"认证不通过，请确保APIKey正确");
//            return;
//        }
        mCallback = callback;
        Intent intent = new Intent(mContext, IndoorPositionService.class);
        boolean status = mContext.bindService(intent, connection, Context.BIND_AUTO_CREATE);
        Log.d(TAG, "Service start!");
    }

    private final ServiceConnection connection = new ServiceConnection() {

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            Log.d("ServiceConnection", "Service connected!");
            final IndoorPositionService.LocalBinder binder = (IndoorPositionService.LocalBinder) service;
            IndoorPositionService indoorPositionService = binder.getService();
            indoorPositionService.register(measurement -> {

                // Do something
                String text = "x=" + measurement.getX() + "\n" +
                        "y=" + measurement.getY() + "\n" +
                        "z=" + measurement.getZ() + "\n" +
                        "vx=" + measurement.getVx() + "\n" +
                        "vy=" + measurement.getVy() + "\n" +
                        "vz=" + measurement.getVz() + "\n" +
                        "mapID=m" + measurement.getMapID() + "\n" +
                        "Mode=" + measurement.getMode() + "\n" + "GNSS\n" + measurement.getText();
                mIsIndoor = measurement.getMode() == IPSMeasurement.Mode.INDOOR;
//                System.out.println(text);
                Log.d("galaxy", text);
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

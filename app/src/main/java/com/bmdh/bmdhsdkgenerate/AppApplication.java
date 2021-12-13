package com.bmdh.bmdhsdkgenerate;

import android.app.Application;
import android.util.Log;

import com.indoor.AzimuthIndoorConfig;
import com.indoor.AzimuthIndoorSDK;
import com.indoor.IAzimuthNaviManager;

public class AppApplication extends Application {

    private static final String TAG="AppApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        AzimuthIndoorSDK.getInstance().init(this, new AzimuthIndoorConfig.Builder().DEAFULT(this).build(), new IAzimuthNaviManager.IInitSDKListener() {

            @Override
            public void initStart() {

            }

            @Override
            public void initSuccess(String code) {

            }

            @Override
            public void initFailed(String message) {
                Log.d(TAG,"initFailed;message is "+message);
            }
        });
    }
}

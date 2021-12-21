package com.bmdh.bmdhsdkgenerate;

import android.app.Application;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.indoor.AzimuthIndoorConfig;
import com.indoor.AzimuthIndoorSDK;
import com.indoor.IAzimuthNaviManager;
@RequiresApi(api = Build.VERSION_CODES.O)
public class AppApplication extends Application {

    private static final String TAG="AppApplication";


    @Override
    public void onCreate() {
        super.onCreate();
        AzimuthIndoorSDK.getInstance().init(this,  new IAzimuthNaviManager.IInitSDKListener() {

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

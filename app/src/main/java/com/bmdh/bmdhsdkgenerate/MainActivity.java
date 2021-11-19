package com.bmdh.bmdhsdkgenerate;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;

import com.indoor.BmdhIndoorConfig;
import com.indoor.BmdhIndoorSDK;
import com.indoor.IBmdhNaviManager;
import com.indoor.position.IPSMeasurement;

public class MainActivity extends AppCompatActivity implements IPSMeasurement.Callback {
private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BmdhIndoorSDK.getInstance().init(this, new BmdhIndoorConfig.Builder().DEAFULT(this).build(), new IBmdhNaviManager.IInitSDKListener() {
            @Override
            public void onAuthResult(int code, String message) {
                Log.d(TAG,"onAuthResult code:"+code);
            }

            @Override
            public void initStart() {

            }

            @Override
            public void initSuccess() {

            }

            @Override
            public void initFailed(int code, String message) {
                Log.d(TAG,"initFailed code:"+code+";message is "+message);
            }
        });
        BmdhIndoorSDK.getInstance().startIndoorLocation(2,this);
    }

    @Override
    public void onReceive(IPSMeasurement measurement) {
        Log.d(TAG,"measurement :"+measurement.getClass().getSimpleName());
    }
}
package com.bmdh.bmdhsdkgenerate;

import static androidx.core.os.HandlerCompat.postDelayed;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.indoor.AzimuthIndoorSDK;
import com.indoor.position.IPSMeasurement;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements IPSMeasurement.Callback {

    private static final String TAG = MainActivity.class.getSimpleName();
    private BluetoothAdapter mBluetoothAdapter;
    private Timer timer;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION}, 100);
        timer = new Timer();
        AzimuthIndoorSDK.getInstance().onCreate();


    }

    public void startLocation() {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                AzimuthIndoorSDK.getInstance().startIndoorLocation("000003", MainActivity.this);
            }
        },5000);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean isAllGrant = true;
        for (int grantResult : grantResults) {
            if (grantResult != PackageManager.PERMISSION_GRANTED) {
                isAllGrant = false;
                Toast.makeText(this, "请通过权限了才能获取位置信息", Toast.LENGTH_LONG).show();
                break;
            }
        }
        if (isAllGrant) {
            openBluetooth();
            startLocation();
        }
    }
    public  boolean openBluetooth() {
        BluetoothAdapter adapter = getBluetoothAdapter();
        if (adapter != null) {
            return adapter.enable();
        }
        return false;
    }

    public  BluetoothAdapter getBluetoothAdapter() {
        if (mBluetoothAdapter == null) {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }
        return mBluetoothAdapter;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timer.cancel();
        timer=null;
        AzimuthIndoorSDK.getInstance().exitSDK();
    }

    @Override
    public void onReceive(IPSMeasurement measurement) {
        String text = "\n result" +
                "\nx," + measurement.getX() + "," +
                "y," + measurement.getY() + "\n" +
                "z=" + measurement.getZ() + "\n" +
                "vx=" + measurement.getVx() + "\n" +
                "vy=" + measurement.getVy() + "\n" +
                "state=" + measurement.getVz() + "\n" +
                "mapID=" + measurement.getMapID() + "\n" +
                "Mode=" + measurement.getMode() + "\n"  + measurement.getText();
        Log.d(TAG,"measurement is :"+text);
    }

}
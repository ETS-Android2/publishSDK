package com.bmdh.bmdhsdkgenerate;

import static android.os.Environment.DIRECTORY_DOWNLOADS;
import static androidx.core.os.HandlerCompat.postDelayed;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.indoor.AzimuthIndoorSDK;
import com.indoor.position.IPSMeasurement;
import com.indoor.position.IndoorPositionService;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
@RequiresApi(api = Build.VERSION_CODES.O)
public class MainActivity extends AppCompatActivity implements IPSMeasurement.Callback {

    private static final String TAG = MainActivity.class.getSimpleName();
    private BluetoothAdapter mBluetoothAdapter;
    private Timer timer;
    private BufferedWriter mFileWriter;
    private File mFile;
    String currentFilePath;
    private Button startlog;
    private Button endlog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION}, 100);
        startlog =  ((Button) findViewById(R.id.button));
        startlog.setBackgroundColor(Color.BLUE);
        startlog.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startNewLog();
                        startlog.setText("Logging");
                        startlog.setBackgroundColor(Color.TRANSPARENT);
                        startlog.setEnabled(false);

                    }
                });
        endlog = ((Button) findViewById(R.id.button2));
        endlog.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        recordend();
                        startlog.setText("StartLog");
                        startlog.setBackgroundColor(Color.BLUE);
                        startlog.setEnabled(true);
                    }
                });
        timer = new Timer();
        AzimuthIndoorSDK.getInstance().onCreate();
    }

    public void startLocation() {
        timer.schedule(new TimerTask() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void run() {
                AzimuthIndoorSDK.getInstance().setAreaId("14717545583162245141639728605239310115");
                AzimuthIndoorSDK.getInstance().startIndoorLocation( MainActivity.this);
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
    public void onReceive(IPSMeasurement measurement) {
        runOnUiThread(() -> {
            TextView t = findViewById(R.id.textView1);
            long timeSeconds = System.currentTimeMillis();
            SimpleDateFormat sdf =new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss", Locale.getDefault());
            String timetag = sdf.format(timeSeconds);
            if (t != null) {
                String text = measurement.getText();
                text = "定位时间:" + timetag +"\n" + text;
                t.setText(text);
                if(!startlog.isEnabled())
                {
                    if (mFileWriter != null) {
                        try {
                            mFileWriter.write(text);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        Log.e(TAG, "run: mFileWriter null");
                    }
                    Log.i("result", "x," +twoDigitsString(measurement.getX())+ "," +
                            "y," + twoDigitsString(measurement.getY()) + "\n");
                }
            }
        });
    }


    protected void onDestroy() {
        super.onDestroy();
        timer.cancel();
        timer=null;
        recordend();
        AzimuthIndoorSDK.getInstance().exitSDK();
    }
    public static String twoDigitsString(double value) {

        return String.format("%.2f", value);
    }
    public void startNewLog() {
        File baseDirectory;
        ContextWrapper cw = new ContextWrapper(getApplicationContext());

        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            baseDirectory = new File(String.valueOf(Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS)));
            baseDirectory.mkdirs();
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            Log.e(TAG, "startNewLog: Cannot write to external storage.");
            return;
        } else {
            Log.e(TAG, "Cannot read external storage.");
            return;
        }

        SimpleDateFormat formatter = new SimpleDateFormat("yyy_MM_dd_HH_mm_ss");
        Date now = new Date();
        String fileName = String.format("%s_%s.txt", "algorithm_log", formatter.format(now));
        File currentFile = new File(baseDirectory, fileName);
        currentFilePath = currentFile.getAbsolutePath();
        BufferedWriter currentFileWriter;
        try {
            currentFileWriter = new BufferedWriter(new FileWriter(currentFile));
        } catch (IOException e) {
            Log.e(TAG, " Could not open file:+ currentFilePath", e);
            return;
        }
        if (mFileWriter != null) {
            try {
                mFileWriter.close();
            } catch (IOException e) {
                Log.e(TAG, " Unable to close all file streams.", e);
                return;
            }
        }

        mFile = currentFile;
        mFileWriter = currentFileWriter;
    }
    public void recordend() {
        if (mFile == null) {
            return;
        }

        if (mFileWriter != null) {
            try {
                mFileWriter.flush();
                mFileWriter.close();
                mFileWriter = null;
            } catch (IOException e) {
                Log.e(TAG, "recordend: Unable to close all file streams.", e);
                return;
            }
        }
    }



}
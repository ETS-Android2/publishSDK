package com.indoor.position;

import android.app.Service;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.indoor.BmdhIndoorConfig;
import com.indoor.position.swiggenerated.IndoorPositionProcessor;

/**
 * {@link IndoorPositionService} is {@link Service} implementation that is used for accurate
 * positioning in a room.
 */

@Keep
@RequiresApi(api = Build.VERSION_CODES.O)
public class IndoorPositionService extends Service {
    static {
        System.loadLibrary("IPS");
    }

    private final IBinder binder = new LocalBinder();
    private IPSCoreRunner ipsCoreRunner;


    /**
     * Register a self-defined callback to receive the IPS response.
     *
     * @param callback to receive IPS measurements
     */
    public void register(IPSMeasurement.Callback callback) {
        ipsCoreRunner.addCallbacks(callback);
    }

    /**
     * 修正坐标
     *
     * @param mapId 设备端返回的MapId
     */
    public void setIpsCoreRunnerFixCoord(String mapId){
        ipsCoreRunner.initConfig(BmdhIndoorConfig.getFixCoord(mapId));
    }
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("IndoorPositionService", "Service onCreate");
        LocationManager locationManager =
                (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

        ipsCoreRunner = new IPSCoreRunner(
                new GNSSProcessor(locationManager),
                new SensorProcessor(sensorManager),
                new BluetoothProcessor(bluetoothManager),
                new IndoorPositionProcessor());
        boolean gnssStatus = ipsCoreRunner.startUp();
        if (gnssStatus) {
            Log.d("IndoorPositionService", "GNSS module startup successfully.");
        }
    }

    public void onDestroy() {
        super.onDestroy();
        Log.d("IndoorPositionService", "Service onDestroy");
        ipsCoreRunner.tearDown();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    /**
     * Binder for {@link IndoorPositionService}.
     */
    // TODO: REFACTOR this
    @Keep
    public class LocalBinder extends Binder {
        public IndoorPositionService getService() {
            // Return this instance of service so clients can call public methods
            return IndoorPositionService.this;
        }
    }
}

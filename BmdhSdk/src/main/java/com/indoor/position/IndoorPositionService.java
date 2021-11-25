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

import com.google.gson.Gson;
import com.indoor.MapConfig;
import com.indoor.position.swiggenerated.IndoorPositionProcessor;
import com.indoor.position.swiggenerated.Inputparameter;
import com.indoor.position.swiggenerated.SatelliteInfo;
import com.indoor.position.swiggenerated.SatelliteInfoList;

import lombok.SneakyThrows;

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

    @SneakyThrows
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
    }

    public void onDestroy() {
        super.onDestroy();
        Log.d("IndoorPositionService", "Service onDestroy");
        ipsCoreRunner.tearDown();
    }

    public void setInfoAndStartup(MapConfig.DataConfigDTO mCurrentConfig) {
        Log.d("IndoorPositionService", "setInfoAndStartup begin");
        SatelliteInfoList s=new SatelliteInfoList();
        for(MapConfig.DataConfigDTO.SatelliteInfoDTO satelliteInfo:mCurrentConfig.getSatelliteInfo()){
            s.add(new SatelliteInfo(satelliteInfo.getX(),satelliteInfo.getY(),satelliteInfo.getZ(),satelliteInfo.getSvid()));
        }
        Log.d("IndoorPositionService", "setInfoAndStartup SatelliteInfoDTO");
        double[] roomCenter=new double[]{mCurrentConfig.getRoomCenter().getX(),mCurrentConfig.getRoomCenter().getY()};
        double[] threshold_x_y=new double[]{mCurrentConfig.getThresholdXY().getX(),mCurrentConfig.getThresholdXY().getY()};
        double[] gmocratorfixcoord=new double[]{mCurrentConfig.getGmocratorfixcoord().getX(),mCurrentConfig.getGmocratorfixcoord().getY(),mCurrentConfig.getGmocratorfixcoord().getZ()};
        double[] fixedCoor= new double[]{mCurrentConfig.getFixedCoor().getX(),mCurrentConfig.getFixedCoor().getY(),mCurrentConfig.getFixedCoor().getZ()};
        Inputparameter inputparameter=new Inputparameter(roomCenter,threshold_x_y,false,new double[]{},fixedCoor);
        ipsCoreRunner.updateInputData(s,inputparameter);
        ipsCoreRunner.updateMercatorConvertParameter(gmocratorfixcoord,mCurrentConfig.getGdegZfixcoord());
        ipsCoreRunner.updateSatelliteSerial(mCurrentConfig.getListL1().toArray(new Integer[mCurrentConfig.getListL1().size()]),mCurrentConfig.getListL5().toArray(new Integer[mCurrentConfig.getListL5().size()]),mCurrentConfig.getIspsemodeL1());
        Log.d("IndoorPositionService", "setInfoAndStartup");
        boolean gnssStatus = ipsCoreRunner.startUp();
        if (gnssStatus) {
            Log.d("IndoorPositionService", "GNSS module startup successfully.");
        }

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

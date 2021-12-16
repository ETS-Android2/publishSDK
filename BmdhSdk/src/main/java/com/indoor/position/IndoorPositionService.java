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

import com.indoor.AzimuthIndoorStrategy;
import com.indoor.MapConfig;
import com.indoor.MapConfigData;
import com.indoor.data.SDKRepository;
import com.indoor.position.swiggenerated.IndoorPositionProcessor;
import com.indoor.position.swiggenerated.Inputparameter;
import com.indoor.position.swiggenerated.SatelliteInfo;
import com.indoor.position.swiggenerated.SatelliteInfoList;
import com.indoor.utils.KLog;

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

    private static final String TAG = "IndoorPositionService";
    private final IBinder binder = new LocalBinder();
    private IPSCoreRunner ipsCoreRunner;
    private MapConfigData mCureentMapConfig = null;


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
        Log.d(TAG, "Service onCreate");
        LocationManager locationManager =
                (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

        ipsCoreRunner = new IPSCoreRunner(
                new GNSSProcessor(locationManager),
                new SensorProcessor(sensorManager),
                new StepProcessor(sensorManager),
                new BluetoothProcessor(bluetoothManager),
                new IndoorPositionProcessor());
    }

    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service onDestroy");
        ipsCoreRunner.tearDown();
    }

    public void setInfoAndStartup(MapConfigData mMapConfig, SDKRepository sdkRepository) {
        KLog.d(TAG, "setInfoAndStartup begin");
        if (mMapConfig == null) {
            KLog.e(TAG, "setInfoAndStartup,mMapConfig is null ,Please make sure you have access to indoor map information....");
            return;
        }
        if ((mCureentMapConfig != null && AzimuthIndoorStrategy.getAreaCode(mCureentMapConfig.getProjectAreaId()).equals(AzimuthIndoorStrategy.getAreaCode(mMapConfig.getProjectAreaId())))) {
            KLog.d(TAG, "setInfoAndStartup,same areaCode....");
            return;
        }
        mCureentMapConfig = mMapConfig;
        double[] roomCenter = new double[]{0, 0};
        double[] threshold_x_y = new double[]{0, 0};
        double[] gmocratorfixcoord = new double[]{0, 0, 0};
        double[] ref_Coord = new double[]{0, 0, 0};
        double gmocatordeg = 0;
        double jingweideg = 0;
        boolean is_l1_pse = true;
        String strL1 = "1";
        String strL5 = "1";
        SatelliteInfoList s = new SatelliteInfoList();
        try {
            MapConfigData.GmocratorParameterDecypt gmocratorFixcoordDecypt = mMapConfig.getGmocratorFixcoordDecypt(sdkRepository.get3DesSalt());
            for (MapConfigData.SatelliteInfoDTO satelliteInfo : mMapConfig.getSatelliteInfo()) {
                s.add(new SatelliteInfo(satelliteInfo.getXxAxisCoordinate(), satelliteInfo.getYyAxisCoordinate(), satelliteInfo.getZzAxisCoordinate(), satelliteInfo.getSvid()));
            }
            Log.d("IndoorPositionService", "setInfoAndStartup SatelliteInfoDTO");
            roomCenter = new double[]{mMapConfig.getXxRoomCenter(), mMapConfig.getYyRoomCenter()};
            threshold_x_y = new double[]{mMapConfig.getXxThreshold(), mMapConfig.getYyThreshold()};
            gmocratorfixcoord = new double[]{gmocratorFixcoordDecypt.getXxGmocratorFixcoord(), gmocratorFixcoordDecypt.getYyGmocratorFixcoord(), gmocratorFixcoordDecypt.getZzGmocratorFixcoord()};
            ref_Coord = new double[]{mMapConfig.getXxFixedCoor(), mMapConfig.getYyFixedCoor(), mMapConfig.getZzFixedCoor()};
            gmocatordeg = gmocratorFixcoordDecypt.getGdegzFixcoord();
            jingweideg = mMapConfig.getLgtLatDeg();
            is_l1_pse = mMapConfig.getPseModeOne() == 1;
            strL1 = mMapConfig.getSpectrumList(MapConfigData.PSE_MODE_NAME_L1);
            strL5 = mMapConfig.getSpectrumList(MapConfigData.PSE_MODE_NAME_L5);
        } catch (Exception e) {
            KLog.e(TAG, "setInfoAndStartup Exception:" + e.getMessage());
        }
        Inputparameter inputparameter = new Inputparameter(roomCenter, threshold_x_y,
                ref_Coord, gmocratorfixcoord, gmocatordeg, jingweideg, is_l1_pse);
        inputparameter.setListL1(strL1);
        inputparameter.setListL5(strL5);
        ipsCoreRunner.updateInputData(s, inputparameter);
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

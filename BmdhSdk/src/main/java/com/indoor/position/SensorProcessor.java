package com.indoor.position;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.google.common.collect.ImmutableList;
import com.indoor.position.SensorData.SensorDimMeasurements;
import com.indoor.position.SensorData.SensorType;

import java.util.concurrent.atomic.AtomicReference;

import lombok.Builder;
import lombok.ToString;
import lombok.Value;


/**
 * {@link SensorProcessor} deals the data collected from sensors.
 */
class SensorProcessor {
    private static final float NS2S = 1.0f / 1000000000.0f;
    private static final String LOG_TAG = "SensorProcessor";

    private final SensorManager sensorManager;

    private final IPSSensorEventListener accSensorEventListener = new IPSSensorEventListener();
    private final IPSSensorEventListener gyroscopeEventListener = new IPSSensorEventListener();
    private final IPSSensorEventListener magneticEventListener = new IPSSensorEventListener();
    private final IPSSensorEventListener rotationEventListener = new IPSSensorEventListener();
    // 只存了各项当前数据
    private final AtomicReference<SensorDimMeasurements> oriData;
    private final AtomicReference<SensorDimMeasurements> gyrData;
    private final AtomicReference<SensorDimMeasurements> accData;
    private final AtomicReference<SensorDimMeasurements> magData;
    private final AtomicReference<SensorDimMeasurements> rotData;
    private final ThreadSafeQueue<SensorDimMeasurements> oriHistory;
    private final ThreadSafeQueue<SensorDimMeasurements> gyrHistory;
    private final ThreadSafeQueue<SensorDimMeasurements> accHistory;
    private final ThreadSafeQueue<SensorDimMeasurements> magHistory;
    private final ThreadSafeQueue<SensorDimMeasurements> rotHistory;
    private boolean accState;
    private boolean gyrState;
    private boolean magState;
    private boolean rotState;
    private long gyroscopeTimestamp = 0;

    SensorProcessor(SensorManager sensorManager) {
        this.sensorManager = sensorManager;
        this.oriData = new AtomicReference<>(new SensorDimMeasurements(SensorType.ORIENTATION));
        this.gyrData = new AtomicReference<>(new SensorDimMeasurements(SensorType.GYROSCOPE));
        this.accData = new AtomicReference<>(new SensorDimMeasurements(SensorType.ACCELEROMETER));
        this.magData = new AtomicReference<>(new SensorDimMeasurements(SensorType.MAGNETIC_FIELD));
        this.rotData = new AtomicReference<>(new SensorDimMeasurements(SensorType.ROTATION));
        this.oriHistory = new ThreadSafeQueue<>(50);
        this.gyrHistory = new ThreadSafeQueue<>(50);
        this.accHistory = new ThreadSafeQueue<>(50);
        this.magHistory = new ThreadSafeQueue<>(50);
        this.rotHistory = new ThreadSafeQueue<>(50);
    }

    /**
     * Initialization.
     */
    boolean startUp() {
        // 加速度
        Sensor accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometerSensor != null) {
            accState = sensorManager.registerListener(accSensorEventListener, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
        // 陀螺仪
        Sensor gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        if (gyroscopeSensor != null) {
            gyrState = sensorManager.registerListener(gyroscopeEventListener, gyroscopeSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
        // 电磁场
        Sensor magneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if (magneticSensor != null) {
            magState = sensorManager.registerListener(magneticEventListener, magneticSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
        // 矢量
        Sensor rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        if (rotationSensor != null) {
            rotState = sensorManager.registerListener(rotationEventListener, rotationSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }

        return accState && gyrState && magState && rotState;
    }

    /**
     * Finish.
     */
    void tearDown() {
        if (accState) {
            sensorManager.unregisterListener(accSensorEventListener);
            accState = false;
        }
        if (gyrState) {
            sensorManager.unregisterListener(gyroscopeEventListener);
            gyrState = false;
        }
        if (magState) {
            sensorManager.unregisterListener(magneticEventListener);
            magState = false;
        }
        if (rotState) {
            sensorManager.unregisterListener(rotationEventListener);
            rotState = false;
        }
        oriHistory.clear();
        gyrHistory.clear();
        accHistory.clear();
        magHistory.clear();
        rotHistory.clear();
        gyroscopeTimestamp = 0;
    }

    /**
     * Get the most recent sensor data.
     */
    SensorData getSensorData() {
        return SensorData.builder()
                .accMeasurements(accData.get())
                .magMeasurements(magData.get())
                .orientationMeasurements(oriData.get())
                .gyroscopeMeasurements(gyrData.get())
                .rotMeasurements(rotData.get())
                .build();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    SensorDimMeasurementsHistoryPack getHistory() {
        return SensorDimMeasurementsHistoryPack.builder()
                .accHistory(accHistory.toList())
                .oriHistory(oriHistory.toList())
                .magHistory(magHistory.toList())
                .rotHistory(rotHistory.toList())
                .gyrHistory(gyrHistory.toList()).build();
    }

    void receiveSensorDataFromEvent(SensorEvent sensorEvent) {
        SensorDimMeasurements val = null;
        switch (sensorEvent.sensor.getType()) {
            case Sensor.TYPE_GYROSCOPE:
                val = buildGyroscopeData(sensorEvent);
                break;
            case Sensor.TYPE_ACCELEROMETER:
                val = buildAccelerometerData(sensorEvent);
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                val = buildMagneticFieldData(sensorEvent);
                break;
            case Sensor.TYPE_ROTATION_VECTOR:
                val = buildRotationFieldData(sensorEvent);
                break;
            default:
                break;
                // do nothing
        }
        if (val != null) {
            switch (sensorEvent.sensor.getType()) {
                case Sensor.TYPE_GYROSCOPE:
                    gyrHistory.add(val);
                    break;
                case Sensor.TYPE_ACCELEROMETER:
                    accHistory.add(val);
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD:
                    magHistory.add(val);
                    break;
                case Sensor.TYPE_ROTATION_VECTOR:
                    rotHistory.add(val);
                    break;
            }
        }
        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER || sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            // 计算方向
            val = buildOrientationData(sensorEvent);
            if (val != null) {
               oriHistory.add(val);
            }
        }
    }

    SensorDimMeasurements buildGyroscopeData(SensorEvent sensorEvent) {
        if (gyroscopeTimestamp == 0) {
            gyroscopeTimestamp = sensorEvent.timestamp;
            return null;
        }
        float dT = (sensorEvent.timestamp - gyroscopeTimestamp) * NS2S;
        float x = sensorEvent.values[0] * dT;
        float y = sensorEvent.values[1] * dT;
        float z = sensorEvent.values[2] * dT;
        SensorDimMeasurements prev = oriData.get();
        SensorDimMeasurements curr =
                SensorDimMeasurements.getBuilder(SensorType.GYROSCOPE)
                        .axisX(prev.getAxisX() + x)
                        .axisY(prev.getAxisY() + y)
                        .axisZ(prev.getAxisZ() + z)
                        .createTimeMillis(sensorEvent.timestamp)
                        .build();
        gyrData.set(curr);
        // Log.d(LOG_TAG, String.format("GyroscopeMeasurements data received. %s",
        //        gyrData.get().toString()));
        return curr;
    }

    SensorDimMeasurements buildAccelerometerData(SensorEvent sensorEvent) {
        SensorDimMeasurements curr =
                SensorDimMeasurements.getBuilder(SensorType.ACCELEROMETER)
                        .axisX(sensorEvent.values[0])
                        .axisY(sensorEvent.values[1])
                        .axisZ(sensorEvent.values[2])
                        .createTimeMillis(sensorEvent.timestamp)
                        .build();
        accData.set(curr);
        /*
        Log.d(LOG_TAG,
                String.format("AccMeasurements data received. %s", accData.get().toString()));
        */
        return curr;
    }

    SensorDimMeasurements buildRotationFieldData(SensorEvent sensorEvent) {
        float[] rotvecR = new float[9];
        float[] rotVecValues = sensorEvent.values.clone();
        SensorManager.getRotationMatrixFromVector(rotvecR, rotVecValues);

        float[] values = new float[3];
        SensorManager.getOrientation(rotvecR, values);
        if(values[0]>0)
        {
            values[0]= (float) Math.toDegrees(values[0]);
        }
        else
        {
            values[0]= (float) (360+Math.toDegrees(values[0]));
        }
        SensorDimMeasurements curr =
                SensorDimMeasurements.getBuilder(SensorType.ROTATION)
                        .axisX(values[0])
                        .axisY(values[1])
                        .axisZ(values[2])
                        .createTimeMillis(sensorEvent.timestamp)
                        .build();
        rotData.set(curr);
//        Log.d(LOG_TAG, String.format("rotMeasurements data received. %s",
//                rotData.get().toString()));
        return curr;
    }

    SensorDimMeasurements buildMagneticFieldData(SensorEvent sensorEvent) {
        SensorDimMeasurements curr =
                SensorDimMeasurements.getBuilder(SensorType.MAGNETIC_FIELD)
                        .axisX(sensorEvent.values[0])
                        .axisY(sensorEvent.values[1])
                        .axisZ(sensorEvent.values[2])
                        .createTimeMillis(sensorEvent.timestamp)
                        .build();
        magData.set(curr);
        // Log.d(LOG_TAG,
        //         String.format("MagMeasurements data received. %s", magData.get().toString()));
        return curr;
    }

    SensorDimMeasurements buildOrientationData(SensorEvent sensorEvent) {
        float[] r = new float[9];
        float[] values = new float[3];
        SensorDimMeasurements currAcc = accData.get();
        SensorDimMeasurements currMag = magData.get();
        if (currAcc == null || currMag == null) {
            return null;
        }
        float[] accMeasurements = {currAcc.getAxisX(), currAcc.getAxisY(), currAcc.getAxisZ()};
        float[] magMeasurements = {currMag.getAxisX(), currMag.getAxisY(), currMag.getAxisZ()};
        SensorManager.getRotationMatrix(r, null, accMeasurements, magMeasurements);
        SensorManager.getOrientation(r, values);

        SensorDimMeasurements curr =
                SensorDimMeasurements.getBuilder(SensorType.MAGNETIC_FIELD)
                        .axisX(values[0])
                        .axisY(values[1])
                        .axisZ(values[2])
                        .createTimeMillis(sensorEvent.timestamp)
                        .build();
        oriData.set(curr);
        //  Log.d(LOG_TAG, String.format("OrientationMeasurements data received. %s",
        //        oriData.get().toString()));
        return curr;
    }

    @ToString
    @Builder
    @Value
    public static class SensorDimMeasurementsHistoryPack {

        ImmutableList<SensorDimMeasurements> oriHistory;

        ImmutableList<SensorDimMeasurements> gyrHistory;

        ImmutableList<SensorDimMeasurements> accHistory;

        ImmutableList<SensorDimMeasurements> magHistory;

        ImmutableList<SensorDimMeasurements> rotHistory;

        ImmutableList<SensorDimMeasurements> getOriHistory()
        {
            return oriHistory;
        }
        ImmutableList<SensorDimMeasurements> getRotHistory()
        {
            return rotHistory;
        }

    }


    private final class IPSSensorEventListener implements SensorEventListener {
        @Override
        public void onSensorChanged(SensorEvent event) {
            receiveSensorDataFromEvent(event);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    }
}

package com.indoor.position;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.google.common.collect.ImmutableList;
import com.indoor.imustep.StepDetector;
import com.indoor.position.SensorData.SensorDimMeasurements;
import com.indoor.position.SensorData.SensorType;
import com.indoor.position.swiggenerated.IndoorPositionMeasurement;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import lombok.Builder;
import lombok.ToString;
import lombok.Value;


/**
 * {@link StepProcessor} deals the data collected from sensors.
 */
class StepProcessor {
    private static final float NS2S = 1.0f / 1000000000.0f;
    private static final double D2R = Math.PI / 180;
    private static final double STEPLENGTH = 0.4;
    private static final String LOG_TAG = "StepProcessor";
    static int stepcnt = 0;
    private final SensorManager sensorManager;

    private final StepDetector stepDetector = new StepDetector();
    private double jingweidistance[] = new double[]{0,0};
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

    private final ThreadSafeQueue<Double> normacclistori;
    private final ThreadSafeQueue<Double> afterfliternormacc;

    private boolean accState;
    private boolean gyrState;
    private boolean magState;
    private boolean rotState;
    private long gyroscopeTimestamp = 0;

    StepProcessor(SensorManager sensorManager) {
        this.sensorManager = sensorManager;
        this.oriData = new AtomicReference<>(new SensorDimMeasurements(SensorType.ORIENTATION));
        this.gyrData = new AtomicReference<>(new SensorDimMeasurements(SensorType.GYROSCOPE));
        this.accData = new AtomicReference<>(new SensorDimMeasurements(SensorType.ACCELEROMETER));
        this.magData = new AtomicReference<>(new SensorDimMeasurements(SensorType.MAGNETIC_FIELD));
        this.rotData = new AtomicReference<>(new SensorDimMeasurements(SensorType.ROTATION));
        this.normacclistori = new ThreadSafeQueue<>(20);
        this.afterfliternormacc = new ThreadSafeQueue<>(20);
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
        normacclistori.clear();
        afterfliternormacc.clear();
        gyroscopeTimestamp = 0;
    }

    double  getStepjing()
    {
        return jingweidistance[0];
    }
    double getStepwei()
    {
        return jingweidistance[1];
    }
    void clearStepjingwei()
    {
        jingweidistance[0]=0;
        jingweidistance[1]=0;
    }
    public double[] jingweitoxy(double fixcoordRad)
    {

        double[] ourposxy=new double[]{0,0};
        ourposxy[0]= jingweidistance[0]*Math.cos(fixcoordRad)+jingweidistance[1]*Math.sin(fixcoordRad);
        ourposxy[1]= -jingweidistance[0]*Math.sin(fixcoordRad)+jingweidistance[1]*Math.cos(fixcoordRad);
        return ourposxy;
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
    Double accfliter(double allacc)
    {
        List<Double> list = normacclistori.toList();
        List<Double> afterlist = afterfliternormacc.toList();

        Double result;
        if(list.size()>=20)
        {
            return 0.007737* list.get(19)+0.010147* list.get(18)+0.017117*list.get(17)+0.027892*list.get(16)
                    +0.041303*list.get(15)+0.055898*list.get(14)+0.070095*list.get(13)+0.082355*list.get(12)
                    +0.09135*list.get(11)+0.096105*list.get(10)+0.096105*list.get(9)+0.09135*list.get(8)
                    +0.082355*list.get(7)+0.070095*list.get(6)+0.055898*list.get(5)+0.041303*list.get(4)
                    +0.027892*list.get(3)+0.017117*list.get(2)+0.010147*list.get(1)+0.007737*list.get(0);
        }
        else
        {
            result = list.stream().mapToDouble(Double::doubleValue).average().getAsDouble();
            afterfliternormacc.add(result);
            return result;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    void receiveSensorDataFromEvent(SensorEvent sensorEvent) {
        SensorDimMeasurements val = null;
        switch (sensorEvent.sensor.getType()) {
            case Sensor.TYPE_GYROSCOPE:
                val = buildGyroscopeData(sensorEvent);
                break;
            case Sensor.TYPE_ACCELEROMETER:
                val = buildAccelerometerData(sensorEvent);
                double currentgravity = Math.sqrt(sensorEvent.values[0]*sensorEvent.values[0]+sensorEvent.values[1]*sensorEvent.values[1]+sensorEvent.values[2]*sensorEvent.values[2]);
                normacclistori.add(currentgravity);
                accfliter(currentgravity);
                if(stepDetector.stepdector((float) currentgravity))
                {
                    Log.i("stepcnt", "receiveSensorDataFromEvent: "+stepcnt++);
                    Log.i("stepcnt", "getAxisX(): "+rotData.get().getAxisX());
                    jingweidistance[1]+=STEPLENGTH*Math.cos(rotData.get().getAxisX()*D2R);
                    jingweidistance[0]+=STEPLENGTH*Math.sin(rotData.get().getAxisX()*D2R);
                };
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

        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER || sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            // 计算方向
            val = buildOrientationData(sensorEvent);
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
  /*        Log.d(LOG_TAG, String.format("OrientationMeasurements data received. %s",
                oriData.get().toString()));*/
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
        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void onSensorChanged(SensorEvent event) {
            receiveSensorDataFromEvent(event);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    }
}

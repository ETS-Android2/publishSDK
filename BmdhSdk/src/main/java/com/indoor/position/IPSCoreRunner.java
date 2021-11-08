package com.indoor.position;



import static com.indoor.position.IPSMeasurement.Mode.INDOOR;
import static com.indoor.position.IPSMeasurement.Mode.OUTDOOR;

import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

//import com.google.common.collect.EvictingQueue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.indoor.position.swiggenerated.IndoorPositionMeasurement;
import com.indoor.position.swiggenerated.IndoorPositionProcessor;
import com.indoor.position.swiggenerated.SatelliteIndoorMeasurement;
import com.indoor.position.swiggenerated.SatelliteIndoorMeasurementList;
import com.indoor.position.swiggenerated.SatelliteInfo;
import com.indoor.position.swiggenerated.SatelliteInfoList;
import com.indoor.position.swiggenerated.SatelliteIndoorMeasurement;
import com.indoor.position.swiggenerated.Inputparameter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
//import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;

/**
 * {@link IPSCoreRunner} coordinates the GNSS and sensor measurements as well as the indoor position
 * calculations.
 */
@RequiresApi(api = Build.VERSION_CODES.O)
class IPSCoreRunner {
    //private static final String LOG_TAG = "IPSCoreRunner";
    private final GNSSProcessor gnssProcessor;
    //private final SensorProcessor sensorProcessor;
    private final BluetoothProcessor bluetoothProcessor;
    private final IndoorPositionProcessor indoorPositionProcessor;
    private final String TAG ="changxing";
    private final Timer timer = new Timer();
    private String debugstring;
    private final AtomicReference<IPSMeasurement.Callback> callback;
    /*private final IPSMeasurement userPosition = null;
    private final IPSMeasurement crossPositon1 = new IPSMeasurement("", 0, 0, 1.4, 0.0, 0.0, 0.0, 123, INDOOR);
    private final IPSMeasurement crossPositon2 = new IPSMeasurement("", 15, 20, 0, 0.0, 0.0, 0.0, 123, INDOOR);*/
    //private final ThreadSafeQueue<Float> orientationX = new ThreadSafeQueue<>(6);
    // TODO: refactor this
    private double[] bluetoothReferLocation={1,2,3};
    private IPSMeasurement.Mode mode= OUTDOOR;
    private long mapID;
    private boolean hasinitonedeKF=false;
    private boolean hasinittwodeKF=false;
    private int currentpath=0;//= GNSSProcessor.GNSSFilter.RoadDir.Lengthwise;
    private double[] pathRange;
    double[] fixedCoor= new double[]{0,0,1.4};

    //double[] gmocratorfixcoord=new double[]{13553441.0197,3679297.281,0};
    //double gdegZfixcoord=Math.PI*1.5;

    /***长兴海洋装备
    double[] gmocratorfixcoord=new double[]{0,0,0};
    double gdegZfixcoord=0;**/
    //方位角办公室
    double[] gmocratorfixcoord=new double[]{13536740.8542030,3649358.63141886,3.4312};
    double gdegZfixcoord=Math.PI*1.5;
/** 地下室 **/
/*    double[] roomCenter=new double[]{29.63,6.88};
    double[] threshold_x_y=new double[]{9,8};*/
    /** 停车场 **/
    double[] roomCenter=new double[]{6,7};
    double[] threshold_x_y=new double[]{14,13};

    Map<String, List<Double>> bluetoothLabel;

    private static final double[][] pathrangeindex=new double[][]{{0,-28,0,0},{5,0,17,0}};
    private static double[] areaRange;
    boolean bluetoothCheckResult = false;
    private IndoorPositionMeasurement posresult;
    IPSCoreRunner(
            GNSSProcessor gnssProcessor,
            SensorProcessor sensorProcessor,
            BluetoothProcessor bluetoothProcessor,
            IndoorPositionProcessor indoorPositionProcessor) {
        this.gnssProcessor = gnssProcessor;
       // this.sensorProcessor = sensorProcessor;
        this.bluetoothProcessor = bluetoothProcessor;
        this.indoorPositionProcessor = indoorPositionProcessor;
        this.callback = new AtomicReference<>();
    }

    public void initConfig(double[] gmocratorfixcoord){
        this.gmocratorfixcoord=gmocratorfixcoord;
    }


    /**
     * Runner starts and initializes.
     */
    boolean startUp() {
        boolean state = gnssProcessor.startUp();
        if (state) {
            SatelliteInfoList s = new SatelliteInfoList();
/*  地下室老发射机坐标
            s.add(new SatelliteInfo(22.64,1.44,3.85,1));
            s.add(new SatelliteInfo(30.53,5.06,3.77, 2));
            s.add(new SatelliteInfo(34.84,5.06,3.75, 3));
            s.add(new SatelliteInfo(35.24,10.49,4.00, 4));
            s.add(new SatelliteInfo(22.64,10.49,3.90, 5));
            s.add(new SatelliteInfo(24.53,3.44,3.88, 6));
            s.add(new SatelliteInfo(26.09,7.688,3.887, 7));
            s.add(new SatelliteInfo(31.75,7.688,4.00, 8));*/
            /*二维停车场室坐标*/
            s.add(new SatelliteInfo( 13.59,0.00,2.43,1  ));
            s.add(new SatelliteInfo( 19.20,0.00,2.63,2));
            s.add(new SatelliteInfo( 0.00,16.10,2.67,3 ));
            s.add(new SatelliteInfo( 14.00,16.10,2.66,4 ));
            s.add(new SatelliteInfo(18.74,16.10,2.66,5 ));
            s.add(new SatelliteInfo( -4.96,11.00,2.68,6  ));
            s.add(new SatelliteInfo( 7.09,11.00,2.69,7 ));
            s.add(new SatelliteInfo( 7.09,21.38,2.68,8));
            /*二维移动双发射机坐标
           s.add(new SatelliteInfo(22.64,1.44,3.85, 1));
            s.add(new SatelliteInfo(30.53,5.06,3.77, 3));
            s.add(new SatelliteInfo(34.84,5.06,3.75, 24));
            s.add(new SatelliteInfo(35.24,10.49,4.00, 25));
            s.add(new SatelliteInfo(22.64,10.49,3.90, 26));
            s.add(new SatelliteInfo(24.53,3.44,3.88, 27));
            s.add(new SatelliteInfo(26.09,7.688,3.887 , 30));
            s.add(new SatelliteInfo(31.75,7.688,4.00 , 32));*/
            double[] initpoint=new double[] {roomCenter[0],roomCenter[1],1.3} ;
            indoorPositionProcessor.updateSatelliteInfo(s,initpoint);
            CallbackSchedule();
        }
        //蓝牙标签位置初始化
        bluetoothLabel =ImmutableMap.<String, List<Double>>builder().
                put("016", Arrays.<Double>asList(0.0,0.0, 1.4)).
                        build();
        return state;
    }


    /**
     * Finish and clean up the runner.
     */
    void tearDown() {
        timer.cancel();
        gnssProcessor.tearDown();
        //sensorProcessor.tearDown();
    }


    // TODO: remove this method to {@link BluetoothProcessor}
    boolean bluetoothDataCheck(ImmutableMap<String, BluetoothData> data) {
        String maxId = "";
        int maxCnt = 0;
        for (Map.Entry<String, BluetoothData> entry : data.entrySet()) {
            if (entry.getValue().getCount() > maxCnt) {
                maxId = entry.getKey();
                maxCnt = entry.getValue().getCount();
            }
        }
        if (!maxId.equals("") && maxCnt >= 2) {
            if((maxId.equals("016"))) {
                if(!hasinittwodeKF) {
                    indoorPositionProcessor.initialAPBCKFTwoDimen();
                    hasinittwodeKF = true;
                    areaRange= new double[]{roomCenter[0] - threshold_x_y[0], roomCenter[1] - threshold_x_y[1], roomCenter[0] + threshold_x_y[0], roomCenter[1] + threshold_x_y[1]};
                }
                mapID=2;
                mode=INDOOR;
            }
            if(bluetoothLabel.containsKey(maxId)) {
                bluetoothReferLocation = bluetoothLabel.get(maxId).stream().mapToDouble(i -> i).toArray();
                    bluetoothLabel =ImmutableMap.<String, List<Double>>builder().
                            put("xx", Arrays.<Double>asList(0.00,  -27.52,  1.3)).
                            build();
            return true;
            }
        }
        return false;
    }


    IPSMeasurement coordExchange(IndoorPositionMeasurement result,double[] mocratorfixcoord,double degZfixcoord,String text, long currentMapID, IPSMeasurement.Mode currentMod)
    {
        if(result!=null) {
            IPSMeasurement mocrator = new IPSMeasurement(text,result.getXAxisCoordinate()*Math.cos(degZfixcoord)+result.getYAxisCoordinate()*Math.sin(degZfixcoord)+ mocratorfixcoord[0],
                    -result.getXAxisCoordinate()*Math.sin(degZfixcoord)+result.getYAxisCoordinate()*Math.cos(degZfixcoord)+ mocratorfixcoord[1],
                    0+mocratorfixcoord[2],
                    0, 0, result.getlocatestate(), currentMapID, currentMod);/*(-result.getY())*/
            return mocrator;
        }
        else
        {
            return new IPSMeasurement(text,0,0, 0,
                0, 0, 0, currentMapID, currentMod);
        }

    }

    void CallbackSchedule() {
        long period = 1000;
        if(!hasinittwodeKF) {
            indoorPositionProcessor.initialAPBCKFTwoDimen();
            hasinittwodeKF = true;
            areaRange= new double[]{roomCenter[0] - threshold_x_y[0], roomCenter[1] - threshold_x_y[1], roomCenter[0] + threshold_x_y[0], roomCenter[1] + threshold_x_y[1]};
            Log.i(TAG, "init initialAbsolutePositioningBaseCarrierKFTwoDimen");
        }
        mapID=2;
        mode=INDOOR;

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                IPSMeasurement.Callback c = callback.get();
            if (c == null) {
                    return;
                }
               /* bluetoothProcessor.tryScan(900);
                ImmutableMap<String, BluetoothData> data = bluetoothProcessor.getHisBluetoothData();
                if (data.size() >= 1) {
                    bluetoothCheckResult = bluetoothDataCheck(data);
                }
                bluetoothProcessor.clearHisBluetoothData();*/
                GNSSData gnssData = gnssProcessor.getGNSSData();
                if (mode == INDOOR) {
                    GNSSProcessor.GNSSFilter fliter;
                    if (gnssData != null&&gnssData.getSatelliteGNSSMeasurements().size()>0) {
                        fliter = new GNSSProcessor.GNSSFilter(GNSSProcessor.GNSSFilter.flitermod.freL5);
                        ImmutableList<GNSSData.SatelliteGNSSMeasurements> finalGnssData = gnssData.
                                getSatelliteGNSSMeasurements().values().stream().filter(fliter).collect(ImmutableList.toImmutableList());
                        SatelliteIndoorMeasurementList finalInputMeasurementsL5 = new SatelliteIndoorMeasurementList();
                        finalGnssData.asList().forEach(
                                s -> {
                                    SatelliteIndoorMeasurement m = new SatelliteIndoorMeasurement();
                                    m.setSvid(s.getSvid());
                                    m.setaccumulatedDeltaRangeMeters(s.getAccumulatedDeltaRangeMeters());
                                    m.setaccumulatedDeltaRangeUncertaintyMeters(s.getAccumulatedDeltaRangeUncertaintyMeters());
                                    m.setcarrierFrequencyHz(s.getCarrierFrequencyHz());
                                    m.setreceivedSvTimeUncertaintyNanos(s.getReceivedSvTimeUncertaintyNanos());
                                    m.setPseudorange(s.getPseudorange());
                                    m.setaccumulatedDeltaRangeState(s.getAccumulatedDeltaRangeState());
                                    m.setpseudorangeRateMetersPerSecond(s.getPseudorangeRateMetersPerSecond());
                                    m.setpseudorangeRateUncertaintyMetersPerSecond(s.getPseudorangeRateUncertaintyMetersPerSecond());
                                    finalInputMeasurementsL5.add(m);
                                });
                        SatelliteIndoorMeasurementList finalInputMeasurementsL1 = new SatelliteIndoorMeasurementList();
                        fliter = new GNSSProcessor.GNSSFilter(GNSSProcessor.GNSSFilter.flitermod.freL1);
                        finalGnssData = gnssData.
                                getSatelliteGNSSMeasurements().values().stream().filter(fliter).collect(ImmutableList.toImmutableList());
                        finalGnssData.asList().forEach(
                                s -> {
                                    SatelliteIndoorMeasurement m = new SatelliteIndoorMeasurement();
                                    m.setSvid(s.getSvid());
                                    m.setaccumulatedDeltaRangeMeters(s.getAccumulatedDeltaRangeMeters());
                                    m.setaccumulatedDeltaRangeUncertaintyMeters(s.getAccumulatedDeltaRangeUncertaintyMeters());
                                    m.setcarrierFrequencyHz(s.getCarrierFrequencyHz());
                                    m.setreceivedSvTimeUncertaintyNanos(s.getReceivedSvTimeUncertaintyNanos());
                                    m.setPseudorange(s.getPseudorange());
                                    m.setaccumulatedDeltaRangeState(s.getAccumulatedDeltaRangeState());
                                    m.setpseudorangeRateMetersPerSecond(s.getPseudorangeRateMetersPerSecond());
                                    m.setpseudorangeRateUncertaintyMetersPerSecond(s.getPseudorangeRateUncertaintyMetersPerSecond());
                                    finalInputMeasurementsL1.add(m);
                                });
                        Inputparameter inputparameter =new Inputparameter();
                        posresult = indoorPositionProcessor.run(finalInputMeasurementsL1,finalInputMeasurementsL5,inputparameter);
                        c.onReceive(coordExchange(posresult, gmocratorfixcoord, gdegZfixcoord,debugstring, mapID, mode));
                        finalInputMeasurementsL5.clear();
                        finalInputMeasurementsL1.clear();
                    } else {
                        c.onReceive(coordExchange(posresult, gmocratorfixcoord, gdegZfixcoord,"无测量数据", mapID, mode));
                    }
                } else {
                    c.onReceive(coordExchange(null, gmocratorfixcoord, 0,"室外模式", mapID, mode));
                }
            }
        }, period, period);
    }
    void addCallbacks(IPSMeasurement.Callback callback) {
        this.callback.set(callback);
    }
}


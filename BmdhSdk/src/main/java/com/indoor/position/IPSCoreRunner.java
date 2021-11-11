package com.indoor.position;



import static com.indoor.position.IPSMeasurement.Mode.INDOOR;
import static com.indoor.position.IPSMeasurement.Mode.OUTDOOR;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.indoor.position.swiggenerated.IndoorPositionMeasurement;
import com.indoor.position.swiggenerated.IndoorPositionProcessor;
import com.indoor.position.swiggenerated.SatelliteIndoorMeasurement;
import com.indoor.position.swiggenerated.SatelliteIndoorMeasurementList;
import com.indoor.position.swiggenerated.SatelliteInfo;
import com.indoor.position.swiggenerated.SatelliteInfoList;
import com.indoor.position.swiggenerated.Inputparameter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
    // TODO: refactor this
    private double[] bluetoothReferLocation={1,2,3};
    private IPSMeasurement.Mode mode= OUTDOOR;
    private long mapID;
    private boolean hasinitonedeKF=false;
    private boolean hasinittwodeKF=false;
    private int currentpath=0;//= GNSSProcessor.GNSSFilter.RoadDir.Lengthwise;
    private double[] pathRange;
	double[] fixedCoor= new double[]{0,0,1.0};
	boolean ispsemodeL1=true;
    //坏了一路的双发射机
/*    Integer listL1[]={1, 3, 24, 25,26, 27, 30, 32};
    Integer listL5[]={1, 3, 24, 25,26, 30, 32};*/
/*    Integer listL1[]={1, 2, 3, 4,5, 27, 30, 32};
    Integer listL5[]={1, 2, 3, 4,5, 30, 32};*/
    Integer listL1[]={1, 2, 3, 4,5,6, 7, 8,9,10,11,12};
    Integer listL5[]={1, 2, 3, 4,5,6, 7, 8,9,10,11,12};
    //移动停车场发射机
  /*  Integer listL1[]={1, 3, 24, 25,26, 27, 30, 32};
    Integer listL5[]={1, 3, 24, 25,26, 27,30, 32};*/
    /*公司*/
    /*double[] gmocratorfixcoord=new double[]{13536740.6489254,3649357.91954317,0};
    double gdegZfixcoord=4.3907;*/
    /*长兴*/
    /*
    double[] gmocratorfixcoord=new double[]{13553441.0197,3679297.281,0};
    double gdegZfixcoord=Math.PI*1.5;*/

    double[] gmocratorfixcoord=new double[]{0,0,0};
    double gdegZfixcoord=0;
/** 地下室 **/
/*    double[] roomCenter=new double[]{29.63,6.88};
    double[] threshold_x_y=new double[]{9,8};*/

        /*停车场*/
/*    double[] roomCenter=new double[]{7,9};
    double[] threshold_x_y=new double[]{13,14};*/
/*   *//* *//**//** 公司 **//*
    double[] roomCenter=new double[]{0.3,7};
    double[] threshold_x_y=new double[]{9,9};*/
    /** 移动停车场 **/
   /* double[] roomCenter=new double[]{0,15};
    double[] threshold_x_y=new double[]{4,17};*/
    /** 公司12颗星 **/
   double[] roomCenter=new double[]{5.1528,7.1252};
    double[] threshold_x_y=new double[]{20,10};
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


    void updateInputData(SatelliteInfoList s,Inputparameter inputparameter)
    {
        roomCenter=inputparameter.getRoomCenter();
        threshold_x_y=inputparameter.getThreshold_x_y();
        fixedCoor=inputparameter.getFixedCoor();
        double[] initpoint=new double[] {roomCenter[0],roomCenter[1],1.0} ;
        indoorPositionProcessor.updateSatelliteInfo(s,initpoint);
    }

    /**
     * 更新卫星序列号和定位模式是否仅用L1
     *
     * @param listL1 L1星序列号
     * @param listL5 L5星序列号
     * @param ispsemodeL1 定位模式是否仅用L1
     */
    void updateSatelliteSerial(Integer listL1[],Integer listL5[],boolean ispsemodeL1){
        this.listL1=listL1;
        this.listL5=listL5;
        this.ispsemodeL1=ispsemodeL1;
    }


    /**
     * 更新墨卡托转换参数
     *
     * @param gmocratorfixcoord
     * @param gdegZfixcoord
     */
    void updateMercatorConvertParameter(double[] gmocratorfixcoord, double gdegZfixcoord){
        this.gmocratorfixcoord=gmocratorfixcoord;
        this.gdegZfixcoord=gdegZfixcoord;
    }


    /**
     * Runner starts and initializes.
     */
    boolean startUp() {
        boolean state = gnssProcessor.startUp();
        if (state) {
            CallbackSchedule();
        }
        //蓝牙标签位置初始化
        bluetoothLabel =ImmutableMap.<String, List<Double>>builder().

               put("017", Arrays.<Double>asList(0.00,  -27.52,  1.3)).
                put("018", Arrays.<Double>asList(0.00,  -27.52,  1.3)).
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
         if ((maxId.equals("017") || maxId.equals("018")) && mode == OUTDOOR) {
                mode = INDOOR;
                mapID=1;
                currentpath=0;
                pathRange=pathrangeindex[currentpath];
                if(!hasinitonedeKF) {
                    indoorPositionProcessor.initialAPBCaKFOneDimen();
                    hasinitonedeKF=true;
                    debugstring="first time indoor,initialAbsolutePositioningBaseCarrierKFOneDimen";
                    Log.i(TAG, debugstring);
                }
            }
            if((maxId.equals("016"))) {

                if(!hasinittwodeKF) {
                    indoorPositionProcessor.initialAPBCKFTwoDimen();
                    hasinittwodeKF = true;
                    areaRange= new double[]{roomCenter[0] - threshold_x_y[0], roomCenter[1] - threshold_x_y[1], roomCenter[0] + threshold_x_y[0], roomCenter[1] + threshold_x_y[1]};
                    Log.i(TAG, "init initialAbsolutePositioningBaseCarrierKFTwoDimen");
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
                long deltaTimeMillis=0;
                if(gnssData != null) {
                    deltaTimeMillis = Utils.getCurrentTimeMillis() - gnssData.getCreateTimeMillis();
                }

                Inputparameter inputparameter =new Inputparameter(roomCenter,threshold_x_y,bluetoothCheckResult,bluetoothReferLocation,fixedCoor);

                if (mode == INDOOR) {
                    GNSSProcessor.GNSSFilter fliter;
                    if (gnssData != null&&gnssData.getSatelliteGNSSMeasurements().size()>0) {
                        if(deltaTimeMillis<3000) {
                            if(ispsemodeL1)
                            {
                                fliter = new GNSSProcessor.GNSSFilter(GNSSProcessor.GNSSFilter.flitermod.freL1);
                                fliter.setflitermode(GNSSProcessor.GNSSFilter.flitermod.freL1, Arrays.asList(listL1));}
                            else
                            {
                                fliter = new GNSSProcessor.GNSSFilter(GNSSProcessor.GNSSFilter.flitermod.freL5);
                                fliter.setflitermode(GNSSProcessor.GNSSFilter.flitermod.freL5, Arrays.asList(listL5));
                            }
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
                            fliter.setflitermode(GNSSProcessor.GNSSFilter.flitermod.freL1, Arrays.asList(listL1));
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
                            posresult = indoorPositionProcessor.run(finalInputMeasurementsL1,finalInputMeasurementsL5,inputparameter);
                            debugstring = "\n当前L5伪卫星数," + finalInputMeasurementsL5.size() +
                                    "\n当前L1伪卫星数," + finalInputMeasurementsL1.size() +
                                    "\nL5定位状态," + posresult.getL5locatestate() +
                                    "\nL5定位结果," + posresult.getL5_x() + "," + posresult.getL5_y();
                            c.onReceive(coordExchange(posresult, gmocratorfixcoord, gdegZfixcoord, debugstring, mapID, mode));
                            finalInputMeasurementsL5.clear();
                            finalInputMeasurementsL1.clear();
                        }
                        else
                        {
                            c.onReceive(coordExchange(posresult, gmocratorfixcoord, gdegZfixcoord,"重捕获复位", mapID, mode));
                            gnssProcessor.tearDown();
                            gnssProcessor.startUp();
                            indoorPositionProcessor.initialAPBCKFTwoDimen();
                        }
                    } else {
                            c.onReceive(coordExchange(posresult, gmocratorfixcoord, gdegZfixcoord,"无测量值", mapID, mode));
                    }
                } else {
                    //非室内模式
                    Log.i(TAG, "run: 室外模式！！");
                    c.onReceive(coordExchange(null, gmocratorfixcoord, 0,"室外模式", mapID, mode));
                }
            }
        }, period, period);
    }
    void addCallbacks(IPSMeasurement.Callback callback) {
        this.callback.set(callback);
    }
}


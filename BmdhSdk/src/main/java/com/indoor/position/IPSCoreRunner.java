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
    private final StepProcessor stepProcessor;
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


    /** 配置参数  **/
	double[] fixedCoor= new double[]{0,0,1.0};
	boolean ispsemodeL1=true;
/*    Integer[] listL1={1, 2, 3, 4,5,6, 7, 8};
    Integer[] listL5={1, 2, 3, 4,5,6, 7, 8};*/
    Integer[] listL1={1, 3, 24, 25,26,27, 30, 32};
    Integer[] listL5={1, 3, 24, 25,26,27, 30, 32};
    double[] gmocratorfixcoord=new double[]{0,0,0};
    double gdegZfixcoord=0;
   double[] roomCenter=new double[]{10,8};
    double[] threshold_x_y=new double[]{15,10};
    Map<String, List<Double>> bluetoothLabel;
    double jingweideg=292./180*Math.PI;
    /** 配置参数  **/

/*    double[] gmocratorfixcoord=new double[]{13536740.6489254,3649357.91954317,0};
    double gdegZfixcoord=4.3907;*/




    boolean bluetoothCheckResult = false;
    private IndoorPositionMeasurement posresult;
    IPSCoreRunner(
            GNSSProcessor gnssProcessor,
            SensorProcessor sensorProcessor,
            StepProcessor stepProcessor,
            BluetoothProcessor bluetoothProcessor,
            IndoorPositionProcessor indoorPositionProcessor) {
        this.gnssProcessor = gnssProcessor;
        this.stepProcessor = stepProcessor;
       // this.sensorProcessor = sensorProcessor;
        this.bluetoothProcessor = bluetoothProcessor;
        this.indoorPositionProcessor = indoorPositionProcessor;
        this.callback = new AtomicReference<>();
    }

    public static String twoDigitsString(double value) {

        return String.format("%.2f", value).toString();
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
     * Runner starts and initializes.
     */
    boolean startUp() {
        boolean state = gnssProcessor.startUp();
        if (state) {
            CallbackSchedule();
        }
       stepProcessor.startUp();
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
        stepProcessor.tearDown();
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
                    //areaRange= new double[]{roomCenter[0] - threshold_x_y[0], roomCenter[1] - threshold_x_y[1], roomCenter[0] + threshold_x_y[0], roomCenter[1] + threshold_x_y[1]};
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
            //areaRange= new double[]{roomCenter[0] - threshold_x_y[0], roomCenter[1] - threshold_x_y[1], roomCenter[0] + threshold_x_y[0], roomCenter[1] + threshold_x_y[1]};
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

                stepProcessor.jingweitoxy(jingweideg);
                double[] stepjingwei=new double[]{stepProcessor.getStepjing(),stepProcessor.getStepwei()};
                Inputparameter inputparameter =new Inputparameter(roomCenter,threshold_x_y,
                        bluetoothCheckResult,bluetoothReferLocation,
                        fixedCoor,stepjingwei);

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
                            SatelliteIndoorMeasurementList finalInputMeasurementsL1 = new SatelliteIndoorMeasurementList();
                            SatelliteIndoorMeasurementList finalInputMeasurementsL5 = new SatelliteIndoorMeasurementList();
                            if(gnssData!=null) {
                                ImmutableList<GNSSData.SatelliteGNSSMeasurements> finalGnssData = gnssData.
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
                                            finalInputMeasurementsL5.add(m);
                                        });

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
                                            //m.setaccumulatedDeltaRangeState(20);
                                            m.setpseudorangeRateMetersPerSecond(s.getPseudorangeRateMetersPerSecond());
                                            m.setpseudorangeRateUncertaintyMetersPerSecond(s.getPseudorangeRateUncertaintyMetersPerSecond());
                                            finalInputMeasurementsL1.add(m);
                                        });
                            }
                            posresult = indoorPositionProcessor.run(finalInputMeasurementsL1,finalInputMeasurementsL5,inputparameter);
                            debugstring = "\n当前L5伪卫星数," + finalInputMeasurementsL5.size() +
                                    "\n当前L1伪卫星数," + finalInputMeasurementsL1.size() +
                                    "\nL5定位状态," + posresult.getL5locatestate() +" 成功率"+twoDigitsString(posresult.getL5_z()*100)+"%"+
                                    "\nL5定位结果," + twoDigitsString(posresult.getL5_x()) + "," + twoDigitsString(posresult.getL5_y());
                            c.onReceive(coordExchange(posresult, gmocratorfixcoord, gdegZfixcoord, debugstring, mapID, mode));
                            finalInputMeasurementsL5.clear();
                            finalInputMeasurementsL1.clear();
                            stepProcessor.clearStepjingwei();
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


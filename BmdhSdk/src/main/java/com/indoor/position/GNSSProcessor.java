package com.indoor.position;

import android.annotation.SuppressLint;
import android.location.GnssClock;
import android.location.GnssMeasurement;
import android.location.GnssMeasurementsEvent;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * {@link GNSSProcessor} collects, sanitizes and calculates the GNSS raw data.
 */
@RequiresApi(api = Build.VERSION_CODES.O)
class GNSSProcessor {
    private static final LocationListener DEFAULT_LOCATION_LISTENER =new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location location) {

        }

        @Override
        public void onProviderDisabled(@NonNull String provider) {

        }
    };
    private static final String LOG_TAG = "GNSSProcessor";
    private static final String LOCATION_SERVICE_PROVIDER = "gps";
    private static final int MIN_INTERVAL_MILLIS = 1000;
    private static final long MAX_PRESERVED_DATA_MILLIS = 10000L;

    private final LocationManager locationManager;
    private final AtomicReference<GNSSData> currentPayload;
    private final ConcurrentLinkedQueue<GNSSData> historicData;

    private GnssMeasurementsEvent.Callback gnssCallback;

    /**
     * Constructor.
     */
    GNSSProcessor(LocationManager locationManager) {
        this.locationManager = locationManager;
        this.currentPayload = new AtomicReference<>();
        this.historicData = new ConcurrentLinkedQueue<>();
    }


    /**
     * {@link GNSSFilter} filters the gnss data by certain criteria.
     * */
    static class GNSSFilter implements Predicate<GNSSData.SatelliteGNSSMeasurements> {

        private flitermod flitermode; // TODO

        private final Set<Integer> LengthAllowSvid = Stream.of(1, 2, 3, 4,5).collect(Collectors.toSet());
        private final Set<Integer> CrossAllowSvid = Stream.of(4, 6, 7, 8).collect(Collectors.toSet());
        private Set<Integer> othersvids ;//= Stream.of(2, 4, 6, 8).collect(Collectors.toSet());


        public void setflitermode(flitermod p, List<Integer> svids){
            flitermode = p;
            if(svids!=null) {
                othersvids = new HashSet<>(svids);
            }
            else
            {
                othersvids=null;
            }
        }
        // 过滤模式
        enum flitermod {
            Lengthwise,
            crosswise,
            others,
            freL5,
            freL1
        }

        public GNSSFilter(flitermod fliter) {
            this.flitermode=fliter;
        }

        @Override
        public boolean test(GNSSData.SatelliteGNSSMeasurements s) {
            if(flitermode == flitermod.freL5) {
                if(othersvids!=null) {
                    return s.getFrequency() == GNSSData.Frequency.L5 && othersvids.contains(s.getSvid());
                }else{
                    return s.getFrequency() == GNSSData.Frequency.L5;
                }
            }
            else if (flitermode == flitermod.Lengthwise) {
                // 纵向1，3，5，6，L1
                return LengthAllowSvid.contains(s.getSvid()) && s.getFrequency() == GNSSData.Frequency.L1;
            }
            else if (flitermode == flitermode.crosswise) {
                // 横向
                return CrossAllowSvid.contains(s.getSvid()) && s.getFrequency() == GNSSData.Frequency.L1;
            }
            else if(flitermode==flitermode.others){
                return othersvids.contains(s.getSvid()) && s.getFrequency() == GNSSData.Frequency.L1;
            }else if(flitermode == flitermod.freL1) {
                if(othersvids!=null) {
                    return s.getFrequency() == GNSSData.Frequency.L1 && othersvids.contains(s.getSvid());
                }else{
                    return s.getFrequency() == GNSSData.Frequency.L1;
                }
            }else {
                return false;
            }
        }

        @Override
        public Predicate<GNSSData.SatelliteGNSSMeasurements> and(Predicate<? super GNSSData.SatelliteGNSSMeasurements> other) {
            return null;
        }

        @Override
        public Predicate<GNSSData.SatelliteGNSSMeasurements> negate() {
            return null;
        }

        @Override
        public Predicate<GNSSData.SatelliteGNSSMeasurements> or(Predicate<? super GNSSData.SatelliteGNSSMeasurements> other) {
            return null;
        }
    }

    /**
     * Initialization.
     */
    @SuppressLint("MissingPermission")
    boolean startUp() {
        if (gnssCallback != null) {
            // already initialized.
            return true;
        }
        gnssCallback = new GnssMeasurementsEvent.Callback() {
            @Override
            public void onGnssMeasurementsReceived(GnssMeasurementsEvent event) {
                super.onGnssMeasurementsReceived(event);
                setGNSSData(buildGNSSDataFromRawMeasurements(event));
            }

            @Override
            public void onStatusChanged(int status) {
                super.onStatusChanged(status);
            }
        };
        locationManager.requestLocationUpdates(
                LOCATION_SERVICE_PROVIDER, MIN_INTERVAL_MILLIS, 0, DEFAULT_LOCATION_LISTENER);
        return locationManager.registerGnssMeasurementsCallback(gnssCallback);
    }

    /**
     * Finish.
     */
    void tearDown() {
        if (gnssCallback == null) {
            return;
        }

        locationManager.unregisterGnssMeasurementsCallback(gnssCallback);
        historicData.clear();
        gnssCallback=null;
    }

    /**
     * Get the most recent GNSS measurements.
     */
    GNSSData getGNSSData() {
        return currentPayload.get();
    }


    /**
     * Get the data within {@literal millisSeconds} ago.
     */
    ImmutableList<GNSSData> getLastMillisData(long milliSeconds) {
        long offset = System.currentTimeMillis() - milliSeconds;
        return historicData.stream()
                .filter(s -> s.getCreateTimeMillis() > offset)
                .collect(ImmutableList.toImmutableList());
    }

    /**
     * Set the most recent GNSS measurement.
     */
    void setGNSSData(GNSSData data) {
//        Log.d(LOG_TAG, String.format("GNSS data received. %s", data.toString()));
        this.currentPayload.set(data);
        addDataToQueue(data);
    }

    void addDataToQueue(GNSSData data) {
        historicData.add(data);
        GNSSData topData = historicData.peek();
        long offset = System.currentTimeMillis() - MAX_PRESERVED_DATA_MILLIS;
        while (topData != null && topData.getCreateTimeMillis() < offset) {
            historicData.poll();
            topData = historicData.peek();
        }
    }


    GNSSData buildGNSSDataFromRawMeasurements(GnssMeasurementsEvent rawMeasurements) {
        HashMap<GNSSData.UniqueSatelliteKey, GNSSData.SatelliteGNSSMeasurements.SatelliteGNSSMeasurementsBuilder>
                builders = new HashMap<>();
        GNSSData preSatellite = currentPayload.get();
        ImmutableMap<GNSSData.UniqueSatelliteKey, GNSSData.SatelliteGNSSMeasurements> preGNSS = null;
        if (preSatellite != null) {
            preGNSS = preSatellite.getSatelliteGNSSMeasurements();
        }
        GnssClock rawclock =rawMeasurements.getClock();
        for (GnssMeasurement raw : rawMeasurements.getMeasurements()) {
            if (raw.getConstellationType() != GnssStatus.CONSTELLATION_GPS||(raw.getCn0DbHz()<25)) {
                continue;
            }
            @NonNull GNSSData.SatelliteGNSSMeasurements.SatelliteGNSSMeasurementsBuilder builder =
                    Objects.requireNonNull(builders.getOrDefault(
                            raw.getSvid(),
                            GNSSData.SatelliteGNSSMeasurements.builder()));
            // build raw data.
            GNSSData.Frequency freType = GNSSData.getFrequencyType(raw.getCarrierFrequencyHz());
//            if (freType == GNSSData.Frequency.Unknown) {
//                // 暂不支持的频率
//                continue;
//            }
            GNSSData.UniqueSatelliteKey uniqueKey = GNSSData.UniqueSatelliteKey.builder().
                    svid(raw.getSvid()).frequency(freType).build();
            double pseudorange = GNSSData.calPseudorange(raw.getReceivedSvTimeNanos());
            builder.smoothpseudorange(pseudorange);

            builder.hasPre1s(false);
            if (preGNSS != null && preGNSS.containsKey(uniqueKey)) {
                GNSSData.SatelliteGNSSMeasurements pre = preGNSS.get(uniqueKey);
                long deltaNanos = raw.getReceivedSvTimeNanos() - pre.getReceivedSvTimeNanos();
                if (800_000_000 < deltaNanos && deltaNanos < 1200_000_000) {
                    builder.hasPre1s(true);
                    builder.deltaAccumulatedDeltaRangeMeters(raw.getAccumulatedDeltaRangeMeters() - pre.getAccumulatedDeltaRangeMeters());
                    builder.prePseudorangeRateMetersPerSecond(pre.getPseudorangeRateMetersPerSecond());
                    builder.smoothpseudorange(GNSSData.calPresmoothedpseudorange(pseudorange, raw.getPseudorangeRateMetersPerSecond(), pre.getSmoothpseudorange()));
                }
            }

            builder.svid(raw.getSvid())
                    .timeNanos(rawclock.getTimeNanos())
                    .leapSecond(rawclock.getLeapSecond())
                    .timeUncertaintyNanos(rawclock.getTimeUncertaintyNanos())
                    .fullBiasNanos(rawclock.getFullBiasNanos())
                    .biasNanos(rawclock.getBiasNanos())
                    .biasUncertaintyNanos(rawclock.getBiasUncertaintyNanos())
                    .driftNanosPerSecond(rawclock.getDriftNanosPerSecond())
                    .driftUncertaintyNanosPerSecond(rawclock.getDriftUncertaintyNanosPerSecond())

                    .timeOffsetNanos(raw.getTimeOffsetNanos())
                    .state(raw.getState())
                    .automaticGainControlLevelDb(raw.getAutomaticGainControlLevelDb())
                    .receivedSvTimeNanos(raw.getReceivedSvTimeNanos())
                    .receivedSvTimeUncertaintyNanos(raw.getReceivedSvTimeUncertaintyNanos())
                    .cn0DbHz(raw.getCn0DbHz())
                    .pseudorangeRateMetersPerSecond(
                            raw.getPseudorangeRateMetersPerSecond())
                    .pseudorangeRateUncertaintyMetersPerSecond(
                            raw.getPseudorangeRateUncertaintyMetersPerSecond()
                    )
                    .accumulatedDeltaRangeState(raw.getAccumulatedDeltaRangeState())
                    .accumulatedDeltaRangeMeters(raw.getAccumulatedDeltaRangeMeters())
                    .accumulatedDeltaRangeUncertaintyMeters(
                            raw.getAccumulatedDeltaRangeUncertaintyMeters())

                    .carrierFrequencyHz(raw.getCarrierFrequencyHz())
                    .carrierCycles(raw.getCarrierCycles())
                    .carrierPhase(raw.getCarrierPhase())
                    .carrierPhaseUncertainty(raw.getCarrierPhaseUncertainty())
                    .multipathIndicator(raw.getMultipathIndicator())

                    .constellationType(raw.getConstellationType())
                    .multipathIndicator(raw.getMultipathIndicator())
                    .agcDb(raw.getAutomaticGainControlLevelDb())

/*                    .fullInterSignalBiasNanos(raw.getFullInterSignalBiasNanos())
                    .fullInterSignalBiasUncertaintyNanos(raw.getFullInterSignalBiasUncertaintyNanos())
                    .satelliteInterSignalBiasNanos(raw.getSatelliteInterSignalBiasNanos())
                    .satelliteInterSignalBiasUncertaintyNanos(raw.getSatelliteInterSignalBiasUncertaintyNanos())
                    .codeType(raw.getCodeType())*/


                    .frequency(freType)
                    .pseudorange(pseudorange)
                    .receivedSvTimeNanos(raw.getReceivedSvTimeNanos())
                    .receivedSvTimeUncertaintyNanos(
                            raw.getReceivedSvTimeUncertaintyNanos());


/*            GNSSData.UniqueSatelliteKey key = GNSSData.UniqueSatelliteKey.builder().svid(raw.getSvid())
                    .frequency(GNSSData.getFrequencyType(raw.getCarrierFrequencyHz())).build();*/
            GNSSData.UniqueSatelliteKey key = GNSSData.UniqueSatelliteKey.builder().svid(raw.getSvid())
                    .frequency(freType).build();
            builders.put(key, builder);
        }
        return GNSSData.builder()
                .satelliteGNSSMeasurements(
                        builders.entrySet().stream().collect(
                                ImmutableMap.toImmutableMap(
                                        Map.Entry::getKey, s -> s.getValue().build())
                        ))
                .build();
    }
}
package com.indoor.position;

import com.google.common.collect.ImmutableMap;

import java.math.BigDecimal;
import java.util.Objects;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.Value;


/**
 * {@link GNSSData} contains information received from Gnss module.
 */
@Value
@Builder
class GNSSData {
    private static final BigDecimal T1e9 = BigDecimal.valueOf(1e9);
    private static final BigDecimal c_ON_NANO = BigDecimal.valueOf(299792458E-9);
    private static final long M = 20;
    /**
     * {@link SatelliteGNSSMeasurements} is the GNSS measurements based on per satellite.
     */
    @Data
    @Value
    @Builder
    static class SatelliteGNSSMeasurements {
        @Builder.Default
        long createTimeMillis = Utils.getCurrentTimeMillis();
        double timeNanos;
        double leapSecond;
        double timeUncertaintyNanos;
        double fullBiasNanos;
        double biasNanos;
        double biasUncertaintyNanos;
        double driftNanosPerSecond;
        double driftUncertaintyNanosPerSecond;
        double hardwareClockDiscontinuityCount ;
        double timeOffsetNanos ;
        int state ;
        double automaticGainControlLevelDb;
        long receivedSvTimeNanos ;
        double receivedSvTimeUncertaintyNanos ;
        double cn0DbHz ;
        double pseudorangeRateMetersPerSecond;
        double pseudorangeRateUncertaintyMetersPerSecond;
        int accumulatedDeltaRangeState;
        double accumulatedDeltaRangeMeters;
        double accumulatedDeltaRangeUncertaintyMeters;
        float carrierFrequencyHz;
        double carrierCycles;
        double carrierPhase;
        double carrierPhaseUncertainty;
        double multipathIndicator;
        double anrInDb ;
        double constellationType;
        double agcDb ;
        double basebandCn0DbHz;
        double fullInterSignalBiasNanos;
        double fullInterSignalBiasUncertaintyNanos;
        double satelliteInterSignalBiasNanos;
        double satelliteInterSignalBiasUncertaintyNanos;
        String codeType ;
        double chipsetElapsedRealtimeNanos;
        double pseudorange;
        double smoothpseudorange;
        boolean hasPre1s;
        double deltaAccumulatedDeltaRangeMeters;
        double prePseudorangeRateMetersPerSecond;

        Integer svid;
        Frequency frequency;

    }

    enum Frequency {
        L1,
        L5,
        Unknown
    }

    @Value
    @Builder
    static class UniqueSatelliteKey {
        Integer svid;
        Frequency frequency;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            UniqueSatelliteKey that = (UniqueSatelliteKey) o;
            return Objects.equals(svid, that.svid) && frequency == that.frequency;
        }

        @Override
        public int hashCode() {
            return Objects.hash(svid, frequency);
        }
    }

    // 计算频点
    static Frequency getFrequencyType(double frequency) {
        if (Math.abs(frequency / 1e6 - 1575.42) < 0.1) {
            return Frequency.L1;
        }
        if (Math.abs(frequency / 1e6 - 1176.45) < 0.1) {
            return Frequency.L5;
        }
        return Frequency.Unknown;
    }

    // 计算伪距
    static double calPseudorange(double receivedSvTimeNanos) {
        // (1e9 - ReceivedSvTimeNanos % 1e9) * c_on_nano
        BigDecimal[] result = BigDecimal.valueOf(receivedSvTimeNanos).divideAndRemainder(T1e9); // 商和余数
        return c_ON_NANO.multiply(T1e9.subtract(result[1])).doubleValue();
    }

    static double calPresmoothedpseudorange(double pseudorange, double pseudorangeRateMetersPerSecond, double presmoothedpseudorange) {
        return pseudorange / M + (presmoothedpseudorange+pseudorangeRateMetersPerSecond) * (M-1) / M;
    }


    @Builder.Default
    long createTimeMillis = Utils.getCurrentTimeMillis(); // TODO: use event time millis
    ImmutableMap<UniqueSatelliteKey, SatelliteGNSSMeasurements> satelliteGNSSMeasurements;
}

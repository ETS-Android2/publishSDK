package com.indoor.position;

import com.google.common.collect.ImmutableMap;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.ToString;
import lombok.Value;

@Value
@Builder
class SensorData {
    private static final ImmutableMap<SensorType, String> SENSOR_TYPE_TO_UNIT =
            ImmutableMap.<SensorType, String>builder()
                    .put(SensorType.ACCELEROMETER, "acc")
                    .put(SensorType.MAGNETIC_FIELD, "mag")
                    .put(SensorType.GYROSCOPE, "gyr")
                    .put(SensorType.ORIENTATION, "ori")
                    .put(SensorType.ROTATION, "rot")
                    .build();
    private static final String UNKNOWN_UNIT = "unknown";

    public static SensorDimMeasurements[] ArrayFromObject(Object[] objs) {
        SensorDimMeasurements[] ret = new SensorDimMeasurements[objs.length];
        for(int i=0;i<objs.length;i++){
            ret[i] = (SensorDimMeasurements) objs[i];
        }
        return ret;
    }

    enum SensorType {
        ACCELEROMETER,

        MAGNETIC_FIELD,

        GYROSCOPE,

        ORIENTATION,

        ROTATION
    }


    @Builder
    @AllArgsConstructor
    @ToString
    @Value
    static class SensorDimMeasurements {
        float axisX;

        float axisY;

        float axisZ;

        long createTimeMillis;

        SensorType sensorType;

        /**
         * Description of the unit.
         */
        String unit;

        SensorDimMeasurements(SensorType type) {
            this.axisX = 0;
            this.axisY = 0;
            this.axisZ = 0;
            this.createTimeMillis = Utils.getCurrentTimeMillis();
            this.sensorType = type;
            this.unit = SENSOR_TYPE_TO_UNIT.getOrDefault(type, "unknown");
        }

        static SensorDimMeasurementsBuilder getBuilder(SensorType type) {
            return builder()
                    .sensorType(type)
                    .unit(SENSOR_TYPE_TO_UNIT.getOrDefault(type, UNKNOWN_UNIT));
        }
    }

    /**
     * Accelerometer sensor measurements.
     */
    SensorDimMeasurements accMeasurements;

    /**
     * Magnetic field sensor measurements.
     */
    SensorDimMeasurements magMeasurements;

    /**
     * Gyroscope sensor measurements.
     */
    SensorDimMeasurements gyroscopeMeasurements;

    /**
     * Orientation sensor measurements.
     */
    SensorDimMeasurements orientationMeasurements;

    SensorDimMeasurements rotMeasurements;
}





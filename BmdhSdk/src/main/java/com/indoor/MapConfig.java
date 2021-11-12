package com.indoor;

import com.google.gson.annotations.SerializedName;

import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class MapConfig {

    @SerializedName("dataConfig")
    private List<DataConfigDTO> dataConfig;

    @NoArgsConstructor
    @Data
    public static class DataConfigDTO {
        @SerializedName("mapid")
        private String mapid;
        @SerializedName("SatelliteInfo")
        private List<SatelliteInfoDTO> satelliteInfo;
        @SerializedName("roomCenter")
        private RoomCenterDTO roomCenter;
        @SerializedName("threshold_x_y")
        private ThresholdXYDTO thresholdXY;
        @SerializedName("fixedCoor")
        private FixedCoorDTO fixedCoor;
        @SerializedName("listL1")
        private List<Integer> listL1;
        @SerializedName("listL5")
        private List<Integer> listL5;
        @SerializedName("gmocratorfixcoord")
        private GmocratorfixcoordDTO gmocratorfixcoord;
        @SerializedName("gdegZfixcoord")
        private Integer gdegZfixcoord;
        @SerializedName("ispsemodeL1")
        private Boolean ispsemodeL1;

        @NoArgsConstructor
        @Data
        public static class RoomCenterDTO {
            @SerializedName("x")
            private Double x;
            @SerializedName("y")
            private Double y;
        }

        @NoArgsConstructor
        @Data
        public static class ThresholdXYDTO {
            @SerializedName("x")
            private Integer x;
            @SerializedName("y")
            private Integer y;
        }

        @NoArgsConstructor
        @Data
        public static class FixedCoorDTO {
            @SerializedName("x")
            private Integer x;
            @SerializedName("y")
            private Integer y;
            @SerializedName("z")
            private Double z;
        }

        @NoArgsConstructor
        @Data
        public static class GmocratorfixcoordDTO {
            @SerializedName("x")
            private Integer x;
            @SerializedName("y")
            private Integer y;
            @SerializedName("z")
            private Integer z;
        }

        @NoArgsConstructor
        @Data
        public static class SatelliteInfoDTO {
            @SerializedName("x")
            private double x;
            @SerializedName("y")
            private double y;
            @SerializedName("z")
            private double z;
            @SerializedName("svid")
            private Integer svid;
        }
    }
}

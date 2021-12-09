package com.indoor;

import androidx.annotation.Keep;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;
@Keep
@NoArgsConstructor
@Data
public class MapConfig implements Serializable {

    @SerializedName("dataConfig")
    private List<DataConfigDTO> dataConfig;
    @Keep
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
        private Double gdegZfixcoord;
        @SerializedName("ispsemodeL1")
        private Boolean ispsemodeL1;
        @SerializedName("versionNum")
        private int versionNum;
        @Keep
        @NoArgsConstructor
        @Data
        public static class RoomCenterDTO {
            @SerializedName("x")
            private Double x;
            @SerializedName("y")
            private Double y;
        }
        @Keep
        @NoArgsConstructor
        @Data
        public static class ThresholdXYDTO {
            @SerializedName("x")
            private Double x;
            @SerializedName("y")
            private Double y;
        }
        @Keep
        @NoArgsConstructor
        @Data
        public static class FixedCoorDTO {
            @SerializedName("x")
            private Double x;
            @SerializedName("y")
            private Double y;
            @SerializedName("z")
            private Double z;
        }
        @Keep
        @NoArgsConstructor
        @Data
        public static class GmocratorfixcoordDTO {
            @SerializedName("x")
            private Double x;
            @SerializedName("y")
            private Double y;
            @SerializedName("z")
            private Double z;
        }
        @Keep
        @NoArgsConstructor
        @Data
        public static class SatelliteInfoDTO {
            @SerializedName("x")
            private Double x;
            @SerializedName("y")
            private Double y;
            @SerializedName("z")
            private Double z;
            @SerializedName("svid")
            private Integer svid;
        }
    }
}

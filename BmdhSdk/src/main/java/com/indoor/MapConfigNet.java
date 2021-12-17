package com.indoor;

import android.location.Address;

import androidx.annotation.Keep;

import com.google.gson.annotations.SerializedName;

import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 与服务端的地图数据相对应
 * create by Aaron on 2021/1214
 *
 */
@Keep
@NoArgsConstructor
@Data
public class MapConfigNet {
    @SerializedName("projectAreaId")
    private String projectAreaId;
    @SerializedName("projectId")
    private Long projectId;
    @SerializedName("lgtLatDeg")
    private Double lgtLatDeg;
    @SerializedName("logicDelRelation")
    private Integer logicDelRelation;
    @SerializedName("pseModeOne")
    private Integer pseModeOne;
    @SerializedName("versionNum")
    private Integer versionNum;
    @SerializedName("xxFixedCoor")
    private Double xxFixedCoor;
    @SerializedName("yyFixedCoor")
    private Double yyFixedCoor;
    @SerializedName("zzFixedCoor")
    private Double zzFixedCoor;
    @SerializedName("xxRoomCenter")
    private Double xxRoomCenter;
    @SerializedName("yyRoomCenter")
    private Double yyRoomCenter;
    @SerializedName("xxThreshold")
    private Double xxThreshold;
    @SerializedName("yyThreshold")
    private Double yyThreshold;
    @SerializedName("gmocratorFixcoordDtos")
    private List<GmocratorFixcoordDtosDTO> gmocratorFixcoordDtos;
    @SerializedName("satelliteInfo")
    private List<SatelliteInfoDTO> satelliteInfo;
    @SerializedName("sequenceDtos")
    private List<SequenceDtosDTO> sequenceDtos;
    @Keep
    @NoArgsConstructor
    @Data
    public static class GmocratorFixcoordDtosDTO {
        @SerializedName("gdegzFixcoord")
        private String gdegzFixcoord;
        @SerializedName("mapType")
        private Integer mapType;
        @SerializedName("xxGmocratorFixcoord")
        private String xxGmocratorFixcoord;
        @SerializedName("yyGmocratorFixcoord")
        private String yyGmocratorFixcoord;
        @SerializedName("zzGmocratorFixcoord")
        private String zzGmocratorFixcoord;
    }
    @Keep
    @NoArgsConstructor
    @Data
    public static class SatelliteInfoDTO {
        @SerializedName("svid")
        private Integer svid;
        @SerializedName("xxAxisCoordinate")
        private String xxAxisCoordinate;
        @SerializedName("yyAxisCoordinate")
        private String yyAxisCoordinate;
        @SerializedName("zzAxisCoordinate")
        private String zzAxisCoordinate;
    }
    @Keep
    @NoArgsConstructor
    @Data
    public static class SequenceDtosDTO {
        @SerializedName("pseModeName")
        private String pseModeName;
        @SerializedName("spectrumList")
        private List<Integer> spectrumList;
    }

    public Double getXxAxisCoordinate(){
        return 0d;
    }
    public Double getYyAxisCoordinate(){
        return 0d;
    }
    public Double getZzAxisCoordinate(){
        return 0d;
    }
}

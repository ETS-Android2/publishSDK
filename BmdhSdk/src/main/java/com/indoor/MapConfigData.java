package com.indoor;

import android.text.TextUtils;

import androidx.annotation.Keep;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.indoor.data.SDKRepository;
import com.indoor.utils.KLog;
import com.indoor.utils.RxEncryptTool;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
@Keep
public class MapConfigData {
    public static final String PSE_MODE_NAME_L1 = "L1";
    public static final String PSE_MODE_NAME_L5 = "L5";
    private static final String TAG = "MapConfigData";

    @SerializedName("projectAreaId")
    private String projectAreaId;
    @SerializedName("projectId")
    private Long projectId;
    @SerializedName("pseModeOne")
    private Integer pseModeOne;
    @SerializedName("lgtLatDeg")
    private Double lgtLatDeg;
    @SerializedName("logicDelRelation")
    private Integer logicDelRelation;
    @SerializedName("versionNum")
    private Integer versionNum;
    @SerializedName("xxFixedCoor")
    private Double xxFixedCoor;
    @SerializedName("xxRoomCenter")
    private Double xxRoomCenter;
    @SerializedName("xxThreshold")
    private Double xxThreshold;
    @SerializedName("yyFixedCoor")
    private Double yyFixedCoor;
    @SerializedName("yyRoomCenter")
    private Double yyRoomCenter;
    @SerializedName("yyThreshold")
    private Double yyThreshold;
    @SerializedName("zzFixedCoor")
    private Double zzFixedCoor;
    @SerializedName("gmocratorParameter")
    private String gmocratorParameter;
    @SerializedName("satelliteInfo")
    private List<SatelliteInfoDTO> satelliteInfo;
    @SerializedName("sequenceDtos")
    private List<SequenceDtosDTO> sequenceDtos;

    @NoArgsConstructor
    @Data
    @Keep
    public static class SatelliteInfoDTO {
        @SerializedName("svid")
        private Integer svid;
        @SerializedName("xxAxisCoordinate")
        private Double xxAxisCoordinate;
        @SerializedName("yyAxisCoordinate")
        private Double yyAxisCoordinate;
        @SerializedName("zzAxisCoordinate")
        private Double zzAxisCoordinate;
    }

    @NoArgsConstructor
    @Data
    @Keep
    public static class SequenceDtosDTO {
        @SerializedName("pseModeName")
        private String pseModeName;
        @SerializedName("spectrum")
        private String spectrum;
    }

    @Keep
    @NoArgsConstructor
    @Data
    public static class GmocratorParameterDTO {
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

        public Double getGdegzFixcoord(String key){
            Double result =0d;
            try{
                result= Double.parseDouble(RxEncryptTool.decryptBase64_3DES(gdegzFixcoord, key));
            }catch (Exception e){
                KLog.e(TAG,"getGdegzFixcoord error:"+e.getMessage());
            }
            return result;
        }
        public Double getXxGmocratorFixcoord(String key){
            Double result =0d;
            try{
                result= Double.parseDouble(RxEncryptTool.decryptBase64_3DES(xxGmocratorFixcoord, key));
            }catch (Exception e){
                KLog.e(TAG,"getXxGmocratorFixcoord error:"+e.getMessage());
            }
            return result;
        }
        public Double getYyGmocratorFixcoord(String key){
            Double result =0d;
            try{
                result= Double.parseDouble(RxEncryptTool.decryptBase64_3DES(yyGmocratorFixcoord, key));
            }catch (Exception e){
                KLog.e(TAG,"getYyGmocratorFixcoord error:"+e.getMessage());
            }
            return result;
        }
        public Double getZzGmocratorFixcoord(String key){
            Double result =0d;
            try{
                result= Double.parseDouble(RxEncryptTool.decryptBase64_3DES(zzGmocratorFixcoord, key));
            }catch (Exception e){
                KLog.e(TAG,"getZzGmocratorFixcoord error:"+e.getMessage());
            }
            return result;
        }
    }

    @Keep
    @NoArgsConstructor
    @Data
    public static class GmocratorParameterDecypt {
        @SerializedName("gdegzFixcoord")
        private Double gdegzFixcoord;
        @SerializedName("mapType")
        private Integer mapType;
        @SerializedName("xxGmocratorFixcoord")
        private Double xxGmocratorFixcoord;
        @SerializedName("yyGmocratorFixcoord")
        private Double yyGmocratorFixcoord;
        @SerializedName("zzGmocratorFixcoord")
        private Double zzGmocratorFixcoord;

        public GmocratorParameterDecypt(Double gdegzFixcoord, Integer mapType, Double xxGmocratorFixcoord, Double yyGmocratorFixcoord, Double zzGmocratorFixcoord) {
            this.gdegzFixcoord = gdegzFixcoord;
            this.mapType = mapType;
            this.xxGmocratorFixcoord = xxGmocratorFixcoord;
            this.yyGmocratorFixcoord = yyGmocratorFixcoord;
            this.zzGmocratorFixcoord = zzGmocratorFixcoord;
        }
    }



    /**
     * 获取L1/L5卫星序列号
     *
     * @param pseName 卫星序列名
     * @return
     */
    public String getSpectrumList(String pseName){
        if(TextUtils.isEmpty(pseName)){
            return null;
        }
        String result = "";
        List<SequenceDtosDTO>sequenceDtosDTOList=getSequenceDtos();
        for(SequenceDtosDTO sequenceDtosDTO:sequenceDtosDTOList){
            if(sequenceDtosDTO.getPseModeName().equals(pseName)){
                result=sequenceDtosDTO.getSpectrum();
                break;
            }
        }
        return result;
    }

    public GmocratorParameterDecypt getGmocratorFixcoordDecypt(String key) throws Exception{
        GmocratorParameterDecypt result = null;
        String desStr = RxEncryptTool.decryptBase64_3DES(gmocratorParameter, key);
        Gson gson = new Gson();
        GmocratorParameterDTO gmocratorParameterDTO =null;
            gmocratorParameterDTO = gson.fromJson(desStr, GmocratorParameterDTO.class);
            result=new GmocratorParameterDecypt(gmocratorParameterDTO.getGdegzFixcoord(SDKRepository.getSalt()), gmocratorParameterDTO.mapType,
                    gmocratorParameterDTO.getXxGmocratorFixcoord(SDKRepository.getSalt()), gmocratorParameterDTO.getYyGmocratorFixcoord(SDKRepository.getSalt()), gmocratorParameterDTO.getZzGmocratorFixcoord(SDKRepository.getSalt()));
        return result;
    }
}

package com.indoor;

import android.content.Context;
import android.text.TextUtils;

import java.util.HashMap;

public class BmdhIndoorConfig {
    public static final HashMap<String,double[]> FIX_COORD =new HashMap<>();
    private static final String MAP_50078="50078";

    public Context context;
    public String sdcardRootPath;
    public String appFolderName;
    public String appId;
    public String appKey;
    public String secretKey;


    static {
        FIX_COORD.put(MAP_50078,new double[]{13536740.8542030,3649358.63141886,3.4312});
    }

    private BmdhIndoorConfig(Context var1, String var2, String var3, String var4, String var5, String var6) {
        this.context = var1;
        this.sdcardRootPath = var2;
        this.appFolderName = var3;
        this.appId = var4;
        this.appKey = var5;
        this.secretKey = var6;
    }

    public static double[] getFixCoord(String mapID){
        double []deafult=new double[]{0,0,0};
        if(TextUtils.isEmpty(mapID)){
            return deafult;
        }
        if(FIX_COORD.containsKey(mapID)){
            return FIX_COORD.get(mapID);
        }
        return deafult;
    }

    public static class Builder {
        private Context context;
        private String sdcardRootPath;
        private String appFolderName;
        private String appId;
        private String appKey;
        private String secretKey;

        public Builder() {
        }

        public BmdhIndoorConfig.Builder DEAFULT(Context var1) {
            this.context = var1;
            return this;
        }

        public BmdhIndoorConfig.Builder context(Context var1) {
            this.context = var1;
            return this;
        }

        public BmdhIndoorConfig.Builder sdcardRootPath(String var1) {
            this.sdcardRootPath = var1;
            return this;
        }

        public BmdhIndoorConfig.Builder appFolderName(String var1) {
            this.appFolderName = var1;
            return this;
        }

        public BmdhIndoorConfig.Builder appId(String var1) {
            this.appId = var1;
            return this;
        }

        public BmdhIndoorConfig.Builder appKey(String var1) {
            this.appKey = var1;
            return this;
        }

        public BmdhIndoorConfig.Builder secretKey(String var1) {
            this.secretKey = var1;
            return this;
        }

        public BmdhIndoorConfig build() {
            BmdhIndoorConfig.Builder var10002 = this;
            BmdhIndoorConfig.Builder var10003 = this;
            BmdhIndoorConfig.Builder var10004 = this;
            BmdhIndoorConfig.Builder var10005 = this;
            BmdhIndoorConfig.Builder var10006 = this;
            Context var6 = this.context;
            String var1 = var10006.sdcardRootPath;
            String var2 = var10005.appFolderName;
            String var3 = var10004.appId;
            String var4 = var10003.appKey;
            String var5 = var10002.secretKey;
            return new BmdhIndoorConfig(var6, var1, var2, var3, var4, var5);
        }
    }
}

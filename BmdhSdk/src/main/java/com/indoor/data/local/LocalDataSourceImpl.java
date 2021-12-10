package com.indoor.data.local;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;

import androidx.room.Room;

import com.indoor.data.local.db.UserActionData;
import com.indoor.utils.KLog;
import com.indoor.utils.RxAppUtils;
import com.indoor.utils.RxFileUtils;
import com.indoor.utils.Utils;

import java.io.File;
import java.util.List;

/**
 * 本地数据源，可配合Room框架使用
 * Created by Aaron on 2019/3/26.
 */
public class LocalDataSourceImpl implements LocalDataSource {
    private static final String TAG="LocalDataSourceImpl";
    private volatile static LocalDataSourceImpl INSTANCE = null;
    private static final String META_DATA = "com.bmdh.indoorsdk.API_KEY";
    private static final String KEY_TOKEN = "Token";
    private volatile static SDKDataBase mSDKDataBase;
    private volatile static String  DATABASE_PATH;
    private volatile String token="";
    private volatile String apiKey="";
    private volatile String shaCode="";
    private volatile String packageName="";
    private volatile String areaId="";
    public static LocalDataSourceImpl getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (LocalDataSourceImpl.class) {
                if (INSTANCE == null) {
                    INSTANCE = new LocalDataSourceImpl();
                    DATABASE_PATH = Utils.getContext().getExternalFilesDir(null)+File.separator+"mapdb";
                    RxFileUtils.createOrExistsDir(DATABASE_PATH);
                    mSDKDataBase= Room.databaseBuilder(context.getApplicationContext(),
                            SDKDataBase.class, DATABASE_PATH+File.separator+"sdkdata.db").allowMainThreadQueries().build();
                }
            }
        }
        return INSTANCE;
    }


    public static void destroyInstance() {
        INSTANCE = null;
    }

    private LocalDataSourceImpl() {
        //数据库Helper构建
    }

    @Override
    public String getApiKey() {
        if(TextUtils.isEmpty(apiKey)){
            ApplicationInfo appInfo = null;
            try {
                appInfo = Utils.getContext().getPackageManager().getApplicationInfo(Utils.getContext().getPackageName(), PackageManager.GET_META_DATA);
                apiKey =appInfo.metaData.getString(META_DATA);
            } catch (Exception e) {
                KLog.e(TAG, "get meta-data error:"+e.getMessage());
                return "";
            }
        }
        return apiKey;
    }

    @Override
    public String getPackageName() {
        if(TextUtils.isEmpty(packageName)){
            packageName= Utils.getContext().getPackageName();
        }
        return packageName;
    }

    @Override
    public String getShaCode() {
        if(TextUtils.isEmpty(shaCode)){
            shaCode= RxAppUtils.getAppSignatureSHA1(Utils.getContext());
        }
        return shaCode;
    }

    @Override
    public void saveToken(String token) {
        this.token=token;
    }

    @Override
    public void saveUnitId(int unitId) {

    }

    @Override
    public String getToken() {
        return this.token;
    }

    @Override
    public void saveAreaId(String areaId) {
        this.areaId = areaId;
    }

    @Override
    public String getAreaId() {
        return areaId;
    }

    @Override
    public int getUnitID() {
        return 0;
    }

    @Override
    public void saveUserActionDataToDB(UserActionData... userActionData) {
        mSDKDataBase.getUserActionDao().insertActionData(userActionData);
    }

    @Override
    public  void deleteUserActionDataToDB(List<UserActionData> userActionDatas) {
        mSDKDataBase.getUserActionDao().deleteActionData(userActionDatas);
    }

    @Override
    public List<UserActionData> getLimitUserActionDataToDB() {
        return mSDKDataBase.getUserActionDao().getTopTenActionData();
    }
}

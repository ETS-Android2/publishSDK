package com.indoor.data.local;

import android.content.Context;

import androidx.room.Room;

import com.indoor.data.local.db.UserActionData;

import java.io.File;
import java.util.List;

/**
 * 本地数据源，可配合Room框架使用
 * Created by goldze on 2019/3/26.
 */
public class LocalDataSourceImpl implements LocalDataSource {
    private volatile static LocalDataSourceImpl INSTANCE = null;
    private static final String KEY_TOKEN = "Token";
    private volatile static SDKDataBase mSDKDataBase;
    private volatile static String  DATABASE_PATH;
    private volatile String token="";
    public static LocalDataSourceImpl getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (LocalDataSourceImpl.class) {
                if (INSTANCE == null) {
                    INSTANCE = new LocalDataSourceImpl();
                    DATABASE_PATH = context.getExternalFilesDir("logs")+ File.separator+"mapdata";
                    mSDKDataBase= Room.databaseBuilder(context.getApplicationContext(),
                            SDKDataBase.class, DATABASE_PATH).build();
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
    public int getUnitID() {
        return 0;
    }

    @Override
    public void saveUserActionDataToDB(UserActionData userActionData) {
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

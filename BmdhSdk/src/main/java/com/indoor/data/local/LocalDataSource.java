package com.indoor.data.local;


import com.indoor.data.local.db.UserActionData;

import java.util.List;

/**
 * Created by Aaron on 2021/11/18.
 */
public interface LocalDataSource {

    /**
     * 保存token
     */
    void saveToken(String token);

    /**
     * 保存设备唯一ID
     */
    void saveUnitId(int unitId);

    /**
     * 获取Token
     */
    String getToken();

    /**
     * 保存AreaId
     */
    void saveAreaId(String areaId);

    /**
     * 获取AreaId
     */
    String getAreaId();

    /**
     * 获取ApiKey
     */
    String getApiKey();

    /**
     * 获取ShaCode
     */
    String getShaCode();

    /**
     * 获取用户当前的包名
     */
    String getPackageName();

    /**
     * 获取设备唯一ID
     * @return
     */
    int getUnitID();

    /**
     * save 用户数据
     */
    void saveUserActionDataToDB(UserActionData... userActionData);

    /**
     * delete 用户数据
     */
    void deleteUserActionDataToDB(List<UserActionData> userActionDatas);

    /**
     * get 指定数量的的用户数据
     */
    List<UserActionData>  getLimitUserActionDataToDB();
}

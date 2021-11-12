package com.indoor.data.local;


import com.indoor.data.local.db.UserActionData;

/**
 * Created by goldze on 2019/3/26.
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
     * 获取设备唯一ID
     * @return
     */
    int getUnitID();

    /**
     * save 用户数据
     */
    void saveUserActionDataToDB(UserActionData userActionData);

    /**
     * delete 用户数据
     */
    void deleteUserActionDataToDB(UserActionData ...userActionDatas);
}

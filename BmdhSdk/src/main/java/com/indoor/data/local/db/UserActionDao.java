package com.indoor.data.local.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface UserActionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public  void  insertActionData(UserActionData... userActionData);
    @Delete
    public void deleteActionData(List<UserActionData> userActionDatas);

    @Query("SELECT * FROM UserActionData limit 0,10")
    public List<UserActionData> getTopTenActionData();
}

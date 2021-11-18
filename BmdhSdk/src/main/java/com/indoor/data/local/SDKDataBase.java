package com.indoor.data.local;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.indoor.data.local.db.UserActionDao;
import com.indoor.data.local.db.UserActionData;

@Database(entities = {UserActionData.class}, version = 1, exportSchema = false)
public abstract class SDKDataBase extends RoomDatabase {
    public abstract UserActionDao getUserActionDao();
}

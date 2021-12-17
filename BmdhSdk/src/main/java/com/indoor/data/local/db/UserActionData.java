package com.indoor.data.local.db;

import androidx.annotation.Keep;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 "apiKey": "",
 "floorNum": "",
 "ipsInfo": "",
 "latitude": 0,
 "longitude": 0,
 "positionState": 0,
 "projectAreaId": "",
 "uuid": ""，
 ”machineOs“:"",
 "machineManu":""
 */
@Keep
@Entity
public class UserActionData {
    @PrimaryKey(autoGenerate = true)
    public Long recoredId;
    @ColumnInfo(name = "apiKey")
    public String apiKey;
    @ColumnInfo(name = "uuid")
    public String uuid;
    @ColumnInfo(name = "floorNum")
    public String floorNum;
    @ColumnInfo(name = "positionState")
    public int positionState;
    @ColumnInfo(name = "latitude")
    public String latitude;
    @ColumnInfo(name = "longitude")
    public String longitude;
    @ColumnInfo(name = "ipsInfo")
    public String ipsInfo;
    @ColumnInfo(name = "projectAreaId")
    public String projectAreaId;
    //机器系统版本
    @ColumnInfo(name = "machineOs")
    public String machineOs;
    //机器制造厂商
    @ColumnInfo(name = "machineModel")
    public String machineModel;



}
package com.indoor.data.local.db;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * "remoteId": 13,
 * "actFlag": "forward",
 * "actVal": "1",
 * "actTime": "2019-10-01 00:00:00",
 * "unitId": 3
 * appid,uuid,mapid,fnum,isIndoor,当前经纬度，出入口蓝牙设备的唯一码，json(坐标位置信息)
 */
@Entity
public class UserActionData {
    @PrimaryKey(autoGenerate = true)
    public Long recoredId;
    @ColumnInfo(name = "apiKey")
    public String apiKey;
    @ColumnInfo(name = "uuid")
    public String uuid;
    @ColumnInfo(name = "mapId")
    public String mapId;
    @ColumnInfo(name = "floorNum")
    public String floorNum;
    @ColumnInfo(name = "positionState")
    public int positionState;
    @ColumnInfo(name = "locationLatitude")
    public String locationLatitude;
    @ColumnInfo(name = "locationLongitude")
    public String locationLongitude;
    @ColumnInfo(name = "ipsInfo")
    public String ipsInfo;


}
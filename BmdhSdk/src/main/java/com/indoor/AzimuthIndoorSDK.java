package com.indoor;

import android.content.Context;
import android.os.Build;

import androidx.annotation.Keep;
import androidx.annotation.RequiresApi;

import com.indoor.position.IPSMeasurement;
import com.indoor.utils.KLog;
import com.indoor.utils.Utils;

@Keep
@RequiresApi(api = Build.VERSION_CODES.O)
public class AzimuthIndoorSDK {
    private static final String TAG = "AzimuthIndoorSDK";
    private static final String SDK_VERSION = "V1.0";
    private static volatile AzimuthIndoorSDK mInstance;
    private Context mContext;
    private AzimuthIndoorStrategy mAzimuthIndoorStrategy;


    private AzimuthIndoorSDK() {

    }

    public static AzimuthIndoorSDK getInstance() {
        if (mInstance == null) {
            synchronized (AzimuthIndoorSDK.class) {
                if (mInstance == null) {
                    mInstance = new AzimuthIndoorSDK();
                }
            }
        }
        return mInstance;
    }

    /**
     * 获取SDK版本号
     *
     * @return 返回SDK版本号
     */
    public String getSDKVersion() {
        return SDK_VERSION;
    }

    /**
     * 当前是否在室内
     *
     * @return 当前是否在室内
     */
    public boolean isCurrentIndoor() {
        if (mAzimuthIndoorStrategy == null) {
            KLog.e(TAG, "mAzimuthIndoorStrategy is null,please invoke init() first...");
            return false;
        }
        return mAzimuthIndoorStrategy.ismIsIndoor();
    }

    /**
     * 开启室内定位服务
     *
     * @param callback 获取到位置信息后的回调，1s回调一次
     */
    public void startIndoorLocation(IPSMeasurement.Callback callback) {
        if (mAzimuthIndoorStrategy == null) {
            KLog.e(TAG, "mAzimuthIndoorStrategy is null,please invoke init() first...");
            return;
        }
        mAzimuthIndoorStrategy.startIndoorSdkLocate(callback);
    }


    /**
     * 对当前室内外环境改变，当前室内楼层改变，已经服务断开了的监听
     *
     * @param listener 监听器
     */
    public void setIndoorOrOutdoorChangedListener(IAzimuthNaviManager.INaviIndoorStateChangeListener listener) {

    }

    /**
     * 初始化SDK环境，
     *
     *
     * @param context             环境上下文
     */
    public void init(Context context, IAzimuthNaviManager.IInitSDKListener iInitSDKListener) {
        mContext = context.getApplicationContext();
        KLog.init(true);
        Utils.init(context);
        mAzimuthIndoorStrategy = new AzimuthIndoorStrategy(mContext);
        //TODO 配置文件检测更新
        mAzimuthIndoorStrategy.verifySDK(iInitSDKListener);
    }

    /**
     * 更新区域配置文件
     */
    public void refreshAreaConfig(String areaId, IAzimuthNaviManager.IUpdateAreaConfigListener iUpdateAreaConfigListener) {
        if (mAzimuthIndoorStrategy == null) {
            KLog.e(TAG, "mAzimuthIndoorStrategy is null,please invoke init() first...");
            return;
        }
        mAzimuthIndoorStrategy.refreshAreaConfig(areaId, iUpdateAreaConfigListener);
    }

    /**
     * 更新当前AreaId对应的区域配置文件
     */
    public void refreshCurrentAreaConfig(){
        mAzimuthIndoorStrategy.refreshCurrentAreaConfig();
    }

    /**
     * 更新市配置文件
     */
    public void refreshCityConfig(String areaId, IAzimuthNaviManager.IUpdateAreaConfigListener iUpdateAreaConfigListener) {
        if (mAzimuthIndoorStrategy == null) {
            KLog.e(TAG, "mAzimuthIndoorStrategy is null,please invoke init() first...");
            return;
        }
        mAzimuthIndoorStrategy.refreshAreaConfig(areaId, iUpdateAreaConfigListener);
    }

    /**
     * 设置区域ID
     *
     * @param areaId
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void setAreaId(String areaId) {
        if (mAzimuthIndoorStrategy == null) {
            KLog.e(TAG, "mAzimuthIndoorStrategy is null,please invoke init() first...");
            return;
        }
        mAzimuthIndoorStrategy.setAreaId(areaId);
    }
    /****************************在Activit或Fragment的各个生命周期做不同的设置，比如蓝牙扫描频率************************************************/
    public void onCreate() {

    }

    public void onResume() {

    }

    public void onPause() {

    }


    public void onStop() {

    }

    public void onDestroy() {

    }


    /**
     * 销毁当前SDK资源
     */
    public void exitSDK() {
        if (mAzimuthIndoorStrategy == null) {
            KLog.e(TAG, "mAzimuthIndoorStrategy is null,please invoke init() first...");
            return;
        }
        mAzimuthIndoorStrategy.clearDataAndExit();
    }


}

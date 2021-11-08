package com.indoor;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.webkit.ValueCallback;

import androidx.annotation.RequiresApi;

import com.indoor.position.IPSMeasurement;
import com.indoor.position.IndoorPositionService;

public class BmdhIndoorSDK {
    private static final String TAG="BmdhIndoorSDK";
    private static final String SDK_VERSION="V1.0";
    private static volatile BmdhIndoorSDK mInstance;

    private BmdhIndoorConfig mBmdhIndoorConfig=null;
    private Context mContext;
    private BmdhIndoorStrategy mBmdhIndoorStrategy;


    private BmdhIndoorSDK(){

    }
    public static BmdhIndoorSDK getInstance(){
        if(mInstance ==null){
            synchronized (BmdhIndoorSDK.class){
                if(mInstance ==null){
                    mInstance =new BmdhIndoorSDK();
                }
            }
        }
        return mInstance;
    }

    /**
     * 获取SDK版本号
     * @return
     */
    public String getSDKVersion(){
        return SDK_VERSION;
    }

    /**
     * 当前是否在室内
     * @return
     */
    public boolean isCurrentIndoor(){
        if(mBmdhIndoorStrategy==null){
            Log.e(TAG,"mBmdhIndoorStrategy is null");
            return false;
        }
        return mBmdhIndoorStrategy.ismIsIndoor();
    }

    /**
     * 开启室内定位服务
     *
     * @param callback 获取到位置信息后的回调，1s回调一次
     */
    public void startIndoorLocation(IPSMeasurement.Callback callback){
        if(mBmdhIndoorStrategy==null){
            Log.e(TAG,"mBmdhIndoorStrategy is null");
            return;
        }
        mBmdhIndoorStrategy.startIndoorSdkLocate(callback);
    }


    /**
     * 当前室内外环境改变的回调
     *
     * @param listener 监听器
     */
    public void setIndoorOrOutdoorChangedListener(IBmdhNaviManager.INaviIndoorOrOutdoorChangeListener listener){

    }

    /**
     * 当前室内楼层改变的回调
     *
     * @param listener 监听器
     */
    public void setFloorChangedListener(IBmdhNaviManager.INaviFloorChangeListener listener){

    }

    /**
     * 初始化SDK环境，
     *
     * @param context 环境上下文
     * @param bmdhIndoorConfig 使用默认值则传入BmdhIndoorConfig.DEFAULT
     */
    public void init(Context context, BmdhIndoorConfig bmdhIndoorConfig) {
        mContext=context.getApplicationContext();
        mBmdhIndoorConfig=bmdhIndoorConfig;
        mBmdhIndoorStrategy=new BmdhIndoorStrategy(mContext);

    }


    /****************************在Activit或Fragment的各个生命周期做不同的设置，比如蓝牙扫描频率************************************************/
    public void onCreate(){

    }

    public void onResume(){

    }

    public void onPause(){

    }


    public void onStop(){

    }

    public void onDestroy(){

    }


    /**
     * 销毁当前SDK资源
     */
    public void exitSDK(){

    }


}

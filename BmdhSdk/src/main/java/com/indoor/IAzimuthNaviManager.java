package com.indoor;

import androidx.annotation.Keep;

import com.indoor.position.IPSMeasurement;
@Keep
public interface IAzimuthNaviManager {
    @Keep
    public interface INaviIndoorStateChangeListener{
        public abstract void onIndoorOrOutdoorChanged(IPSMeasurement.Mode mode);
        public abstract void onFloorChanged();
        public abstract void onServiceDisconnected();
    }
    @Keep
    public interface IInitSDKListener{
        void initStart();

        void initSuccess(String code);

        void initFailed(String message);
    }

    @Keep
    interface IUpdateAreaConfigListener{
        void updateSuccess();
        void updateError(Throwable e);
        void updateFailed(String msg);
    }
}

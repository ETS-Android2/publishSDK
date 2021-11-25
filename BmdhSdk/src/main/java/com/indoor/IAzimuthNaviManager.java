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
        void onAuthResult(int code, String message);

        void initStart();

        void initSuccess();

        void initFailed(int code, String message);
    }

}

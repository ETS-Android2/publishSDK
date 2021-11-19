package com.indoor;

import com.indoor.position.IPSMeasurement;

public interface IBmdhNaviManager {

    public interface INaviIndoorStateChangeListener{
        public abstract void onIndoorOrOutdoorChanged(IPSMeasurement.Mode mode);
        public abstract void onFloorChanged();
        public abstract void onServiceDisconnected();
    }

    public interface IInitSDKListener{
        void onAuthResult(int code, String message);

        void initStart();

        void initSuccess();

        void initFailed(int code, String message);
    }

}

package com.indoor;

public interface IBmdhNaviManager {

    public interface INaviIndoorStateChangeListener{
        public abstract void onIndoorOrOutdoorChanged();
        public abstract void onFloorChanged();
        public abstract void onServiceDisconnected();
    }

}

package com.indoor.position;

import lombok.Data;
import lombok.Value;



@Data
public class EnvironmentData {


    private boolean bluetoothCheckResult;
    private double[] bluetoothReferLocation;
    private int steps;

    public EnvironmentData(boolean bluetoothCheckResult, double[] bluetoothReferLocation, int steps) {
        this.bluetoothCheckResult = bluetoothCheckResult;
        this.bluetoothReferLocation = bluetoothReferLocation;
        this.steps = steps;
    };


}

package com.indoor.position;

import lombok.Builder;
import lombok.ToString;
import lombok.Value;

@Builder(toBuilder = true)
@ToString
@Value
class BluetoothData {

    String bluetoothID;
    int bluetoothRssi;
    @Builder.Default int count=0;


}
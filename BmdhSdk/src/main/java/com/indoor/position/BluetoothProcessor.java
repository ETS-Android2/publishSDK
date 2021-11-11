package com.indoor.position;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableTable;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * {@link BluetoothProcessor} deals with bluetooth scan and connections.
 */
@RequiresApi(api = Build.VERSION_CODES.O)
class BluetoothProcessor {
    private static final ScanFilter SCAN_FILTER = new ScanFilter.Builder().setManufacturerData(76,new byte[]{2, 21, 1, 18}).build();
    private static final String LOG_TAG = "BluetoothProcessor";
    private final BluetoothManager bluetoothManager;
    private final HashMap<String, BluetoothData> historicalData;
    private final AtomicBoolean inScanning;
    private final Handler handler;
    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            updateBluetoothByScanResult(result);
        }
    };

    BluetoothProcessor(BluetoothManager bluetoothManager) {
        this.bluetoothManager = bluetoothManager;
        this.inScanning = new AtomicBoolean(false);
        this.handler = new Handler();
        this.historicalData = new HashMap<>();
    }


    /**
     * Scan bluetooth devices.
     *
     * @param timeoutMillis configures the scan timeout in millisecond.
     */
    void tryScan(long timeoutMillis) {
        BluetoothAdapter adapter = bluetoothManager.getAdapter();
        if (!adapter.enable()) {
           // Log.d(LOG_TAG, "Bluetooth is not enabled.");
        }
        startScan(adapter);
        handler.postDelayed(() -> tryScan(timeoutMillis), timeoutMillis);
        //handler.postDelayed(() -> stopScan(adapter), timeoutMillis);
    }

    /*获取到扫描的蓝牙数据列表*/
    ImmutableMap<String, BluetoothData> getHisBluetoothData() {
        return ImmutableMap.copyOf(historicalData);
    }
    /*获取到扫描的蓝牙数据列表*/
    ImmutableMap<String, BluetoothData> clearHisBluetoothData() {
        historicalData.clear();
        return ImmutableMap.copyOf(historicalData);
    }
    private void startScan(BluetoothAdapter bluetoothAdapter) {
        if (inScanning.get()) {
           // Log.d(LOG_TAG, "already in scanning.");
        }
        BluetoothLeScanner scanner = bluetoothAdapter.getBluetoothLeScanner();
        this.inScanning.set(true);
        List<ScanFilter> filters = ImmutableList.of(SCAN_FILTER);
        scanner.startScan(filters,
                new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(),
                scanCallback);
    }

    private void stopScan(BluetoothAdapter bluetoothAdapter) {
        BluetoothLeScanner scanner = bluetoothAdapter.getBluetoothLeScanner();
        scanner.stopScan(scanCallback);
        this.inScanning.set(false);
    }


    private synchronized void updateBluetoothByScanResult(ScanResult result) {
        String deviceName = result.getDevice().getName();
        if (result.getRssi() <= -102) {
            return;
        }
        @NonNull BluetoothData last =
                Objects.requireNonNull(
                        historicalData.getOrDefault(
                                deviceName,
                                BluetoothData.builder().bluetoothID(deviceName).build()));
        BluetoothData.BluetoothDataBuilder current = last.toBuilder();
        current.bluetoothRssi(result.getRssi());
        current.count(last.getCount() + 1);
        this.historicalData.put(deviceName, current.build());
        //Log.d(LOG_TAG, String.format("onScanResult: %s", current));
    }
}

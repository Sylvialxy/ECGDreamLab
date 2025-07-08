package com.liuxinyu.neurosleep.core.ble

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt

interface BleCallback {
    fun onScanResult(device: BluetoothDevice, rssi: Int)
    fun onConnected(gatt: BluetoothGatt?)
    fun onDisconnected(gatt: BluetoothGatt?)
    fun onDataReceived(data: ByteArray)
}
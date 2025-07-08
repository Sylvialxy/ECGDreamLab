package com.liuxinyu.neurosleep.feature.home.viewmodel

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.liuxinyu.neurosleep.core.ble.BleCallback
import com.liuxinyu.neurosleep.core.ble.BleClient
import com.liuxinyu.neurosleep.core.ble.ByteUtil
import com.liuxinyu.neurosleep.util.TimeUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.LinkedBlockingQueue

class BluetoothViewModel(private val bleClient: BleClient) : ViewModel(), BleCallback {
    companion object {
        const val TAG = "BluetoothViewModel"
    }

    private val _devicesList = MutableLiveData<MutableList<BluetoothDevice>>(mutableListOf())
    val devicesList: LiveData<MutableList<BluetoothDevice>> = _devicesList

    private val _connectionState = MutableLiveData<String>("设备未连接")
    val connectionState: LiveData<String> = _connectionState

    private val _collectionState = MutableLiveData<String>("采集未开始")
    val collectionState: LiveData<String> = _collectionState

    private val _isConnected = MutableLiveData<Boolean>(false)
    val isConnected: LiveData<Boolean> = _isConnected

    private val _isCollecting = MutableLiveData<Boolean>(false)
    val isCollecting: LiveData<Boolean> = _isCollecting

    private val _isTransferring = MutableLiveData<Boolean>(false)
    val isTransferring: LiveData<Boolean> = _isTransferring

    // 数据流，用于将原始数据发送到处理层
    private val _ecgDataQueue = LinkedBlockingQueue<Int>()
    val ecgDataQueue: LinkedBlockingQueue<Int> = _ecgDataQueue

    private val _ecgDataReceived = MutableStateFlow<ByteArray?>(null)
    val ecgDataReceived: StateFlow<ByteArray?> = _ecgDataReceived

    init {
        // 设置回调
        bleClient.setBleCallback(this)
    }

    fun startScan() {
        if (bleClient.bluetoothAdapter?.isEnabled == true) {
            bleClient.startScan()
        } else {
            bleClient.ensureBluetoothEnabled()
        }
    }

    fun stopScan() {
        bleClient.stopBleScan()
    }

    fun connectToDevice(device: BluetoothDevice) {
        bleClient.connectToDevice(device)
    }

    fun disconnect() {
        bleClient.disconnect()
    }

    fun startCollecting() {
        if (_isConnected.value == true) {
            bleClient.sendControlCommand(
                ByteUtil.packCollectCommand(true, TimeUtil.getFormattedDateTime())
            )
            _isCollecting.value = true
            _collectionState.value = "采集已开始"
        }
    }

    fun stopCollecting() {
        if (_isConnected.value == true) {
            bleClient.sendControlCommand(
                ByteUtil.packCollectCommand(false, null)
            )
            _isCollecting.value = false
            _collectionState.value = "采集未开始"
        }
    }

    fun startTransferring() {
        if (_isConnected.value == true) {
            bleClient.sendControlCommand(ByteUtil.packTransferCommand(true))
            _isTransferring.value = true
        }
    }

    fun stopTransferring() {
        if (_isConnected.value == true) {
            bleClient.sendControlCommand(ByteUtil.packTransferCommand(false))
            _isTransferring.value = false
        }
    }

    fun clearData() {
        _ecgDataQueue.clear()
    }

    override fun onScanResult(device: BluetoothDevice, rssi: Int) {
        val currentList = _devicesList.value ?: mutableListOf()
        if (!currentList.contains(device)) {
            currentList.add(device)
            _devicesList.postValue(currentList)
        }
        _connectionState.postValue("扫描中...")
    }

    override fun onConnected(gatt: BluetoothGatt?) {
        _connectionState.postValue("设备已连接")
        _isConnected.postValue(true)
    }

    override fun onDisconnected(gatt: BluetoothGatt?) {
        _connectionState.postValue("设备未连接")
        _collectionState.postValue("采集未开始")
        _isConnected.postValue(false)
        _isCollecting.postValue(false)
        _isTransferring.postValue(false)
    }

    override fun onDataReceived(data: ByteArray) {
        _ecgDataReceived.value = data
    }

    fun addEcgDataToQueue(data: Int) {
        _ecgDataQueue.add(data)
        // 队列大小控制
        if (_ecgDataQueue.size > 5 * 200) {
            repeat(8) {
                _ecgDataQueue.poll()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        bleClient.close()
        bleClient.disconnect()
    }
    
    /**
     * 用于创建BluetoothViewModel的Factory
     */
    class Factory(private val bleClient: BleClient) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(BluetoothViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return BluetoothViewModel(bleClient) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
} 
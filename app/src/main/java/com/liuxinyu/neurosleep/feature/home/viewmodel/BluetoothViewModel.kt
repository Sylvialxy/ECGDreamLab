package com.liuxinyu.neurosleep.feature.home.viewmodel

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.os.SystemClock
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.liuxinyu.neurosleep.core.ble.BleCallback
import com.liuxinyu.neurosleep.core.ble.BleClient
import com.liuxinyu.neurosleep.core.ble.ByteUtil
import com.liuxinyu.neurosleep.util.TimeUtil
import com.liuxinyu.neurosleep.util.FormattedTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.LinkedBlockingQueue
import android.os.Handler
import android.os.Looper
import android.util.Log

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
    
    // 记录采集开始的时间 - 使用SystemClock.elapsedRealtime()用于计时器显示
    private val _collectionStartTime = MutableLiveData<Long>(0)
    val collectionStartTime: LiveData<Long> = _collectionStartTime

    // 记录采集开始的实际时间 - 用于发送给设备和状态恢复
    private val _collectionStartDateTime = MutableLiveData<FormattedTime?>(null)
    val collectionStartDateTime: LiveData<FormattedTime?> = _collectionStartDateTime

    // 数据流，用于将原始数据发送到处理层
    private val _ecgDataQueue = LinkedBlockingQueue<Int>()
    val ecgDataQueue: LinkedBlockingQueue<Int> = _ecgDataQueue

    private val _ecgDataReceived = MutableStateFlow<ByteArray?>(null)
    val ecgDataReceived: StateFlow<ByteArray?> = _ecgDataReceived

    // 设备信号强度缓存
    private val deviceRssiMap = mutableMapOf<String, Int>()
    
    init {
        // 设置回调
        bleClient.setBleCallback(this)
        
        // 检查是否有保存的设备和状态
        checkForSavedDevice()
    }
    
    // 检查保存的设备并尝试恢复连接
    private fun checkForSavedDevice() {
        if (bleClient.hasSavedDevice()) {
            // 如果有保存的设备，尝试恢复连接
            bleClient.restoreSavedConnection()
            
            // 设置保存的状态
            _isCollecting.value = bleClient.getSavedCollectionState()
            _isTransferring.value = bleClient.getSavedTransferState()
            
            // 恢复采集开始时间
            val savedStartTime = bleClient.getSavedStartTime()
            if (savedStartTime > 0) {
                _collectionStartTime.value = savedStartTime
            }
            
            // 更新UI状态
            if (_isCollecting.value == true) {
                _collectionState.value = "采集已开始"
            }
            
            _connectionState.value = "正在恢复连接..."
        }
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
        // 连接时不需要立即保存状态，等连接成功后保存
    }

    fun disconnect() {
        // 不再保存状态，直接断开连接
        bleClient.disconnect()
    }
    
    // 断开连接并保存状态（用于应用退出等情况）
    fun disconnectAndSaveState() {
        // 断开连接前保存状态
        saveDeviceState()
        bleClient.disconnect()
    }

    fun startCollecting() {
        if (_isConnected.value == true) {
            // 获取当前时间用于发送给设备
            val currentDateTime = TimeUtil.getFormattedDateTime()

            bleClient.sendControlCommand(
                ByteUtil.packCollectCommand(true, currentDateTime)
            )
            _isCollecting.value = true
            _collectionState.value = "采集已开始"

            // 记录采集开始时间 - 两个时间基准
            _collectionStartTime.value = SystemClock.elapsedRealtime() // 用于计时器
            _collectionStartDateTime.value = currentDateTime // 用于设备同步和状态恢复

            // 保存状态
            saveDeviceState()
        }
    }

    fun stopCollecting() {
        if (_isConnected.value == true) {
            bleClient.sendControlCommand(
                ByteUtil.packCollectCommand(false, null)
            )
            _isCollecting.value = false
            _collectionState.value = "采集未开始"
            
            // 重置采集时间
            _collectionStartTime.value = 0
            
            // 保存状态
            saveDeviceState()
        }
    }

    fun startTransferring() {
        if (_isConnected.value == true) {
            bleClient.sendControlCommand(ByteUtil.packTransferCommand(true))
            _isTransferring.value = true
            
            // 保存状态
            saveDeviceState()
        }
    }

    fun stopTransferring() {
        if (_isConnected.value == true) {
            bleClient.sendControlCommand(ByteUtil.packTransferCommand(false))
            _isTransferring.value = false
            
            // 保存状态
            saveDeviceState()
        }
    }
    
    // 获取当前连接的设备，公开给UI层使用
    fun getConnectedDevice(): BluetoothDevice? {
        // 优先从BleClient获取当前连接的设备
        val device = bleClient.getCurrentConnectedDevice()
        if (device != null) {
            return device
        }
        
        // 如果未连接，直接返回null
        if (_isConnected.value != true) {
            return null
        }
        
        // 从设备列表中查找可能连接的设备
        val devices = _devicesList.value
        return devices?.firstOrNull()
    }
    
    // 获取设备的信号强度
    fun getDeviceRssi(device: BluetoothDevice): Int {
        return deviceRssiMap[device.address] ?: 0
    }
    
    // 保存当前设备状态
    private fun saveDeviceState() {
        val connectedDevice = getConnectedDevice() ?: return

        // 保存设备状态、采集状态、传输状态和开始时间
        bleClient.saveDeviceState(
            connectedDevice,
            _isCollecting.value ?: false,
            _isTransferring.value ?: false,
            _collectionStartTime.value ?: 0,
            _collectionStartDateTime.value
        )
    }
    
    // 获取当前连接的设备
    private fun getConnectedDeviceInternal(): BluetoothDevice? {
        // 这里需要从bluetoothGatt中获取当前连接的设备
        // 因为无法直接访问bleClient中的bluetoothGatt私有变量
        // 我们可以通过查找设备列表中第一个已连接设备作为替代方案
        val devices = _devicesList.value
        return devices?.firstOrNull()
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
        
        // 保存设备信号强度
        deviceRssiMap[device.address] = rssi
        
        _connectionState.postValue("扫描中...")
    }

    override fun onConnected(gatt: BluetoothGatt?) {
        _connectionState.postValue("设备已连接")
        _isConnected.postValue(true)
        
        // 连接成功后，检查之前保存的状态
        val isCollecting = bleClient.getSavedCollectionState()
        val isTransferring = bleClient.getSavedTransferState()
        
        // 更新视图模型状态
        _isCollecting.postValue(isCollecting)
        _isTransferring.postValue(isTransferring)
        
        if (isCollecting) {
            _collectionState.postValue("采集已开始")
            
            // 恢复保存的开始时间
            val savedStartTime = bleClient.getSavedStartTime()
            if (savedStartTime > 0) {
                _collectionStartTime.postValue(savedStartTime)
            }
        }
        
        // 自动恢复传输状态
        if (isTransferring) {
            // 延迟一点执行，确保服务发现完成
            Handler(Looper.getMainLooper()).postDelayed({
                // 直接向设备发送命令，而不仅仅是更新状态
                bleClient.sendControlCommand(ByteUtil.packTransferCommand(true))
                _isTransferring.postValue(true)
                Log.d(TAG, "自动恢复传输状态：命令已发送")
            }, 1500) // 增加延迟确保初始化完成
        }
    }

    override fun onDisconnected(gatt: BluetoothGatt?) {
        _connectionState.postValue("设备未连接")
        // 不要重置采集状态，因为设备可能仍在采集
        // 但UI状态需要更新
        _isConnected.postValue(false)
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
        // 保存设备状态
        saveDeviceState()
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
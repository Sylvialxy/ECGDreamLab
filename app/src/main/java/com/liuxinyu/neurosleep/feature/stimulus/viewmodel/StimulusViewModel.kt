package com.liuxinyu.neurosleep.feature.stimulus.viewmodel

import android.bluetooth.BluetoothDevice
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.liuxinyu.neurosleep.feature.stimulus.ble.StimulusBleClient
import com.liuxinyu.neurosleep.feature.stimulus.callback.StimulusCallback
import com.liuxinyu.neurosleep.feature.stimulus.config.StimulusConfig
import com.liuxinyu.neurosleep.feature.stimulus.model.StimulusParams
import com.liuxinyu.neurosleep.feature.stimulus.model.StimulusResponse
import com.liuxinyu.neurosleep.feature.stimulus.util.StimulusCommandUtil

/**
 * 刺激设备ViewModel
 * 管理设备状态、参数设置和UI交互逻辑
 */
class StimulusViewModel(private val bleClient: StimulusBleClient) : ViewModel(), StimulusCallback {
    
    companion object {
        private const val TAG = StimulusConfig.LogTag.STIMULUS_VIEW_MODEL
    }
    
    // 设备连接状态
    private val _isConnected = MutableLiveData<Boolean>(false)
    val isConnected: LiveData<Boolean> = _isConnected
    
    private val _isScanning = MutableLiveData<Boolean>(false)
    val isScanning: LiveData<Boolean> = _isScanning
    
    private val _connectionMessage = MutableLiveData<String>("")
    val connectionMessage: LiveData<String> = _connectionMessage
    
    // 设备列表
    private val _devicesList = MutableLiveData<List<BluetoothDevice>>(emptyList())
    val devicesList: LiveData<List<BluetoothDevice>> = _devicesList
    
    // 当前连接的设备
    private val _currentDevice = MutableLiveData<BluetoothDevice?>()
    val currentDevice: LiveData<BluetoothDevice?> = _currentDevice
    
    // 设备状态
    private val _deviceStatus = MutableLiveData<Byte>(StimulusConfig.DeviceStatus.STANDBY)
    val deviceStatus: LiveData<Byte> = _deviceStatus
    
    // 刺激参数
    private val _stimulusParams = MutableLiveData<StimulusParams>(StimulusParams())
    val stimulusParams: LiveData<StimulusParams> = _stimulusParams
    
    // 错误信息
    private val _errorMessage = MutableLiveData<String>("")
    val errorMessage: LiveData<String> = _errorMessage
    
    // 命令执行状态
    private val _isCommandExecuting = MutableLiveData<Boolean>(false)
    val isCommandExecuting: LiveData<Boolean> = _isCommandExecuting
    
    // 设备响应信息
    private val _lastResponse = MutableLiveData<StimulusResponse?>()
    val lastResponse: LiveData<StimulusResponse?> = _lastResponse
    
    init {
        bleClient.setCallback(this)
    }
    
    /**
     * 开始扫描设备
     */
    fun startScan() {
        if (_isScanning.value == true) return
        
        _devicesList.value = emptyList()
        _errorMessage.value = ""
        bleClient.startScan()
    }
    
    /**
     * 停止扫描
     */
    fun stopScan() {
        bleClient.stopScan()
    }
    
    /**
     * 连接设备
     */
    fun connectDevice(device: BluetoothDevice) {
        _connectionMessage.value = "正在连接设备..."
        _errorMessage.value = ""
        bleClient.connectDevice(device)
    }
    
    /**
     * 断开连接
     */
    fun disconnect() {
        bleClient.disconnect()
    }
    
    /**
     * 更新刺激参数
     */
    fun updateStimulusParams(params: StimulusParams) {
        _stimulusParams.value = params
    }
    
    /**
     * 设置通道
     */
    fun setChannel(channel: Byte) {
        if (!_isConnected.value!!) {
            _errorMessage.value = "设备未连接"
            return
        }
        
        _isCommandExecuting.value = true
        val success = bleClient.sendCommand(StimulusConfig.CommandType.CHANNEL_SELECT, byteArrayOf(channel))
        if (success) {
            val params = _stimulusParams.value!!.copy()
            params.channel = channel
            _stimulusParams.value = params
        } else {
            _isCommandExecuting.value = false
        }
    }
    
    /**
     * 设置工作时间
     */
    fun setWorkTime(workTimeMinutes: Int) {
        if (!_isConnected.value!!) {
            _errorMessage.value = "设备未连接"
            return
        }

        _isCommandExecuting.value = true
        // 创建2字节的工作时间数据
        val data = ByteArray(2)
        data[0] = (workTimeMinutes shr 8).toByte() // 高字节
        data[1] = (workTimeMinutes and 0xFF).toByte() // 低字节

        val success = bleClient.sendCommand(StimulusConfig.CommandType.WORK_TIME_SET, data)
        if (success) {
            val params = _stimulusParams.value!!.copy()
            params.workTimeMinutes = workTimeMinutes
            _stimulusParams.value = params
        } else {
            _isCommandExecuting.value = false
        }
    }
    
    /**
     * 启动/停止刺激
     */
    fun controlStimulus(start: Boolean) {
        if (!_isConnected.value!!) {
            _errorMessage.value = "设备未连接"
            return
        }
        
        _isCommandExecuting.value = true
        val control = if (start) StimulusConfig.StimulusControl.START else StimulusConfig.StimulusControl.STOP
        val success = bleClient.sendCommand(StimulusConfig.CommandType.STIMULUS_CONTROL, byteArrayOf(control))
        if (success) {
            val params = _stimulusParams.value!!.copy()
            params.isRunning = start
            _stimulusParams.value = params
        } else {
            _isCommandExecuting.value = false
        }
    }
    
    /**
     * 设置刺激模式
     */
    fun setStimulusMode(mode: Byte) {
        if (!_isConnected.value!!) {
            _errorMessage.value = "设备未连接"
            return
        }
        
        _isCommandExecuting.value = true
        val success = bleClient.sendCommand(StimulusConfig.CommandType.STIMULUS_MODE_SET, byteArrayOf(mode))
        if (success) {
            val params = _stimulusParams.value!!.copy()
            params.stimulusMode = mode
            _stimulusParams.value = params
        } else {
            _isCommandExecuting.value = false
        }
    }
    
    /**
     * 设置频率
     */
    fun setFrequency(frequency: Int) {
        if (!_isConnected.value!!) {
            _errorMessage.value = "设备未连接"
            return
        }

        _isCommandExecuting.value = true
        // 创建2字节的频率数据
        val data = ByteArray(2)
        data[0] = (frequency shr 8).toByte() // 高字节
        data[1] = (frequency and 0xFF).toByte() // 低字节

        val success = bleClient.sendCommand(StimulusConfig.CommandType.FREQUENCY_SET, data)
        if (success) {
            val params = _stimulusParams.value!!.copy()
            params.frequency = frequency
            _stimulusParams.value = params
        } else {
            _isCommandExecuting.value = false
        }
    }
    
    /**
     * 设置脉宽
     */
    fun setPulseWidth(pulseWidth: Int) {
        if (!_isConnected.value!!) {
            _errorMessage.value = "设备未连接"
            return
        }

        _isCommandExecuting.value = true
        // 创建2字节的脉宽数据
        val data = ByteArray(2)
        data[0] = (pulseWidth shr 8).toByte() // 高字节
        data[1] = (pulseWidth and 0xFF).toByte() // 低字节

        val success = bleClient.sendCommand(StimulusConfig.CommandType.PULSE_WIDTH_SET, data)
        if (success) {
            val params = _stimulusParams.value!!.copy()
            params.pulseWidth = pulseWidth
            _stimulusParams.value = params
        } else {
            _isCommandExecuting.value = false
        }
    }
    
    /**
     * 设置电流强度
     */
    fun setIntensity(intensity: Float) {
        if (!_isConnected.value!!) {
            _errorMessage.value = "设备未连接"
            return
        }

        _isCommandExecuting.value = true
        // 创建2字节的电流强度数据（数据*10发送）
        val currentValue = (intensity * 10).toInt()
        val data = ByteArray(2)
        data[0] = (currentValue shr 8).toByte() // 高字节
        data[1] = (currentValue and 0xFF).toByte() // 低字节

        val success = bleClient.sendCommand(StimulusConfig.CommandType.CURRENT_SET, data)
        if (success) {
            val params = _stimulusParams.value!!.copy()
            params.intensity = intensity
            _stimulusParams.value = params
        } else {
            _isCommandExecuting.value = false
        }
    }
    
    /**
     * 设置上升时间
     */
    fun setRiseTime(riseTime: Float) {
        if (!_isConnected.value!!) {
            _errorMessage.value = "设备未连接"
            return
        }

        _isCommandExecuting.value = true
        // 创建2字节的上升时间数据（数据*10发送）
        val timeValue = (riseTime * 10).toInt()
        val data = ByteArray(2)
        data[0] = (timeValue shr 8).toByte() // 高字节
        data[1] = (timeValue and 0xFF).toByte() // 低字节

        val success = bleClient.sendCommand(StimulusConfig.CommandType.RISE_TIME_SET, data)
        if (success) {
            val params = _stimulusParams.value!!.copy()
            params.riseTime = riseTime
            _stimulusParams.value = params
        } else {
            _isCommandExecuting.value = false
        }
    }
    
    /**
     * 设置保持时间
     */
    fun setHoldTime(holdTime: Float) {
        if (!_isConnected.value!!) {
            _errorMessage.value = "设备未连接"
            return
        }

        _isCommandExecuting.value = true
        // 创建2字节的保持时间数据（数据*10发送）
        val timeValue = (holdTime * 10).toInt()
        val data = ByteArray(2)
        data[0] = (timeValue shr 8).toByte() // 高字节
        data[1] = (timeValue and 0xFF).toByte() // 低字节

        val success = bleClient.sendCommand(StimulusConfig.CommandType.HOLD_TIME_SET, data)
        if (success) {
            val params = _stimulusParams.value!!.copy()
            params.holdTime = holdTime
            _stimulusParams.value = params
        } else {
            _isCommandExecuting.value = false
        }
    }
    
    /**
     * 设置下降时间
     */
    fun setFallTime(fallTime: Float) {
        if (!_isConnected.value!!) {
            _errorMessage.value = "设备未连接"
            return
        }

        _isCommandExecuting.value = true
        // 创建2字节的下降时间数据（数据*10发送）
        val timeValue = (fallTime * 10).toInt()
        val data = ByteArray(2)
        data[0] = (timeValue shr 8).toByte() // 高字节
        data[1] = (timeValue and 0xFF).toByte() // 低字节

        val success = bleClient.sendCommand(StimulusConfig.CommandType.FALL_TIME_SET, data)
        if (success) {
            val params = _stimulusParams.value!!.copy()
            params.fallTime = fallTime
            _stimulusParams.value = params
        } else {
            _isCommandExecuting.value = false
        }
    }
    
    /**
     * 设置停止时间
     */
    fun setStopTime(stopTime: Float) {
        if (!_isConnected.value!!) {
            _errorMessage.value = "设备未连接"
            return
        }

        _isCommandExecuting.value = true
        // 创建2字节的停止时间数据（数据*10发送）
        val timeValue = (stopTime * 10).toInt()
        val data = ByteArray(2)
        data[0] = (timeValue shr 8).toByte() // 高字节
        data[1] = (timeValue and 0xFF).toByte() // 低字节

        val success = bleClient.sendCommand(StimulusConfig.CommandType.STOP_TIME_SET, data)
        if (success) {
            val params = _stimulusParams.value!!.copy()
            params.stopTime = stopTime
            _stimulusParams.value = params
        } else {
            _isCommandExecuting.value = false
        }
    }
    
    // StimulusCallback 实现
    override fun onDeviceFound(device: BluetoothDevice, rssi: Int) {
        val currentList = _devicesList.value?.toMutableList() ?: mutableListOf()
        if (!currentList.any { it.address == device.address }) {
            currentList.add(device)
            _devicesList.value = currentList
        }
    }
    
    override fun onScanStarted() {
        _isScanning.value = true
        _connectionMessage.value = "正在扫描设备..."
    }
    
    override fun onScanStopped() {
        _isScanning.value = false
        _connectionMessage.value = if (_devicesList.value?.isEmpty() == true) "未找到设备" else "扫描完成"
    }
    
    override fun onDeviceConnected(device: BluetoothDevice) {
        _isConnected.value = true
        _currentDevice.value = device
        _connectionMessage.value = "设备已连接"
        _errorMessage.value = ""
        Log.d(TAG, "Device connected: ${device.name}")
    }
    
    override fun onDeviceConnectFailed(device: BluetoothDevice, errorCode: Int, errorMessage: String) {
        _isConnected.value = false
        _currentDevice.value = null
        _connectionMessage.value = "连接失败"
        _errorMessage.value = errorMessage
        Log.e(TAG, "Device connect failed: $errorMessage")
    }
    
    override fun onDeviceDisconnected(device: BluetoothDevice) {
        _isConnected.value = false
        _currentDevice.value = null
        _connectionMessage.value = "设备已断开"
        Log.d(TAG, "Device disconnected: ${device.name}")
    }
    
    override fun onConnectionStateChanged(isConnected: Boolean) {
        _isConnected.value = isConnected
    }
    
    override fun onResponseReceived(response: StimulusResponse) {
        _lastResponse.value = response
        _isCommandExecuting.value = false
        Log.d(TAG, "Response received: $response")
    }
    
    override fun onCommandSent(commandType: Byte, data: ByteArray) {
        Log.d(TAG, "Command sent: 0x${String.format("%02X", commandType)}")
    }
    
    override fun onCommandSendFailed(commandType: Byte, data: ByteArray, errorCode: Int, errorMessage: String) {
        _isCommandExecuting.value = false
        _errorMessage.value = "命令发送失败: $errorMessage"
        Log.e(TAG, "Command send failed: $errorMessage")
    }
    
    override fun onCommandTimeout(commandType: Byte) {
        _isCommandExecuting.value = false
        _errorMessage.value = "命令超时"
        Log.w(TAG, "Command timeout: 0x${String.format("%02X", commandType)}")
    }
    
    override fun onDeviceStatusChanged(deviceStatus: Byte) {
        _deviceStatus.value = deviceStatus
        Log.d(TAG, "Device status changed: 0x${String.format("%02X", deviceStatus)}")
    }
    
    override fun onChannelChanged(channel: Byte) {
        val params = _stimulusParams.value!!.copy()
        params.channel = channel
        _stimulusParams.value = params
        Log.d(TAG, "Channel changed: 0x${String.format("%02X", channel)}")
    }
    
    override fun onError(errorCode: Int, errorMessage: String) {
        _errorMessage.value = errorMessage
        Log.e(TAG, "Error: $errorMessage (code: $errorCode)")
    }
    
    override fun onCleared() {
        super.onCleared()
        bleClient.disconnect()
    }
    
    /**
     * ViewModel工厂类
     */
    class Factory(private val bleClient: StimulusBleClient) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(StimulusViewModel::class.java)) {
                return StimulusViewModel(bleClient) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

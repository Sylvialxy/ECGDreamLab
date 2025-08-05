package com.liuxinyu.neurosleep.feature.stimulus.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.liuxinyu.neurosleep.feature.stimulus.callback.StimulusCallback
import com.liuxinyu.neurosleep.feature.stimulus.config.StimulusConfig
import com.liuxinyu.neurosleep.feature.stimulus.model.StimulusResponse
import com.liuxinyu.neurosleep.feature.stimulus.util.StimulusCommandUtil
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * 刺激设备蓝牙客户端
 * 实现串口蓝牙透传通信，包括设备扫描、连接、命令发送和响应接收
 */
@SuppressLint("MissingPermission")
class StimulusBleClient(private val context: Context) {
    
    companion object {
        private const val TAG = StimulusConfig.LogTag.STIMULUS_BLE_CLIENT
    }
    
    // 蓝牙相关组件
    private val bluetoothManager: BluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var bluetoothGatt: BluetoothGatt? = null
    
    // 特征值
    private var dataCharacteristic: BluetoothGattCharacteristic? = null
    
    // 回调接口
    private var stimulusCallback: StimulusCallback? = null
    
    // 状态管理
    private var isScanning = false
    private var isConnected = false
    private var currentDevice: BluetoothDevice? = null
    private var reconnectAttempts = 0
    
    // 命令管理
    private val pendingCommands = ConcurrentHashMap<Byte, Long>()
    private val mainHandler = Handler(Looper.getMainLooper())
    
    // 数据缓冲区（用于处理分包数据）
    private val dataBuffer = mutableListOf<Byte>()
    
    /**
     * 设置回调接口
     */
    fun setCallback(callback: StimulusCallback) {
        this.stimulusCallback = callback
    }
    
    /**
     * 检查蓝牙是否可用
     */
    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }
    
    /**
     * 开始扫描设备
     */
    fun startScan() {
        if (!isBluetoothEnabled()) {
            stimulusCallback?.onError(StimulusConfig.ErrorCode.BLUETOOTH_NOT_ENABLED, "蓝牙未启用")
            return
        }
        
        if (isScanning) {
            Log.w(TAG, "Already scanning")
            return
        }
        
        bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
        if (bluetoothLeScanner == null) {
            stimulusCallback?.onError(StimulusConfig.ErrorCode.BLUETOOTH_NOT_ENABLED, "无法获取蓝牙扫描器")
            return
        }
        
        // 设置扫描过滤器 - 不使用过滤器，扫描所有设备
        val scanFilters = emptyList<ScanFilter>()
        
        // 设置扫描设置 - 使用低延迟模式快速发现设备
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setReportDelay(0) // 立即报告发现的设备
            .build()
        
        try {
            bluetoothLeScanner?.startScan(scanFilters, scanSettings, scanCallback)
            isScanning = true
            stimulusCallback?.onScanStarted()
            
            // 设置扫描超时
            mainHandler.postDelayed({
                stopScan()
            }, StimulusConfig.Protocol.SCAN_TIMEOUT)
            
            Log.d(TAG, "Started scanning for stimulus devices")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start scan", e)
            stimulusCallback?.onError(StimulusConfig.ErrorCode.BLUETOOTH_NOT_ENABLED, "扫描启动失败: ${e.message}")
        }
    }
    
    /**
     * 停止扫描
     */
    fun stopScan() {
        if (!isScanning) return
        
        try {
            bluetoothLeScanner?.stopScan(scanCallback)
            isScanning = false
            stimulusCallback?.onScanStopped()
            Log.d(TAG, "Stopped scanning")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop scan", e)
        }
    }
    
    /**
     * 连接设备
     */
    fun connectDevice(device: BluetoothDevice) {
        // 如果已经连接到同一设备，直接返回
        if (isConnected && currentDevice?.address == device.address) {
            Log.d(TAG, "Already connected to this device")
            return
        }

        // 如果连接到其他设备，先断开
        if (isConnected || bluetoothGatt != null) {
            Log.d(TAG, "Disconnecting from previous device")
            disconnect()
            // 等待一下确保断开完成
            Thread.sleep(500)
        }

        currentDevice = device
        reconnectAttempts = 0

        Log.d(TAG, "Connecting to device: ${device.name} (${device.address})")

        try {
            bluetoothGatt = device.connectGatt(context, false, gattCallback)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to device", e)
            stimulusCallback?.onDeviceConnectFailed(device, StimulusConfig.ErrorCode.CONNECTION_FAILED, "连接失败: ${e.message}")
        }
    }
    
    /**
     * 断开连接
     */
    fun disconnect() {
        try {
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
            bluetoothGatt = null
            dataCharacteristic = null
            isConnected = false
            currentDevice = null
            reconnectAttempts = 0
            pendingCommands.clear()
            dataBuffer.clear()
            
            stimulusCallback?.onConnectionStateChanged(false)
            Log.d(TAG, "Disconnected from device")
        } catch (e: Exception) {
            Log.e(TAG, "Error during disconnect", e)
        }
    }
    
    /**
     * 发送命令
     */
    fun sendCommand(commandType: Byte, data: ByteArray): Boolean {
        if (!isConnected || dataCharacteristic == null) {
            stimulusCallback?.onCommandSendFailed(commandType, data, StimulusConfig.ErrorCode.DEVICE_NOT_CONNECTED, "设备未连接")
            return false
        }
        
        try {
            val command = StimulusCommandUtil.createCommand(commandType, data)
            dataCharacteristic?.value = command
            
            val success = bluetoothGatt?.writeCharacteristic(dataCharacteristic) == true
            
            if (success) {
                // 记录待响应命令
                pendingCommands[commandType] = System.currentTimeMillis()
                
                // 设置命令超时
                mainHandler.postDelayed({
                    if (pendingCommands.containsKey(commandType)) {
                        pendingCommands.remove(commandType)
                        stimulusCallback?.onCommandTimeout(commandType)
                    }
                }, StimulusConfig.Protocol.COMMAND_TIMEOUT)
                
                stimulusCallback?.onCommandSent(commandType, data)
                Log.d(TAG, "Sent command: ${StimulusCommandUtil.bytesToHexString(command)}")
            } else {
                stimulusCallback?.onCommandSendFailed(commandType, data, StimulusConfig.ErrorCode.CONNECTION_FAILED, "命令发送失败")
            }
            
            return success
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send command", e)
            stimulusCallback?.onCommandSendFailed(commandType, data, StimulusConfig.ErrorCode.CONNECTION_FAILED, "命令发送异常: ${e.message}")
            return false
        }
    }
    
    /**
     * 获取当前连接的设备
     */
    fun getCurrentDevice(): BluetoothDevice? = currentDevice
    
    /**
     * 获取连接状态
     */
    fun isConnected(): Boolean = isConnected
    
    /**
     * 扫描回调
     */
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val rssi = result.rssi

            // 获取设备信息
            val deviceName = device.name
            val deviceAddress = device.address

            // 过滤掉未知设备（没有设备名称的设备）
            if (deviceName.isNullOrBlank()) {
                Log.d(TAG, "Filtered out unnamed device: $deviceAddress RSSI: $rssi")
                return
            }

            // 获取广播数据中的服务UUID信息
            val scanRecord = result.scanRecord
            val serviceUuids = scanRecord?.serviceUuids

            Log.d(TAG, "Found device: $deviceName ($deviceAddress) RSSI: $rssi")
            if (serviceUuids != null && serviceUuids.isNotEmpty()) {
                Log.d(TAG, "  Advertised services: ${serviceUuids.joinToString(", ")}")
            }

            // 只显示有名称的设备
            stimulusCallback?.onDeviceFound(device, rssi)
        }
        
        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "Scan failed with error code: $errorCode")
            isScanning = false
            stimulusCallback?.onError(StimulusConfig.ErrorCode.BLUETOOTH_NOT_ENABLED, "扫描失败，错误码: $errorCode")
        }
    }
    
    /**
     * GATT回调
     */
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d(TAG, "Connected to GATT server")
                    isConnected = true
                    reconnectAttempts = 0
                    
                    // 发现服务
                    mainHandler.post {
                        gatt.discoverServices()
                    }
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d(TAG, "Disconnected from GATT server")
                    val wasConnected = isConnected
                    isConnected = false
                    dataCharacteristic = null
                    
                    mainHandler.post {
                        if (wasConnected) {
                            currentDevice?.let { device ->
                                stimulusCallback?.onDeviceDisconnected(device)
                            }
                        }
                        stimulusCallback?.onConnectionStateChanged(false)
                        
                        // 尝试重连
                        if (wasConnected && reconnectAttempts < StimulusConfig.Bluetooth.MAX_RECONNECT_ATTEMPTS) {
                            attemptReconnect()
                        }
                    }
                }
            }
        }
        
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Services discovered")

                // 列出所有可用的服务
                val services = gatt.services
                Log.d(TAG, "Available services:")
                for (service in services) {
                    Log.d(TAG, "Service UUID: ${service.uuid}")
                    for (characteristic in service.characteristics) {
                        Log.d(TAG, "  Characteristic UUID: ${characteristic.uuid}, Properties: ${characteristic.properties}")
                    }
                }

                // 尝试查找可用的串口服务和特征值
                var foundService: BluetoothGattService? = null
                var foundCharacteristic: BluetoothGattCharacteristic? = null

                // 首先尝试已知的串口服务UUID
                for (serviceUuid in StimulusConfig.Bluetooth.SERIAL_SERVICE_UUIDS) {
                    val service = gatt.getService(UUID.fromString(serviceUuid))
                    if (service != null) {
                        Log.d(TAG, "Found known serial service: $serviceUuid")
                        foundService = service

                        // 在该服务中查找可用的特征值，优先选择具有写入属性的
                        for (charUuid in StimulusConfig.Bluetooth.SERIAL_CHARACTERISTIC_UUIDS) {
                            val characteristic = service.getCharacteristic(UUID.fromString(charUuid))
                            if (characteristic != null) {
                                val hasWrite = (characteristic.properties and (BluetoothGattCharacteristic.PROPERTY_WRITE or
                                               BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) != 0
                                val hasNotify = (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0

                                Log.d(TAG, "Checking characteristic: $charUuid, hasWrite: $hasWrite, hasNotify: $hasNotify, properties: ${characteristic.properties}")

                                // 优先选择既有写入又有通知的特征值
                                if (hasWrite && hasNotify) {
                                    Log.d(TAG, "Found ideal characteristic (write+notify): $charUuid")
                                    foundCharacteristic = characteristic
                                    break
                                }
                                // 如果没有找到理想的，选择有写入属性的
                                else if (hasWrite && foundCharacteristic == null) {
                                    Log.d(TAG, "Found write characteristic: $charUuid")
                                    foundCharacteristic = characteristic
                                }
                            }
                        }
                        if (foundCharacteristic != null) break
                    }
                }

                // 如果没有找到已知服务，尝试自动发现
                if (foundService == null || foundCharacteristic == null) {
                    Log.d(TAG, "Known services not found, trying auto-discovery")
                    var bestCharacteristic: BluetoothGattCharacteristic? = null
                    var bestService: BluetoothGattService? = null

                    for (service in services) {
                        for (characteristic in service.characteristics) {
                            val hasWrite = (characteristic.properties and (BluetoothGattCharacteristic.PROPERTY_WRITE or
                                           BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) != 0
                            val hasNotify = (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0

                            Log.d(TAG, "Auto-checking characteristic: ${characteristic.uuid}, hasWrite: $hasWrite, hasNotify: $hasNotify")

                            // 优先选择既有写入又有通知的特征值
                            if (hasWrite && hasNotify) {
                                Log.d(TAG, "Auto-discovered ideal characteristic: ${characteristic.uuid}")
                                foundService = service
                                foundCharacteristic = characteristic
                                break
                            }
                            // 如果没有找到理想的，记录有写入属性的作为备选
                            else if (hasWrite && bestCharacteristic == null) {
                                Log.d(TAG, "Auto-discovered write characteristic: ${characteristic.uuid}")
                                bestCharacteristic = characteristic
                                bestService = service
                            }
                        }
                        if (foundCharacteristic != null) break
                    }

                    // 如果没有找到理想的特征值，使用备选的
                    if (foundCharacteristic == null && bestCharacteristic != null) {
                        foundCharacteristic = bestCharacteristic
                        foundService = bestService
                        Log.d(TAG, "Using fallback characteristic: ${bestCharacteristic.uuid}")
                    }
                }

                if (foundCharacteristic != null) {
                    dataCharacteristic = foundCharacteristic

                    // 检查是否有通知属性
                    val hasNotify = (foundCharacteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0

                    if (hasNotify) {
                        // 启用通知
                        gatt.setCharacteristicNotification(dataCharacteristic, true)

                        // 设置描述符以启用通知
                        val descriptor = dataCharacteristic?.getDescriptor(UUID.fromString(StimulusConfig.Bluetooth.CLIENT_CHARACTERISTIC_CONFIG))
                        if (descriptor != null) {
                            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            gatt.writeDescriptor(descriptor)
                        }
                        Log.d(TAG, "Notifications enabled for characteristic: ${foundCharacteristic.uuid}")
                    } else {
                        // 如果写入特征值没有通知属性，查找一个有通知属性的特征值
                        Log.d(TAG, "Write characteristic has no notify, looking for notify characteristic")
                        for (characteristic in foundService?.characteristics ?: emptyList()) {
                            if ((characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                                gatt.setCharacteristicNotification(characteristic, true)
                                val descriptor = characteristic.getDescriptor(UUID.fromString(StimulusConfig.Bluetooth.CLIENT_CHARACTERISTIC_CONFIG))
                                if (descriptor != null) {
                                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                                    gatt.writeDescriptor(descriptor)
                                }
                                Log.d(TAG, "Notifications enabled for separate characteristic: ${characteristic.uuid}")
                                break
                            }
                        }
                    }

                    mainHandler.post {
                        currentDevice?.let { device ->
                            stimulusCallback?.onDeviceConnected(device)
                        }
                        stimulusCallback?.onConnectionStateChanged(true)
                    }

                    Log.d(TAG, "Data characteristic configured: ${foundCharacteristic.uuid}")
                } else {
                    Log.e(TAG, "No suitable characteristic found")
                    mainHandler.post {
                        currentDevice?.let { device ->
                            stimulusCallback?.onDeviceConnectFailed(device, StimulusConfig.ErrorCode.CONNECTION_FAILED, "未找到合适的数据特征值")
                        }
                    }
                }
            } else {
                Log.e(TAG, "Service discovery failed with status: $status")
                mainHandler.post {
                    currentDevice?.let { device ->
                        stimulusCallback?.onDeviceConnectFailed(device, StimulusConfig.ErrorCode.CONNECTION_FAILED, "服务发现失败")
                    }
                }
            }
        }
        
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            val data = characteristic.value
            if (data != null && data.isNotEmpty()) {
                handleReceivedData(data)
            }
        }
        
        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "Characteristic write failed with status: $status")
            }
        }
    }
    
    /**
     * 处理接收到的数据
     */
    private fun handleReceivedData(data: ByteArray) {
        Log.d(TAG, "Received data: ${StimulusCommandUtil.bytesToHexString(data)}")
        
        // 将数据添加到缓冲区
        dataBuffer.addAll(data.toList())
        
        // 尝试解析完整的响应帧
        parseResponseFrames()
    }
    
    /**
     * 解析响应帧
     */
    private fun parseResponseFrames() {
        while (dataBuffer.size >= StimulusConfig.Protocol.MIN_FRAME_LENGTH) {
            // 查找帧头
            val headerIndex = findFrameHeader()
            if (headerIndex == -1) {
                // 没有找到帧头，清空缓冲区
                dataBuffer.clear()
                break
            }
            
            // 移除帧头之前的无效数据
            if (headerIndex > 0) {
                repeat(headerIndex) { dataBuffer.removeAt(0) }
            }
            
            // 检查是否有足够的数据来解析帧长度
            if (dataBuffer.size < 3) break
            
            val dataLength = dataBuffer[2].toInt() and 0xFF
            val totalFrameLength = 5 + dataLength // 帧头(2) + 长度(1) + 命令(1) + 数据 + 校验(1)
            
            // 检查是否有完整的帧
            if (dataBuffer.size < totalFrameLength) break
            
            // 提取完整帧
            val frameData = dataBuffer.take(totalFrameLength).toByteArray()
            repeat(totalFrameLength) { dataBuffer.removeAt(0) }
            
            // 解析响应
            val response = StimulusCommandUtil.parseResponse(frameData)
            if (response != null) {
                // 移除对应的待响应命令
                pendingCommands.remove(response.commandType)
                
                mainHandler.post {
                    stimulusCallback?.onResponseReceived(response)
                    
                    // 处理特定类型的响应
                    when (response.commandType) {
                        StimulusConfig.CommandType.DEVICE_STATUS_REPORT -> {
                            response.getDeviceStatus()?.let { status ->
                                stimulusCallback?.onDeviceStatusChanged(status)
                            }
                        }
                        StimulusConfig.CommandType.CHANNEL_STATUS_REPORT -> {
                            response.getCurrentChannel()?.let { channel ->
                                stimulusCallback?.onChannelChanged(channel)
                            }
                        }
                    }
                }
            }
        }
    }
    
    /**
     * 查找帧头位置
     */
    private fun findFrameHeader(): Int {
        for (i in 0 until dataBuffer.size - 1) {
            if (dataBuffer[i] == StimulusConfig.Protocol.FRAME_HEADER_1 && 
                dataBuffer[i + 1] == StimulusConfig.Protocol.FRAME_HEADER_2) {
                return i
            }
        }
        return -1
    }
    
    /**
     * 尝试重连
     */
    private fun attemptReconnect() {
        if (reconnectAttempts >= StimulusConfig.Bluetooth.MAX_RECONNECT_ATTEMPTS) {
            Log.w(TAG, "Max reconnect attempts reached")
            return
        }
        
        reconnectAttempts++
        Log.d(TAG, "Attempting reconnect #$reconnectAttempts")
        
        mainHandler.postDelayed({
            currentDevice?.let { device ->
                connectDevice(device)
            }
        }, StimulusConfig.Bluetooth.RECONNECT_INTERVAL)
    }
}

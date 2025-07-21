package com.liuxinyu.neurosleep.core.ble

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.liuxinyu.neurosleep.data.model.EcgConfig
import com.liuxinyu.neurosleep.feature.home.BluetoothDataActivity
import com.liuxinyu.neurosleep.util.FormattedTime
import java.util.UUID
import java.util.Timer
import java.util.TimerTask
import android.os.Handler
import android.os.Looper

@SuppressLint("MissingPermission")
class BleClient(private val context: Context) {
    private val tag : String? = this::class.simpleName
    // 这块后面可能会替换（传入一个主控制器作为观察者）
    private val dataActivity: BluetoothDataActivity = context as BluetoothDataActivity
    // 低功耗蓝牙 API 的基础组件
    var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private var scanCallback: ScanCallback? = null

    private lateinit var mGatt : BluetoothGatt
    private lateinit var firService : BluetoothGattService
    private lateinit var secService : BluetoothGattService
    private lateinit var ctrlCharacteristic : BluetoothGattCharacteristic
    private lateinit var infoCharacteristic : BluetoothGattCharacteristic
    private lateinit var dataCharacteristic : BluetoothGattCharacteristic

    private var scannedDevices = mutableSetOf<BluetoothDevice>()
    private var bleCallback: BleCallback? = null
    private var isScanning = false
    private val SCAN_DURATION: Long = 10 * 1000L
    private val MAX_SCANNED_DEVICES = 50  // 限制最大扫描设备数量
    
    // 记录设备状态的常量
    companion object {
        private const val PREFS_NAME = "BleClientPrefs"
        private const val KEY_DEVICE_ADDRESS = "device_address"
        private const val KEY_DEVICE_NAME = "device_name"
        private const val KEY_IS_COLLECTING = "is_collecting"
        private const val KEY_IS_TRANSFERRING = "is_transferring"
        private const val KEY_START_TIME = "start_time"
        private const val KEY_START_DATETIME_YEAR = "start_datetime_year"
        private const val KEY_START_DATETIME_MONTH = "start_datetime_month"
        private const val KEY_START_DATETIME_DAY = "start_datetime_day"
        private const val KEY_START_DATETIME_HOUR = "start_datetime_hour"
        private const val KEY_START_DATETIME_MINUTE = "start_datetime_minute"
        private const val KEY_START_DATETIME_SECOND = "start_datetime_second"
    }

    init {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
    }

    private val enableBtLauncher = dataActivity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // User enabled Bluetooth
            Toast.makeText(context,"蓝牙已启用", Toast.LENGTH_LONG).show()
            startScan()
        } else {
            // User did not enable Bluetooth
            Toast.makeText(context,"蓝牙未启用", Toast.LENGTH_LONG).show()
        }
    }

    fun ensureBluetoothEnabled() {
        // Device does not support Bluetooth
        if (bluetoothAdapter == null)
            return
        if (!bluetoothAdapter!!.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBtLauncher.launch(enableBtIntent)
        }
    }

    fun setBleCallback(bleCallback: BleCallback) {
        this.bleCallback = bleCallback
    }

    // 保存设备连接状态
    fun saveDeviceState(device: BluetoothDevice, isCollecting: Boolean, isTransferring: Boolean, startTime: Long, startDateTime: FormattedTime? = null) {
        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPref.edit().apply {
            putString(KEY_DEVICE_ADDRESS, device.address)
            putString(KEY_DEVICE_NAME, device.name)
            putBoolean(KEY_IS_COLLECTING, isCollecting)
            putBoolean(KEY_IS_TRANSFERRING, isTransferring)
            putLong(KEY_START_TIME, startTime)

            // 保存FormattedTime信息
            if (startDateTime != null) {
                putInt(KEY_START_DATETIME_YEAR, startDateTime.year)
                putInt(KEY_START_DATETIME_MONTH, startDateTime.month)
                putInt(KEY_START_DATETIME_DAY, startDateTime.day)
                putInt(KEY_START_DATETIME_HOUR, startDateTime.hour)
                putInt(KEY_START_DATETIME_MINUTE, startDateTime.minute)
                putInt(KEY_START_DATETIME_SECOND, startDateTime.second)
            } else {
                // 清除之前保存的时间信息
                remove(KEY_START_DATETIME_YEAR)
                remove(KEY_START_DATETIME_MONTH)
                remove(KEY_START_DATETIME_DAY)
                remove(KEY_START_DATETIME_HOUR)
                remove(KEY_START_DATETIME_MINUTE)
                remove(KEY_START_DATETIME_SECOND)
            }
            apply()
        }
    }

    // 获取当前连接的设备
    @SuppressLint("MissingPermission")
    fun getCurrentConnectedDevice(): BluetoothDevice? {
        if (bluetoothGatt != null) {
            return bluetoothGatt?.device
        }
        return null
    }
    
    // 检查是否有保存的设备
    fun hasSavedDevice(): Boolean {
        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPref.getString(KEY_DEVICE_ADDRESS, null) != null
    }

    // 恢复已保存的设备连接
    fun restoreSavedConnection(): Boolean {
        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val deviceAddress = sharedPref.getString(KEY_DEVICE_ADDRESS, null) ?: return false
        
        try {
            val device = bluetoothAdapter?.getRemoteDevice(deviceAddress)
            if (device != null) {
                Log.d(tag, "尝试恢复连接到设备: ${device.name ?: "未知设备"}")
                connectToDevice(device)
                return true
            }
        } catch (e: Exception) {
            Log.e(tag, "恢复连接失败: ${e.message}")
        }
        return false
    }
    
    // 直接恢复传输状态，不依赖回调
    fun forceRestoreTransferState() {
        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isTransferring = sharedPref.getBoolean(KEY_IS_TRANSFERRING, false)
        
        if (isTransferring && ::ctrlCharacteristic.isInitialized) {
            // 直接发送传输命令
            val command = ByteUtil.packTransferCommand(true)
            sendControlCommand(command)
            Log.d(tag, "强制恢复传输状态命令已发送")
        }
    }
    
    // 检查传输状态是否应该恢复
    fun shouldRestoreTransfer(): Boolean {
        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPref.getBoolean(KEY_IS_TRANSFERRING, false)
    }

    // 获取保存的采集状态
    fun getSavedCollectionState(): Boolean {
        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPref.getBoolean(KEY_IS_COLLECTING, false)
    }

    // 获取保存的传输状态
    fun getSavedTransferState(): Boolean {
        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPref.getBoolean(KEY_IS_TRANSFERRING, false)
    }

    // 获取保存的开始时间
    fun getSavedStartTime(): Long {
        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPref.getLong(KEY_START_TIME, 0)
    }

    // 获取保存的开始时间FormattedTime
    fun getSavedStartDateTime(): FormattedTime? {
        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // 检查是否存在保存的时间信息
        if (!sharedPref.contains(KEY_START_DATETIME_YEAR)) {
            return null
        }

        return FormattedTime(
            year = sharedPref.getInt(KEY_START_DATETIME_YEAR, 0),
            month = sharedPref.getInt(KEY_START_DATETIME_MONTH, 0),
            day = sharedPref.getInt(KEY_START_DATETIME_DAY, 0),
            hour = sharedPref.getInt(KEY_START_DATETIME_HOUR, 0),
            minute = sharedPref.getInt(KEY_START_DATETIME_MINUTE, 0),
            second = sharedPref.getInt(KEY_START_DATETIME_SECOND, 0)
        )
    }

    // 清除保存的设备状态
    fun clearSavedDeviceState() {
        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPref.edit().clear().apply()
        
        // 也清除设备SN码
        val blePref = context.getSharedPreferences("BLE_PREFS", Context.MODE_PRIVATE)
        blePref.edit().remove("SN_CODE").apply()
        
        Log.d(tag, "已清除所有保存的蓝牙设备状态")
    }

    fun startScan() {
        if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled || isScanning)
            return
        Log.i(tag, "startScan: ")

        // 清空之前的扫描结果
        scannedDevices.clear()
        isScanning = true

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .build()

        val filters = listOf(
            ScanFilter.Builder()
                .build(),
            ScanFilter.Builder()
                .build()
        )

        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                super.onScanResult(callbackType, result)
                
                // 检查设备是否为空或已扫描过
                if (result.device == null || scannedDevices.contains(result.device)) {
                    return
                }

                // 限制扫描设备数量
                if (scannedDevices.size >= MAX_SCANNED_DEVICES) {
                    stopBleScan()
                    return
                }

                try {
                    val name = result.device.name
                    if (!name.isNullOrEmpty() && name.startsWith(EcgConfig.DEVICE_NAME_PREFIX)) {
                        Log.i(tag, "onScanResult: found a device: $name")
                        
                        // 保存设备名到 SharedPreferences
                        val sharedPref = context.getSharedPreferences("BLE_PREFS", Context.MODE_PRIVATE)
                        sharedPref.edit().putString("SN_CODE", name).apply()

                        // 在主线程中回调
                        (context as? BluetoothDataActivity)?.runOnUiThread {
                            bleCallback?.onScanResult(result.device, result.rssi)
                        }
                        
                        scannedDevices.add(result.device)
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Error processing scan result: ${e.message}")
                }
            }

            override fun onScanFailed(errorCode: Int) {
                super.onScanFailed(errorCode)
                Log.e(tag, "Scan failed with error: $errorCode")
                isScanning = false
                stopBleScan()
            }
        }

        try {
            bluetoothAdapter?.bluetoothLeScanner?.startScan(filters, settings, scanCallback!!)
            
            // 使用 Timer 安排一个一次性任务，在指定时间后停止扫描
            Timer().schedule(object : TimerTask() {
                override fun run() {
                    stopBleScan()
                }
            }, SCAN_DURATION)
        } catch (e: Exception) {
            Log.e(tag, "Error starting scan: ${e.message}")
            isScanning = false
        }
    }

    fun connectToDevice(device: BluetoothDevice) {
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
        // 保存正在连接的设备信息
        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPref.edit().putString(KEY_DEVICE_ADDRESS, device.address)
            .putString(KEY_DEVICE_NAME, device.name)
            .apply()
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothGatt.STATE_CONNECTED -> {
                    bleCallback?.onConnected(gatt)
                    gatt.discoverServices()
                    mGatt = gatt
                }
                BluetoothGatt.STATE_DISCONNECTED -> {
                    bleCallback?.onDisconnected(gatt)
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                initCharacteristic()
                
                // 特性初始化完成后，尝试恢复传输状态
                val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val isTransferring = sharedPref.getBoolean(KEY_IS_TRANSFERRING, false)
                
                if (isTransferring) {
                    // 延迟一点点再发送，确保特性配置完成
                    Handler(Looper.getMainLooper()).postDelayed({
                        Log.d(tag, "服务发现完成后自动恢复传输")
                        sendControlCommand(ByteUtil.packTransferCommand(true))
                    }, 500)
                }
            }
            else{
                Log.w(tag, "onServicesDiscovered received: $status")
            }
        }

        /*
        * 莫名其妙的很，那个没有被废弃的三个参数的回调函数并不会触发
        * 目前好像只能用这个方法
        * 有些厂商可以用，有的不行
        * */
        override fun onCharacteristicChanged(gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            super.onCharacteristicChanged(gatt, characteristic)
            // value 应该是更新的数据
            val data : ByteArray = characteristic.value
           //callback?.onDataReceived(data)
            Log.i(tag, "onCharacteristicChanged: old" + ByteUtil.parseEcgDataPacket(data))
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            super.onCharacteristicChanged(gatt, characteristic, value)
            bleCallback?.onDataReceived(value)
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            super.onCharacteristicWrite(gatt, characteristic, status)

            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(tag, "Command write successful to characteristic: ${characteristic.uuid}")

                // 如果是控制特征值，记录命令发送成功
                if (characteristic.uuid == UUID.fromString(BleConfig.CHARA_CTRL.toString())) {
                    val command = characteristic.value
                    if (command != null && command.size >= 2) {
                        when {
                            command[0] == 0xFA.toByte() && command[1] == 0x01.toByte() -> {
                                val isStart = command[2] == 0x01.toByte()
                                Log.d(tag, "Collection command ${if (isStart) "START" else "STOP"} sent successfully")

                                if (isStart && command.size >= 10) {
                                    Log.d(tag, "Time data sent to device successfully")
                                    Log.d(tag, "Device should now create ECG.BIN with correct timestamp")
                                }
                            }
                            command[0] == 0xFA.toByte() && command[1] == 0x02.toByte() -> {
                                val isStart = command[2] == 0x01.toByte()
                                Log.d(tag, "Transfer command ${if (isStart) "START" else "STOP"} sent successfully")
                            }
                        }
                    }
                }
            } else {
                Log.e(tag, "Command write failed with status: $status")
            }
        }


        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int
        ) {
            super.onDescriptorWrite(gatt, descriptor, status)
            Log.i(tag, "onDescriptorWrite: ")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("BLE", "Descriptor 写入成功" + descriptor?.uuid)
            } else {
                Log.e("BLE", "Descriptor 写入失败" + descriptor?.uuid)
            }
        }

    }

    // 恢复传输状态
    private fun restoreTransferringState() {
        val sharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isCollecting = sharedPref.getBoolean(KEY_IS_COLLECTING, false)
        val isTransferring = sharedPref.getBoolean(KEY_IS_TRANSFERRING, false)

        if (isCollecting) {
            // 获取保存的开始时间，如果有的话使用它，否则使用null
            val savedStartDateTime = getSavedStartDateTime()
            sendControlCommand(ByteUtil.packCollectCommand(true, savedStartDateTime))
            Log.d(tag, "恢复采集状态，开始时间: $savedStartDateTime")
        }

        if (isTransferring) {
            sendControlCommand(ByteUtil.packTransferCommand(true))
            Log.d(tag, "恢复传输状态")
        }
    }

    fun sendControlCommand(command: ByteArray?) {
        if (!::ctrlCharacteristic.isInitialized) {
            Log.e(tag, "ctrlCharacteristic not initialized yet")
            return
        }

        if (command == null) {
            Log.e(tag, "sendControlCommand: command is null")
            return
        }

        ctrlCharacteristic.value = command
        val result = mGatt.writeCharacteristic(ctrlCharacteristic)

        Log.d(tag, "sendControlCommand: ${command.joinToString(" ") { "0x%02X".format(it.toInt() and 0xFF) }}")
        Log.d(tag, "writeCharacteristic result: $result")

        // 如果是采集命令，额外记录
        if (command.size >= 2 && command[0] == 0xFA.toByte() && command[1] == 0x01.toByte()) {
            val isStart = command[2] == 0x01.toByte()
            Log.d(tag, "Collection command: ${if (isStart) "START" else "STOP"}")
            if (isStart && command.size >= 9) {
                // 解析直接十六进制格式：年-月-日-时-分-秒
                // 注意：这里是直接的字节值，不是BCD编码
                // 时间数据从索引3开始，只使用年份后两位
                val yearHex = command[3].toInt() and 0xFF     // 年份后两位
                val monthHex = command[4].toInt() and 0xFF
                val dayHex = command[5].toInt() and 0xFF
                val hourHex = command[6].toInt() and 0xFF
                val minuteHex = command[7].toInt() and 0xFF
                val secondHex = command[8].toInt() and 0xFF

                Log.d(tag, "Start time in command (HEX): 20$yearHex/$monthHex/$dayHex $hourHex:$minuteHex:$secondHex")
                Log.d(tag, "Raw HEX values: 0x${String.format("%02X", yearHex)} 0x${String.format("%02X", monthHex)} 0x${String.format("%02X", dayHex)} 0x${String.format("%02X", hourHex)} 0x${String.format("%02X", minuteHex)} 0x${String.format("%02X", secondHex)}")
                Log.d(tag, "Expected ECG header (protocol format): 0x${String.format("%02X%02X%02X%02X%02X%02X", yearHex, monthHex, dayHex, hourHex, minuteHex, secondHex)}")
            }
        }
    }

    fun initCharacteristic() {
        firService = mGatt.getService(BleConfig.SERVICE_FIR)
        secService = mGatt.getService(BleConfig.SERVICE_SEC)

        // initCharacteristic()
        ctrlCharacteristic = firService.getCharacteristic(BleConfig.CHARA_CTRL)
        // 设置命令可发送
        ctrlCharacteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        infoCharacteristic = firService.getCharacteristic(BleConfig.CHARA_INFO)

        dataCharacteristic = secService.getCharacteristic(BleConfig.CHARA_DATA)

        // 设置数据可接收
        mGatt.setCharacteristicNotification(dataCharacteristic,true)
        mGatt.setCharacteristicNotification(infoCharacteristic,true)

        // initCharacteristic: 00002902-0000-1000-8000-00805f9b34fb
        for (descriptor in dataCharacteristic.descriptors){
            descriptor?.let{
                it.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                Log.i(tag, "initCharacteristic: " + descriptor.uuid)
                if (mGatt.writeDescriptor(descriptor))
                    Log.i(tag, "initCharacteristic: write data descriptor success")
            }
        }

        // 第二个 descriptor 可能写入失败 这块分开写
        for (descriptor in infoCharacteristic.descriptors){
            descriptor?.let{
                it.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                Log.i(tag, "initCharacteristic: " + descriptor.uuid)
                if (mGatt.writeDescriptor(descriptor))
                    Log.i(tag, "initCharacteristic: write info descriptor success")
            }
        }

        // val descriptor = dataCharacteristic.getDescriptor(BleConfig.DEFAULT_CLIENT)
    }
    
    fun stopBleScan() {
        if (!isScanning) return
        
        try {
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
            isScanning = false
            Log.i(tag, "stopBleScan: ")
        } catch (e: Exception) {
            Log.e(tag, "Error stopping scan: ${e.message}")
        }
    }

    fun disconnect() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
    }

    fun close() {
        bluetoothGatt?.let { gatt ->
            gatt.close()
            bluetoothGatt = null
        }
    }
}
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
import java.util.Timer
import java.util.TimerTask

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

    fun sendControlCommand(command: ByteArray?) {
        if (!::ctrlCharacteristic.isInitialized) {
            Log.e(tag, "ctrlCharacteristic not initialized yet")
            return
        }
        ctrlCharacteristic.value = command
        mGatt.writeCharacteristic(ctrlCharacteristic)
        Log.d(tag, "sendControlCommand: ")
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
package com.liuxinyu.neurosleep.feature.stimulus

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.slider.Slider
import com.liuxinyu.neurosleep.R
import com.liuxinyu.neurosleep.databinding.ActivityStimulusBinding
import com.liuxinyu.neurosleep.feature.stimulus.adapter.DeviceListAdapter
import com.liuxinyu.neurosleep.feature.stimulus.ble.StimulusBleClient
import com.liuxinyu.neurosleep.feature.stimulus.config.StimulusConfig
import com.liuxinyu.neurosleep.feature.stimulus.model.StimulusParams
import com.liuxinyu.neurosleep.feature.stimulus.viewmodel.StimulusViewModel

/**
 * 刺激设备控制Activity
 * 提供用户界面用于连接设备和控制刺激参数
 */
class StimulusActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = StimulusConfig.LogTag.STIMULUS_ACTIVITY
    }
    
    private lateinit var binding: ActivityStimulusBinding
    private lateinit var viewModel: StimulusViewModel
    private lateinit var bleClient: StimulusBleClient
    private lateinit var deviceAdapter: DeviceListAdapter
    
    // 权限请求
    private val bluetoothPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            initializeBluetooth()
        } else {
            Toast.makeText(this, "需要蓝牙权限才能使用此功能", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStimulusBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        checkBluetoothPermissions()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    private fun checkBluetoothPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
        
        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (missingPermissions.isNotEmpty()) {
            bluetoothPermissionLauncher.launch(missingPermissions.toTypedArray())
        } else {
            initializeBluetooth()
        }
    }
    
    private fun initializeBluetooth() {
        // 初始化蓝牙客户端
        bleClient = StimulusBleClient(this)
        
        // 初始化ViewModel
        val factory = StimulusViewModel.Factory(bleClient)
        viewModel = ViewModelProvider(this, factory)[StimulusViewModel::class.java]
        
        setupUI()
        observeViewModel()
    }
    
    private fun setupUI() {
        // 设置设备列表
        deviceAdapter = DeviceListAdapter { device ->
            viewModel.connectDevice(device)
        }
        binding.rvDevices.layoutManager = LinearLayoutManager(this)
        binding.rvDevices.adapter = deviceAdapter
        
        // 设置刺激模式下拉框
        val stimulusModes = arrayOf("直流", "矩形", "双频")
        val modeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, stimulusModes)
        modeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerStimulusMode.adapter = modeAdapter
        binding.spinnerStimulusMode.setSelection(1) // 默认选择矩形
        
        setupClickListeners()
        setupSliders()
    }
    
    private fun setupClickListeners() {
        // 扫描按钮
        binding.btnScan.setOnClickListener {
            if (viewModel.isScanning.value == true) {
                viewModel.stopScan()
            } else {
                viewModel.startScan()
            }
        }
        
        // 断开连接按钮
        binding.btnDisconnect.setOnClickListener {
            viewModel.disconnect()
        }
        
        // 刺激控制按钮
        binding.btnStartStimulus.setOnClickListener {
            viewModel.controlStimulus(true)
        }
        
        binding.btnStopStimulus.setOnClickListener {
            viewModel.controlStimulus(false)
        }
        
        // 通道选择
        binding.toggleChannel.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val channel = when (checkedId) {
                    R.id.btn_channel_1 -> StimulusConfig.Channel.CHANNEL_1
                    R.id.btn_channel_2 -> StimulusConfig.Channel.CHANNEL_2
                    else -> StimulusConfig.Channel.CHANNEL_1
                }
                viewModel.setChannel(channel)
            }
        }
        
        // 刺激模式选择
        binding.spinnerStimulusMode.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val mode = when (position) {
                    0 -> StimulusConfig.StimulusMode.DC
                    1 -> StimulusConfig.StimulusMode.RECTANGULAR
                    2 -> StimulusConfig.StimulusMode.DUAL_FREQUENCY
                    else -> StimulusConfig.StimulusMode.RECTANGULAR
                }
                viewModel.setStimulusMode(mode)
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
    
    private fun setupSliders() {
        // 工作时间滑块
        binding.sliderWorkTime.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                binding.tvWorkTimeValue.text = value.toInt().toString()
                viewModel.setWorkTime(value.toInt())
            }
        }
        
        // 频率滑块
        binding.sliderFrequency.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                binding.tvFrequencyValue.text = value.toInt().toString()
                viewModel.setFrequency(value.toInt())
            }
        }
        
        // 脉宽滑块
        binding.sliderPulseWidth.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                binding.tvPulseWidthValue.text = value.toInt().toString()
                viewModel.setPulseWidth(value.toInt())
            }
        }
        
        // 电流强度滑块
        binding.sliderIntensity.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                binding.tvIntensityValue.text = String.format("%.1f", value)
                viewModel.setIntensity(value)
            }
        }
    }
    
    private fun observeViewModel() {
        // 观察连接状态
        viewModel.isConnected.observe(this) { isConnected ->
            updateConnectionUI(isConnected)
        }
        
        // 观察扫描状态
        viewModel.isScanning.observe(this) { isScanning ->
            binding.btnScan.text = if (isScanning) "停止扫描" else "扫描设备(5秒)"
            binding.progressIndicator.visibility = if (isScanning) View.VISIBLE else View.GONE
        }
        
        // 观察连接消息
        viewModel.connectionMessage.observe(this) { message ->
            binding.tvConnectionStatus.text = message
        }
        
        // 观察设备列表
        viewModel.devicesList.observe(this) { devices ->
            deviceAdapter.updateDevices(devices)
            binding.cardDeviceList.visibility = if (devices.isNotEmpty()) View.VISIBLE else View.GONE
        }
        
        // 观察当前设备
        viewModel.currentDevice.observe(this) { device ->
            if (device != null) {
                binding.tvDeviceName.text = device.name ?: device.address
                binding.tvDeviceName.visibility = View.VISIBLE
            } else {
                binding.tvDeviceName.visibility = View.GONE
            }
        }
        
        // 观察设备状态
        viewModel.deviceStatus.observe(this) { status ->
            val statusText = when (status) {
                StimulusConfig.DeviceStatus.STANDBY -> "待机"
                StimulusConfig.DeviceStatus.RUNNING -> "运行"
                StimulusConfig.DeviceStatus.PAUSED -> "暂停"
                else -> "未知"
            }
            binding.tvDeviceStatus.text = statusText
        }
        
        // 观察刺激参数
        viewModel.stimulusParams.observe(this) { params ->
            updateParametersUI(params)
        }
        
        // 观察错误信息
        viewModel.errorMessage.observe(this) { message ->
            if (message.isNotEmpty()) {
                binding.tvErrorMessage.text = message
                binding.tvErrorMessage.visibility = View.VISIBLE
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            } else {
                binding.tvErrorMessage.visibility = View.GONE
            }
        }
        
        // 观察命令执行状态
        viewModel.isCommandExecuting.observe(this) { isExecuting ->
            binding.progressIndicator.visibility = if (isExecuting) View.VISIBLE else View.GONE
        }
    }
    
    private fun updateConnectionUI(isConnected: Boolean) {
        binding.btnDisconnect.isEnabled = isConnected
        binding.cardDeviceStatus.visibility = if (isConnected) View.VISIBLE else View.GONE
        binding.cardStimulusControl.visibility = if (isConnected) View.VISIBLE else View.GONE
        binding.cardParameters.visibility = if (isConnected) View.VISIBLE else View.GONE
        
        if (!isConnected) {
            binding.cardDeviceList.visibility = View.GONE
        }
    }
    
    private fun updateParametersUI(params: StimulusParams) {
        // 更新通道选择
        when (params.channel) {
            StimulusConfig.Channel.CHANNEL_1 -> binding.btnChannel1.isChecked = true
            StimulusConfig.Channel.CHANNEL_2 -> binding.btnChannel2.isChecked = true
        }
        binding.tvCurrentChannel.text = params.getChannelName()
        
        // 更新刺激模式
        val modePosition = when (params.stimulusMode) {
            StimulusConfig.StimulusMode.DC -> 0
            StimulusConfig.StimulusMode.RECTANGULAR -> 1
            StimulusConfig.StimulusMode.DUAL_FREQUENCY -> 2
            else -> 1
        }
        binding.spinnerStimulusMode.setSelection(modePosition)
        
        // 更新滑块值（避免触发监听器）
        binding.sliderWorkTime.value = params.workTimeMinutes.toFloat()
        binding.tvWorkTimeValue.text = params.workTimeMinutes.toString()
        
        binding.sliderFrequency.value = params.frequency.toFloat()
        binding.tvFrequencyValue.text = params.frequency.toString()
        
        binding.sliderPulseWidth.value = params.pulseWidth.toFloat()
        binding.tvPulseWidthValue.text = params.pulseWidth.toString()
        
        binding.sliderIntensity.value = params.intensity
        binding.tvIntensityValue.text = String.format("%.1f", params.intensity)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (::viewModel.isInitialized) {
            viewModel.disconnect()
        }
    }
}

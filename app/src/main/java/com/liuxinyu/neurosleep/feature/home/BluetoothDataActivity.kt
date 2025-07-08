package com.liuxinyu.neurosleep.feature.home

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Chronometer
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.liuxinyu.neurosleep.DeviceAdapter
import com.liuxinyu.neurosleep.MainActivity
import com.liuxinyu.neurosleep.R
import com.liuxinyu.neurosleep.core.ble.ByteUtil
import com.liuxinyu.neurosleep.data.database.AppDatabase
import com.liuxinyu.neurosleep.data.model.EcgLabel
import com.liuxinyu.neurosleep.data.model.LabelType
import com.liuxinyu.neurosleep.data.repository.EcgLabelRepository
import com.liuxinyu.neurosleep.feature.home.ui.EcgDisplayManager
import com.liuxinyu.neurosleep.feature.home.ui.FullScreenManager
import com.liuxinyu.neurosleep.feature.home.ui.HeartRateChartManager
import com.liuxinyu.neurosleep.feature.home.ui.PermissionManager
import com.liuxinyu.neurosleep.feature.home.view.EcgShowView
import com.liuxinyu.neurosleep.feature.home.viewmodel.BluetoothViewModel
import com.liuxinyu.neurosleep.feature.home.viewmodel.EcgLabelViewModel
import com.liuxinyu.neurosleep.feature.home.viewmodel.EcgLabelViewModelFactory
import com.liuxinyu.neurosleep.util.user.AuthManager
import com.yabu.livechart.view.LiveChart
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import android.content.Context
import com.liuxinyu.neurosleep.core.ble.BleClient
import com.liuxinyu.neurosleep.core.ble.EcgProcessor

class BluetoothDataActivity : AppCompatActivity() {
    companion object {
        const val TAG = "BluetoothDataActivity"
    }

    // ViewModels
    private lateinit var bluetoothViewModel: BluetoothViewModel
    private lateinit var ecgLabelViewModel: EcgLabelViewModel

    // Managers
    private lateinit var permissionManager: PermissionManager
    private lateinit var ecgDisplayManager: EcgDisplayManager
    private lateinit var heartRateChartManager: HeartRateChartManager
    private lateinit var fullScreenManager: FullScreenManager

    // UI Components
    private lateinit var deviceStateView: TextView
    private lateinit var collectStateView: TextView
    private lateinit var startCollectButton: MaterialButton
    private lateinit var stopCollectButton: MaterialButton
    private lateinit var startTransferButton: MaterialButton
    private lateinit var stopTransferButton: MaterialButton
    private lateinit var bluetoothButton: MaterialButton
    private lateinit var devicesList: MutableList<BluetoothDevice>
    private lateinit var deviceAdapter: DeviceAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var ecgView: EcgShowView
    private lateinit var heartRateText: TextView
    private lateinit var heartRateChart: LiveChart
    private lateinit var chronometer: Chronometer
    private lateinit var btnFullScreen: ImageButton
    private lateinit var hrvBtnFullScreen: ImageButton
    private lateinit var btnZoomIn: ImageButton
    private lateinit var btnZoomOut: ImageButton
    private lateinit var hrvbtnZoomIn: ImageButton
    private lateinit var hrvbtnZoomOut: ImageButton
    private lateinit var ecgFrame: FrameLayout
    private lateinit var hrvFrame: FrameLayout
    private lateinit var selectlabelButton: Button
    private lateinit var checklabelButton: Button
    private lateinit var backButton: Button
    private lateinit var statusLayout: LinearLayout
    private lateinit var timeView: TextView
    private lateinit var chronometer_label: Chronometer
    private lateinit var endButton: Button
    private lateinit var status_label: TextView
    private lateinit var rootLayout: ConstraintLayout
    private lateinit var chartSelector2: Spinner
    private var hrvDataType = "心率" // 默认显示心率
    private val hrvDataTypes = arrayOf("心率", "meanHR", "RMSSD", "SDNN")
    
    // Other variables
    private val executor = Executors.newFixedThreadPool(4)
    private val labelList = mutableListOf<EcgLabel>()
    private lateinit var repository: EcgLabelRepository
    private val mainHandler = Handler(Looper.getMainLooper())
    private var selectedLabelType: LabelType = LabelType.SLEEP

    // ECG缩放相关变量
    private var currentScale = 1.0f
    private val MIN_SCALE = 0.5f
    private val MAX_SCALE = 2.0f
    private val SCALE_STEP = 0.1f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "Activity created")
        setContentView(R.layout.activity_bluetooth_data)

        // 初始化BleClient
        val bleClient = BleClient(this)
        
        // 初始化ViewModels
        val bluetoothViewModelFactory = BluetoothViewModel.Factory(bleClient)
        bluetoothViewModel = ViewModelProvider(this, bluetoothViewModelFactory)[BluetoothViewModel::class.java]

        // 初始化依赖
        val dao = AppDatabase.getInstance(this).ecgLabelDao()
        repository = EcgLabelRepository.getInstance(dao, this)
        
        // 设置当前用户ID
        val phone = AuthManager.getPhone(this)
        if (phone == null) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        repository.setUserId(phone)
        
        val factory = EcgLabelViewModelFactory(repository, this)
        ecgLabelViewModel = ViewModelProvider(this, factory)[EcgLabelViewModel::class.java]

        // 初始化UI
        initView()
        initEvent()
        
        // 初始化Managers
        permissionManager = PermissionManager(this)
        rootLayout = findViewById(R.id.rootLayout)
        
        // 初始化心率图表管理器
        heartRateChartManager = HeartRateChartManager(
            this,
            heartRateChart
        )
        
        // 初始化ECG显示管理器，传入心率图表管理器
        ecgDisplayManager = EcgDisplayManager(
            this,
            ecgView,
            heartRateText,
            executor,
            heartRateChartManager
        )
        
        fullScreenManager = FullScreenManager(
            this,
            rootLayout,
            ecgFrame,
            hrvFrame,
            ecgView
        )

        // 观察ViewModel数据变化
        observeViewModels()
    }

    private fun observeViewModels() {
        // 观察蓝牙设备列表变化
        bluetoothViewModel.devicesList.observe(this) { devices ->
            devicesList.clear()
            devicesList.addAll(devices)
            deviceAdapter.notifyDataSetChanged()
        }

        // 观察连接状态变化
        bluetoothViewModel.connectionState.observe(this) { state ->
            deviceStateView.text = state
        }

        // 观察采集状态变化
        bluetoothViewModel.collectionState.observe(this) { state ->
            collectStateView.text = state
        }

        // 观察是否连接
        bluetoothViewModel.isConnected.observe(this) { connected ->
            if (!connected) {
                // 重置UI
                chronometer.stop()
                chronometer.base = SystemClock.elapsedRealtime()
                heartRateChart.visibility = View.INVISIBLE
                ecgDisplayManager.clearData()
                heartRateChartManager.clearData()
            }
        }

        // 观察ECG数据变化
        lifecycleScope.launch {
            bluetoothViewModel.ecgDataReceived.collect { data ->
                data?.let {
                    val ecgData = ByteUtil.parseEcgDataPacket(it)
                    for (point in ecgData) {
                        ecgDisplayManager.addDataPoint(point.ecg1, point.ecg2, point.ecg3)
                        // 这里可以添加根据不同数据类型的处理
                    }
                    // 每收集到足够的数据点后计算HRV指标
                    if (ecgDisplayManager.getDataQueueSize() >= 200) { // 假设采样率为200Hz，收集1秒数据
                        calculateHrvMetrics()
                    }
                }
            }
        }

        // 观察标签数据变化
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                ecgLabelViewModel.labels.collect { labels ->
                    Log.d(TAG, "Received labels from database: ${labels.size}")
                    // 更新本地列表
                    labelList.clear()
                    labelList.addAll(labels)
                    Log.d(TAG, "Updated local labelList: ${labelList.size} items")
                }
            }
        }
    }

    private fun initView() {
        // 找到UI组件
        deviceStateView = findViewById(R.id.deviceState)
        collectStateView = findViewById(R.id.collectState)
        startCollectButton = findViewById(R.id.startCollectButton)
        stopCollectButton = findViewById(R.id.stopCollectButton)
        startTransferButton = findViewById(R.id.startTransferButton)
        stopTransferButton = findViewById(R.id.stopTransferButton)
        bluetoothButton = findViewById(R.id.bleButton)
        backButton = findViewById(R.id.backButton)

        // 添加返回主页面按钮的点击事件
        findViewById<MaterialButton>(R.id.returnHomeButton).setOnClickListener {
            onBackPressed()
        }

        recyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)

        devicesList = mutableListOf()

        deviceAdapter = DeviceAdapter(devicesList) { device ->
            // 处理连接点击事件
            bluetoothViewModel.connectToDevice(device)
        }

        recyclerView.adapter = deviceAdapter
        selectlabelButton = findViewById(R.id.selectlabel_button)
        checklabelButton = findViewById(R.id.checklabel_button)
        statusLayout = findViewById(R.id.status_layout)
        chronometer_label = findViewById(R.id.chronometer_label)
        endButton = findViewById(R.id.end_button)
        status_label = findViewById(R.id.status_label)
        timeView = findViewById(R.id.time_display)

        // 初始化ECG相关组件
        ecgView = findViewById(R.id.real_time_ecgview)
        heartRateText = findViewById(R.id.heart_rate)
        heartRateChart = findViewById(R.id.real_time_heartrate)
        chronometer = findViewById(R.id.chronometer)
        btnFullScreen = findViewById(R.id.btnFullScreen)
        hrvBtnFullScreen = findViewById(R.id.hrvbtnFullScreen)
        btnZoomIn = findViewById(R.id.btnZoomIn)
        btnZoomOut = findViewById(R.id.btnZoomOut)
        hrvbtnZoomIn = findViewById(R.id.hrvbtnZoomIn)
        hrvbtnZoomOut = findViewById(R.id.hrvbtnZoomOut)
        ecgFrame = findViewById(R.id.ecg_frame)
        hrvFrame = findViewById(R.id.hrv_frame)

        // 设置心率图背景
        hrvFrame.setBackgroundColor(Color.parseColor("#F5F5F5"))
        val gridView = GridBackgroundView(this)
        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        hrvFrame.addView(gridView, 0)  // 添加网格视图作为第一个子视图

        // 下拉选择ecg
        setupEcgSpinner()

        // 初始化chart_selector2
        chartSelector2 = findViewById(R.id.chart_selector2)
        
        // 设置chart_selector2的适配器
        val hrvTypeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, hrvDataTypes)
        hrvTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        chartSelector2.adapter = hrvTypeAdapter
    }

    private fun setupEcgSpinner() {
        val chartSelector: Spinner = findViewById(R.id.chart_selector)
        val ecgOptions = resources.getStringArray(R.array.ecg_options)

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            ecgOptions
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        chartSelector.adapter = adapter

        chartSelector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedOption = ecgOptions[position]
                when (selectedOption) {
                    "ecg1" -> ecgDisplayManager.setEcgType(EcgDisplayManager.EcgType.ECG1)
                    "ecg2" -> ecgDisplayManager.setEcgType(EcgDisplayManager.EcgType.ECG2)
                    "ecg3" -> ecgDisplayManager.setEcgType(EcgDisplayManager.EcgType.ECG3)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun initEvent() {
        // 设置按钮点击事件
        bluetoothButton.setOnClickListener {
            permissionManager.checkAndRequestBluetoothPermissions { granted ->
                if (granted) {
                    bluetoothViewModel.startScan()
                }
            }
        }

        startCollectButton.setOnClickListener {
            bluetoothViewModel.startCollecting()
            chronometer.base = SystemClock.elapsedRealtime()
            chronometer.start()
            heartRateChart.visibility = View.VISIBLE
            ecgDisplayManager.startUpdating()
        }

        stopCollectButton.setOnClickListener {
            bluetoothViewModel.stopCollecting()
            chronometer.stop()
            ecgDisplayManager.stopUpdating()
            ecgDisplayManager.clearData()
            heartRateChartManager.clearData()
            heartRateChart.visibility = View.INVISIBLE
        }

        startTransferButton.setOnClickListener {
            bluetoothViewModel.startTransferring()
            heartRateChart.visibility = View.VISIBLE
            ecgDisplayManager.startUpdating()
        }

        stopTransferButton.setOnClickListener {
            bluetoothViewModel.stopTransferring()
            ecgDisplayManager.stopUpdating()
            ecgDisplayManager.clearData()
            heartRateChartManager.clearData()
            heartRateChart.visibility = View.INVISIBLE
        }

        // 设置全屏按钮点击事件
        btnFullScreen.setOnClickListener {
            val isFullScreen = fullScreenManager.toggleEcgFullScreen()
            btnFullScreen.setImageResource(
                if (isFullScreen) R.drawable.ic_fullscreen_exit else R.drawable.ic_fullscreen
            )
        }

        hrvBtnFullScreen.setOnClickListener {
            val isFullScreen = fullScreenManager.toggleHrvFullScreen()
            hrvBtnFullScreen.setImageResource(
                if (isFullScreen) R.drawable.ic_fullscreen_exit else R.drawable.ic_fullscreen
            )
        }

        // 设置缩放按钮点击事件
        btnZoomIn.setOnClickListener {
            zoomIn()
        }

        btnZoomOut.setOnClickListener {
            zoomOut()
        }

        hrvbtnZoomIn.setOnClickListener {
            heartRateChartManager.zoomIn()
        }

        hrvbtnZoomOut.setOnClickListener {
            heartRateChartManager.zoomOut()
        }

        // 蓝牙状态卡点击事件
        findViewById<MaterialCardView>(R.id.status_card).setOnClickListener {
            permissionManager.checkAndRequestBluetoothPermissions { granted ->
                if (granted) {
                    bluetoothViewModel.startScan()
                }
            }
        }

        // 设置chart_selector2的选择事件
        chartSelector2.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                hrvDataType = hrvDataTypes[position]
                // 更新图表类型
                heartRateChartManager.setChartType(hrvDataType)
                // 更新UI，表示当前选择的数据类型
                updateHrvChartTitle()
                // 如果已连接设备并正在传输数据，不需要手动更新图表
                // 因为下一次数据到达时会自动使用新的类型绘制
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // 不做任何操作
            }
        }
    }

    // ECG视图缩放方法
    private fun zoomIn() {
        if (currentScale < MAX_SCALE) {
            currentScale += SCALE_STEP
            applyZoom()
        }
    }

    private fun zoomOut() {
        if (currentScale > MIN_SCALE) {
            currentScale -= SCALE_STEP
            applyZoom()
        }
    }

    private fun applyZoom() {
        // 更新 ECG 视图的缩放
        ecgView.setScale(currentScale)
        ecgView.invalidate()
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onResume() {
        super.onResume()
        permissionManager.checkAndRequestBluetoothPermissions { granted ->
            if (granted) {
                // 可以初始化蓝牙操作
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // 防止布局重置（已在AndroidManifest中配置configChanges）
        ecgView.requestLayout()
    }

    override fun onDestroy() {
        super.onDestroy()
        // 清理资源
        ecgDisplayManager.onDestroy()
        bluetoothViewModel.clearData()
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        // 如果正在扫描，先停止扫描
        bluetoothViewModel.stopScan()
        // 如果已连接设备，先断开连接
        if (bluetoothViewModel.isConnected.value == true) {
            bluetoothViewModel.disconnect()
        }
        // 返回主页面
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
        
        // 调用父类方法
        super.onBackPressed()
    }

    private class GridBackgroundView(context: Context) : View(context) {
        private val paint = Paint().apply {
            color = Color.parseColor("#D8D8D8")
            strokeWidth = 0.5f
            style = Paint.Style.STROKE
            alpha = 255
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            // 绘制水平网格线
            val horizontalSpacing = height / 10f
            for (i in 0..10) {
                val y = i * horizontalSpacing
                canvas.drawLine(0f, y, width.toFloat(), y, paint)
            }
            // 绘制垂直网格线
            val verticalSpacing = width / 20f
            for (i in 0..20) {
                val x = i * verticalSpacing
                canvas.drawLine(x, 0f, x, height.toFloat(), paint)
            }
        }
    }

    // 添加更新图表标题的方法
    private fun updateHrvChartTitle() {
        // 修复属性引用检查的语法
        try {
            val titleTextView = findViewById<TextView>(R.id.heart_rate_title)
            titleTextView?.text = hrvDataType
        } catch (e: Exception) {
            Log.e(TAG, "更新心率标题出错: ${e.message}")
        }
    }

    // 添加计算HRV指标的方法
    private fun calculateHrvMetrics() {
        // 从ECG数据中获取RR间隔
        val ecgData = ecgDisplayManager.getLastEcgData(10) // 获取最近10秒的数据
        if (ecgData.isNotEmpty()) {
            try {
                // 检测R峰
                val rPeakIndices = EcgProcessor.detectRPeaks(ecgData)
                // 如果检测到足够的R峰，计算HRV指标
                if (rPeakIndices.size >= 2) {
                    val hrvMetrics = EcgProcessor.calculateHrvMetrics(rPeakIndices)
                    
                    // 根据选择的数据类型，更新图表和显示值
                    when (hrvDataType) {
                        "心率" -> {
                            val heartRate = EcgProcessor.calculateHeartRate(ecgData)
                            heartRateChartManager.addPoint(heartRate.toFloat())
                            heartRateText.text = "${heartRate.toInt()}"
                        }
                        "meanHR" -> {
                            // 计算平均心率并更新图表
                            val meanHr = 60.0 * 200 / rPeakIndices.zipWithNext { a, b -> (b - a).toDouble() }.average()
                            heartRateChartManager.addPoint(meanHr.toFloat())
                            heartRateText.text = "${meanHr.toInt()}"
                        }
                        "RMSSD" -> {
                            // 显示RMSSD值
                            val rmssdValue = hrvMetrics.rmssd * 1000 // 转换为毫秒
                            heartRateChartManager.addPoint(rmssdValue.toFloat())
                            heartRateText.text = String.format("%.1f", rmssdValue)
                        }
                        "SDNN" -> {
                            // 显示SDNN值
                            val sdnnValue = hrvMetrics.sdnn * 1000 // 转换为毫秒
                            heartRateChartManager.addPoint(sdnnValue.toFloat())
                            heartRateText.text = String.format("%.1f", sdnnValue)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "计算HRV指标出错: ${e.message}")
            }
        }
    }
}
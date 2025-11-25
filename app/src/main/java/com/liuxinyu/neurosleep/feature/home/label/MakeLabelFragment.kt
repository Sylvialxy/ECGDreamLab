package com.liuxinyu.neurosleep.feature.home.label

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Chronometer
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.liuxinyu.neurosleep.R
import com.liuxinyu.neurosleep.data.database.AppDatabase
import com.liuxinyu.neurosleep.data.model.EcgLabel
import com.liuxinyu.neurosleep.data.repository.EcgLabelRepository
import com.liuxinyu.neurosleep.feature.home.viewmodel.EcgLabelViewModel
import com.liuxinyu.neurosleep.feature.home.viewmodel.EcgLabelViewModelFactory
import com.liuxinyu.neurosleep.data.model.LabelType
import com.liuxinyu.neurosleep.util.user.AuthManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar

class MakeLabelFragment : Fragment() {

    private lateinit var viewModel: EcgLabelViewModel
    private lateinit var chronometer: Chronometer
    private lateinit var statusLayout: LinearLayout
    private lateinit var statusLabel: TextView
    private lateinit var timeDisplay: TextView
    private var chronometerBase: Long = 0
    private var countUpHandler: Handler? = null
    private var countUpRunnable: Runnable? = null
    private var currentDialog: AlertDialog? = null
    private var selectedLabelType: LabelType = LabelType.SLEEP
    private var timeUpdateHandler: Handler? = null
    private var timeUpdateRunnable: Runnable? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_make_label, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 初始化依赖
        val dao = AppDatabase.getInstance(requireContext()).ecgLabelDao()
        val repository = EcgLabelRepository.getInstance(dao, requireContext())
        
        // 设置当前用户ID
        val phone = AuthManager.getPhone(requireContext())
        if (phone == null) {
            Toast.makeText(requireContext(), "请先登录", Toast.LENGTH_SHORT).show()
            return
        }
        Log.d("MakeLabelFragment", "Setting user ID: $phone")
        repository.setUserId(phone)
        
        val factory = EcgLabelViewModelFactory(repository, requireContext())
        viewModel = ViewModelProvider(requireActivity(), factory)[EcgLabelViewModel::class.java]

        // 初始化视图
        chronometer = view.findViewById(R.id.chronometer_label)
        statusLayout = view.findViewById(R.id.status_layout)
        statusLabel = view.findViewById(R.id.status_label)
        timeDisplay = view.findViewById(R.id.time_display)

        // 设置时间显示
        updateTimeDisplay()
        startTimeUpdates()

        // 开始观察标签数据
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.labels.collect { labels ->
                    Log.d("MakeLabelFragment", "Received ${labels.size} labels")
                    // 检查是否有正在进行的标签
                    val currentLabel = labels.find { it.endTime == null }
                    if (currentLabel != null) {
                        // 更新UI状态
                        statusLabel.text = getLabelDisplayName(currentLabel.labelType, currentLabel.customName)
                        statusLayout.visibility = View.VISIBLE
                        chronometer.visibility = View.VISIBLE
                        view?.findViewById<View>(R.id.selectlabel_button)?.visibility = View.GONE
                        view?.findViewById<View>(R.id.checklabel_button)?.visibility = View.GONE
                        view?.findViewById<View>(R.id.end_button)?.visibility = View.VISIBLE
                        
                        // 重要修改：根据实际开始时间设置计时器基准
                        setChronometer(currentLabel.startTime)
                    } else {
                        // 重置UI状态
                        statusLayout.visibility = View.GONE
                        chronometer.visibility = View.GONE
                        view?.findViewById<View>(R.id.selectlabel_button)?.visibility = View.VISIBLE
                        view?.findViewById<View>(R.id.checklabel_button)?.visibility = View.VISIBLE
                        view?.findViewById<View>(R.id.end_button)?.visibility = View.GONE
                    }
                }
            }
        }

        // 设置按钮点击事件
        view.findViewById<Button>(R.id.selectlabel_button).setOnClickListener {
            showLabelSelectionDialog()
        }

        view.findViewById<Button>(R.id.checklabel_button).setOnClickListener {
            showCollectionStatusDialog()
        }

        view.findViewById<Button>(R.id.end_button).setOnClickListener {
            endCurrentLabel()
        }

        view.findViewById<View>(R.id.backButton).setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
    }

    private fun updateTimeDisplay() {
        val currentTime = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm:ss")
        timeDisplay.text = currentTime.format(formatter)
    }

    private fun startTimeUpdates() {
        timeUpdateHandler = Handler(Looper.getMainLooper())
        timeUpdateRunnable = object : Runnable {
            override fun run() {
                updateTimeDisplay()
                timeUpdateHandler?.postDelayed(this, 1000)
            }
        }
        timeUpdateHandler?.post(timeUpdateRunnable!!)
    }

    private fun stopTimeUpdates() {
        timeUpdateHandler?.removeCallbacks(timeUpdateRunnable!!)
        timeUpdateHandler = null
        timeUpdateRunnable = null
    }

    private fun showLabelSelectionDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_timer_setup, null)
        val dialog = AlertDialog.Builder(requireContext()).setView(dialogView).create()

        // 获取视图中的控件
        val primarySpinner = dialogView.findViewById<Spinner>(R.id.primary_label_spinner)
        val secondarySpinner = dialogView.findViewById<Spinner>(R.id.secondary_label_spinner)
        val hourPicker = dialogView.findViewById<NumberPicker>(R.id.hour_picker)
        val minutePicker = dialogView.findViewById<NumberPicker>(R.id.minute_picker)
        val timerModeSwitch = dialogView.findViewById<Switch>(R.id.timer_mode_switch)
        val startButton = dialogView.findViewById<Button>(R.id.start_button)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancel_button)

        // 定义一级标签和二级标签的映射关系
        val primaryLabels = arrayOf("睡眠", "日间")
        val secondaryLabelsMap = mapOf(
            "睡眠" to arrayOf("有干预", "无干预"),
            "日间" to arrayOf("静息", "吃饭", "运动", "认知训练", "放松训练", "激励训练", "评估模式")
        )

        // 设置一级标签下拉选项
        val primaryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, primaryLabels)
        primarySpinner.adapter = primaryAdapter

        // 设置二级标签下拉选项（初始显示睡眠的二级标签）
        var secondaryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, secondaryLabelsMap["睡眠"]!!)
        secondarySpinner.adapter = secondaryAdapter

        // 一级标签选择监听器，实现联动
        primarySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedPrimary = parent?.getItemAtPosition(position).toString()
                val secondaryLabels = secondaryLabelsMap[selectedPrimary] ?: emptyArray()
                secondaryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, secondaryLabels)
                secondarySpinner.adapter = secondaryAdapter
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // 设置 NumberPicker 范围
        hourPicker.minValue = 0
        hourPicker.maxValue = 23
        minutePicker.minValue = 0
        minutePicker.maxValue = 59

        startButton.setOnClickListener {
            val selectedPrimary = primarySpinner.selectedItem.toString()
            val selectedSecondary = secondarySpinner.selectedItem.toString()
            // 使用"-"拼接一级标签和二级标签
            val fullLabel = "$selectedPrimary-$selectedSecondary"
            
            // 使用 CUSTOM 类型，并将完整标签存储在 customName 中
            selectedLabelType = LabelType.CUSTOM

            val hour = hourPicker.value
            val minute = minutePicker.value
            val isCountdown = timerModeSwitch.isChecked
            val totalMillis = (hour * 60 + minute) * 60 * 1000L

            dialog.dismiss()
            showStartReminderDialog(isCountdown, totalMillis, fullLabel)
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    @SuppressLint("MissingInflatedId")
    private fun showStartReminderDialog(isCountdown: Boolean, totalMillis: Long, state: String) {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_start_reminder, null)
        val startButton = view.findViewById<Button>(R.id.btn_start)
        val cancelButton = view.findViewById<Button>(R.id.btn_cancel)
        val dialogMessage = view.findViewById<TextView>(R.id.messageText)

        // state 现在包含完整的两级标签（如"睡眠-有干预"）
        val labelText = state
        dialogMessage.text = "您是否要开始【$labelText】？"

        val dialog = AlertDialog.Builder(requireContext())
            .setView(view)
            .setCancelable(false)
            .create()

        currentDialog?.dismiss()
        currentDialog = dialog
        dialog.show()

        startButton.setOnClickListener {
            statusLabel.text = labelText
            val now = LocalDateTime.now()
            
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val existingUnfinishedLabel = viewModel.labels.value.find { it.endTime == null }
                    if (existingUnfinishedLabel != null) {
                        existingUnfinishedLabel.endTime = now
                        viewModel.updateLabel(existingUnfinishedLabel)
                    }
                    
                    val newLabel = EcgLabel(
                        labelType = selectedLabelType,
                        startTime = now,
                        endTime = null,
                        customName = state  // 存储完整的两级标签字符串（用"-"拼接）
                    )
                    viewModel.addLabel(newLabel)
                    
                    withContext(Dispatchers.Main) {
                        dialog.dismiss()
                        
                        // 更新UI状态
                        view?.findViewById<View>(R.id.selectlabel_button)?.visibility = View.GONE
                        view?.findViewById<View>(R.id.checklabel_button)?.visibility = View.GONE
                        statusLayout.visibility = View.VISIBLE
                        chronometer.visibility = View.VISIBLE
                        view?.findViewById<View>(R.id.end_button)?.visibility = View.VISIBLE

                        // 启动计时器 - 修改为使用新方法
                        setChronometer(now)

                        // 根据模式启动相应的计时器
                        if (isCountdown) {
                            startCountdown(totalMillis, state)
                        } else {
                            startCountUp(state)
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "操作失败，请重试", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                }
            }
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }
    }

    private fun endCurrentLabel() {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_end_reminder, null)
        val endButton = view.findViewById<Button>(R.id.btn_end)
        val continueButton = view.findViewById<Button>(R.id.btn_continue)
        val dialogMessage = view.findViewById<TextView>(R.id.messageText)

        lifecycleScope.launch(Dispatchers.IO) {
            val currentLabel = viewModel.labels.value.find { it.endTime == null }

            if (currentLabel == null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "没有正在进行的标签", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            val labelName = getLabelDisplayName(currentLabel.labelType, currentLabel.customName)
            val duration = Duration.between(currentLabel.startTime, LocalDateTime.now())
            val hours = duration.toHours()
            val minutes = duration.toMinutes() % 60

            val durationText = when {
                hours > 0 -> "${hours}小时${minutes}分钟"
                else -> "${minutes}分钟"
            }

            withContext(Dispatchers.Main) {
                dialogMessage.text = "您已经【${labelName}】【${durationText}】，是否结束当前状态"

                val dialog = AlertDialog.Builder(requireContext())
                    .setView(view)
                    .setCancelable(false)
                    .create()

                currentDialog?.dismiss()
                currentDialog = dialog
                dialog.show()

                endButton.setOnClickListener {
                    lifecycleScope.launch(Dispatchers.IO) {
                        val endTime = LocalDateTime.now()
                        currentLabel.endTime = endTime
                        viewModel.updateLabel(currentLabel)
                        
                        withContext(Dispatchers.Main) {
                            statusLabel.text = "无状态"
                            dialog.dismiss()
                            
                            // 停止计时器
                            chronometer.stop()
                            countUpHandler?.removeCallbacks(countUpRunnable!!)
                            countUpHandler = null
                            countUpRunnable = null

                            // 恢复UI状态
                            statusLayout.visibility = View.GONE
                            requireView().findViewById<View>(R.id.selectlabel_button).visibility = View.VISIBLE
                            requireView().findViewById<View>(R.id.checklabel_button).visibility = View.VISIBLE
                        }
                    }
                }

                continueButton.setOnClickListener {
                    dialog.dismiss()
                }
            }
        }
    }

    private fun showCollectionStatusDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_collection_status, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.status_recycler_view)
        val modifyBtn = dialogView.findViewById<Button>(R.id.modify_button)
        val deleteBtn = dialogView.findViewById<Button>(R.id.delete_button)
        val confirmBtn = dialogView.findViewById<Button>(R.id.confirm_button)
        val progressBar = dialogView.findViewById<ProgressBar>(R.id.progress_bar)

        val dialog = AlertDialog.Builder(requireContext()).setView(dialogView).create()
        var selectedPosition = -1
        var lastSelectedView: View? = null

        // 立即显示对话框
        dialog.show()
        progressBar.visibility = View.VISIBLE

        // 观察标签数据
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.labels.collect { labels ->
                    try {
                        val statusList = labels.map { label ->
                            CollectionStatus(
                                state = getLabelDisplayName(label.labelType, label.customName),
                                startTime = formatDateTime(label.startTime),
                                endTime = label.endTime?.let { formatDateTime(it) } ?: "进行中"
                            )
                        }
                        
                        withContext(Dispatchers.Main) {
                            progressBar.visibility = View.GONE
                            
                            recyclerView.layoutManager = LinearLayoutManager(requireContext())
                            val adapter = CollectionStatusAdapter(statusList) { position ->
                                lastSelectedView?.setBackgroundColor(Color.TRANSPARENT)
                                selectedPosition = position
                                val holder = recyclerView.findViewHolderForAdapterPosition(position)
                                holder?.itemView?.let {
                                    it.setBackgroundColor(Color.LTGRAY)
                                    lastSelectedView = it
                                }
                            }
                            recyclerView.adapter = adapter

                            // 设置按钮点击事件
                            setupDialogButtons(
                                dialog,
                                modifyBtn,
                                deleteBtn,
                                confirmBtn,
                                selectedPosition,
                                lastSelectedView,
                                adapter,
                                labels
                            )
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            progressBar.visibility = View.GONE
                            Toast.makeText(requireContext(), "加载失败，请重试", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                        }
                    }
                }
            }
        }
    }

    private fun setupDialogButtons(
        dialog: AlertDialog,
        modifyBtn: Button,
        deleteBtn: Button,
        confirmBtn: Button,
        selectedPosition: Int,
        lastSelectedView: View?,
        adapter: CollectionStatusAdapter,
        labels: List<EcgLabel>
    ) {
        modifyBtn.setOnClickListener {
            val currentSelectedPosition = adapter.getSelectedPosition()
            if (currentSelectedPosition == -1) {
                Toast.makeText(requireContext(), "请先选择要修改的条目", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (currentSelectedPosition >= labels.size) {
                Toast.makeText(requireContext(), "无效的选中位置", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedLabel = labels[currentSelectedPosition]
            showEditLabelDialog(selectedLabel) { updatedLabel ->
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        viewModel.updateLabel(updatedLabel)
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(), "更新失败，请重试", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        deleteBtn.setOnClickListener {
            val currentSelectedPosition = adapter.getSelectedPosition()
            if (currentSelectedPosition == -1) {
                Toast.makeText(requireContext(), "请先选择要删除的条目", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (currentSelectedPosition >= labels.size) {
                Toast.makeText(requireContext(), "无效的选中位置", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            AlertDialog.Builder(requireContext())
                .setTitle("确认删除")
                .setMessage("确定要删除这条记录吗？")
                .setPositiveButton("确定") { _, _ ->
                    lifecycleScope.launch(Dispatchers.IO) {
                        try {
                            val selectedLabel = labels[currentSelectedPosition]
                            viewModel.deleteLabel(selectedLabel)
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(requireContext(), "删除失败，请重试", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                .setNegativeButton("取消", null)
                .show()
        }

        confirmBtn.setOnClickListener { dialog.dismiss() }
    }

    private fun showEditLabelDialog(
        originalLabel: EcgLabel,
        onSave: (EcgLabel) -> Unit
    ) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_label, null)
        val dialog = AlertDialog.Builder(requireContext()).setView(dialogView).create()

        val stateSpinner = dialogView.findViewById<MaterialAutoCompleteTextView>(R.id.spinner_label_type)
        val etCustomName = dialogView.findViewById<EditText>(R.id.et_custom_name)
        val btnStartTime = dialogView.findViewById<Button>(R.id.btn_edit_start_time)
        val btnEndTime = dialogView.findViewById<Button>(R.id.btn_edit_end_time)
        val btnSave = dialogView.findViewById<Button>(R.id.btn_save)
        val btnCancel = dialogView.findViewById<Button>(R.id.btn_cancel)
        var selectedPosition = originalLabel.labelType.ordinal

        val labelTypes = resources.getStringArray(R.array.label_types)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, labelTypes)
        stateSpinner.setAdapter(adapter)
        val initialLabelText = labelTypes[originalLabel.labelType.ordinal]
        stateSpinner.setText(initialLabelText, false)

        if (originalLabel.labelType == LabelType.CUSTOM) {
            etCustomName.setText(originalLabel.customName)
            etCustomName.visibility = View.VISIBLE
        }

        val timeFormatter = DateTimeFormatter.ofPattern("MM/dd HH:mm")
        var newStartTime = originalLabel.startTime
        var newEndTime = originalLabel.endTime

        btnStartTime.text = timeFormatter.format(newStartTime)
        btnEndTime.text = newEndTime?.let { timeFormatter.format(it) } ?: "进行中"

        btnStartTime.setOnClickListener {
            showDateTimePicker(newStartTime) { selectedTime ->
                newStartTime = selectedTime
                btnStartTime.text = timeFormatter.format(selectedTime)
            }
        }

        btnEndTime.setOnClickListener {
            val initialTime = newEndTime ?: LocalDateTime.now()
            showDateTimePicker(initialTime) { selectedTime ->
                newEndTime = selectedTime
                btnEndTime.text = timeFormatter.format(selectedTime)
            }
        }

        stateSpinner.setOnItemClickListener { _, _, position, _ ->
            selectedPosition = position
            val selectedType = LabelType.values()[position]
            val isCustom = (selectedType == LabelType.CUSTOM)
            etCustomName.visibility = if (isCustom) View.VISIBLE else View.GONE
        }

        btnSave.setOnClickListener {
            val isDataChanged = originalLabel.labelType.ordinal != selectedPosition ||
                    originalLabel.startTime != newStartTime ||
                    originalLabel.endTime != newEndTime ||
                    originalLabel.customName != etCustomName.text?.toString()

            if (!isDataChanged) {
                Toast.makeText(requireContext(), "未检测到修改内容", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedType = LabelType.values()[selectedPosition]
            val customName = if (selectedType == LabelType.CUSTOM) {
                etCustomName.text.toString().takeIf { it.isNotBlank() } ?: "自定义"
            } else null

            val updatedLabel = originalLabel.copy(
                labelType = selectedType,
                startTime = newStartTime,
                endTime = newEndTime,
                customName = customName
            )
            onSave(updatedLabel)
            dialog.dismiss()
        }

        btnCancel.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showDateTimePicker(
        initialTime: LocalDateTime,
        onTimeSelected: (LocalDateTime) -> Unit
    ) {
        val calendar = Calendar.getInstance().apply {
            set(initialTime.year, initialTime.monthValue - 1, initialTime.dayOfMonth,
                initialTime.hour, initialTime.minute)
        }

        DatePickerDialog(requireContext(), { _, year, month, day ->
            TimePickerDialog(requireContext(), { _, hour, minute ->
                val selectedTime = LocalDateTime.of(year, month + 1, day, hour, minute)
                onTimeSelected(selectedTime)
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun formatDateTime(dateTime: LocalDateTime): String {
        return try {
            DateTimeFormatter
                .ofPattern("MM/dd HH:mm")
                .format(dateTime)
        } catch (e: Exception) {
            "时间格式错误"
        }
    }

    private fun startCountdown(durationMillis: Long, state: String) {
        object : CountDownTimer(durationMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                val minutes = seconds / 60
                val remainingSeconds = seconds % 60
            }

            override fun onFinish() {
                Toast.makeText(requireContext(), "$state 计时结束", Toast.LENGTH_SHORT).show()
            }
        }.start()
    }

    private fun startCountUp(state: String) {
        val startTime = System.currentTimeMillis()
        countUpHandler = Handler(Looper.getMainLooper())

        countUpRunnable = object : Runnable {
            override fun run() {
                val elapsed = (System.currentTimeMillis() - startTime) / 1000
                val minutes = elapsed / 60
                val seconds = elapsed % 60
                countUpHandler?.postDelayed(this, 1000)
            }
        }

        countUpHandler?.post(countUpRunnable!!)
    }

    private fun fromChineseLabel(label: String): LabelType {
        return when (label) {
            "睡眠" -> LabelType.SLEEP
            "静息" -> LabelType.REST
            "吃饭" -> LabelType.EATING
            "运动" -> LabelType.EXERCISE
            "认知训练" -> LabelType.COGNITIVE_TRAINING
            "放松训练" -> LabelType.RELAXATION_TRAINING
            "激励训练" -> LabelType.MOTIVATIONAL_TRAINING
            "评估模式" -> LabelType.ASSESSMENT_MODE
            "自定义" -> LabelType.CUSTOM
            else -> throw IllegalArgumentException("未知的标签: $label")
        }
    }

    private fun getLabelDisplayName(type: LabelType, customName: String? = null): String {
        // 如果 customName 包含"-"，说明是两级标签格式，直接返回
        if (customName != null && customName.contains("-")) {
            return customName
        }
        
        return when (type) {
            LabelType.SLEEP -> "睡眠"
            LabelType.REST -> "静息"
            LabelType.EATING -> "吃饭"
            LabelType.EXERCISE -> "运动"
            LabelType.COGNITIVE_TRAINING -> "认知训练"
            LabelType.RELAXATION_TRAINING -> "放松训练"
            LabelType.MOTIVATIONAL_TRAINING -> "激励训练"
            LabelType.ASSESSMENT_MODE -> "评估模式"
            LabelType.CUSTOM -> customName ?: "自定义"
        }
    }

    // 添加一个新方法，用于根据开始时间设置计时器的基准值
    private fun setChronometer(startTime: LocalDateTime) {
        try {
            // 计算从开始时间到现在的时间差（以毫秒为单位）
            val now = LocalDateTime.now()
            val durationMillis = Duration.between(startTime, now).toMillis()
            
            // 设置计时器的基准值为当前时间减去已经经过的时间
            val baseTime = SystemClock.elapsedRealtime() - durationMillis
            chronometer.base = baseTime
            chronometer.start()
            
            Log.d("MakeLabelFragment", "设置计时器基准: 经过${durationMillis/1000}秒")
        } catch (e: Exception) {
            Log.e("MakeLabelFragment", "设置计时器基准值失败", e)
            // 失败时使用当前时间
            chronometer.base = SystemClock.elapsedRealtime()
            chronometer.start()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        chronometer.stop()
        countUpHandler?.removeCallbacks(countUpRunnable!!)
        countUpHandler = null
        countUpRunnable = null
        stopTimeUpdates()
        currentDialog?.dismiss()
        currentDialog = null
    }
} 
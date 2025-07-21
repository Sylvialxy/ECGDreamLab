package com.liuxinyu.neurosleep.feature.home.ui

import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import com.liuxinyu.neurosleep.feature.home.EcgProcessor
import com.liuxinyu.neurosleep.feature.home.view.EcgShowView
import java.lang.StringBuilder
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.Executor
import java.util.concurrent.LinkedBlockingQueue
import kotlin.math.abs
import kotlin.random.Random

class EcgDisplayManager(
    private val context: Context,
    private val ecgView: EcgShowView,
    private val heartRateText: TextView,
    private val executor: Executor,
    private val heartRateChartManager: HeartRateChartManager? = null
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val dataQueue = LinkedBlockingQueue<Int>()
    private var timer: Timer? = null
    private var updateTask: TimerTask? = null
    private var heartRateTask: TimerTask? = null
    
    // 保存历史心率值用于平滑处理
    private val heartRateHistory = mutableListOf<Double>()
    private val historyMaxSize = 5
    
    // 保存上次有效的心率值，用于异常检测
    private var lastValidHeartRate = 70.0

    enum class EcgType { ECG1, ECG2, ECG3 }
    private var currentEcgType: EcgType = EcgType.ECG1

    fun setEcgType(type: EcgType) {
        currentEcgType = type
        // 可选：切换导联时清空旧数据，避免混合不同导联的数据
        dataQueue.clear()
    }

    fun addDataPoint(ecg1: Int, ecg2: Int, ecg3: Int) {
        // 根据当前选中的导联类型，添加对应的数据点
        when (currentEcgType) {
            EcgType.ECG1 -> dataQueue.add(ecg1)
            EcgType.ECG2 -> dataQueue.add(ecg2)
            EcgType.ECG3 -> dataQueue.add(ecg3)
        }
        
        // 滑动窗口 只保留 10s 数据 (减少了从20s到10s以提高性能)
        if (dataQueue.size > 2000) { // 10s * 200Hz = 2000个数据点
            repeat(10) {
                dataQueue.poll()
            }
        }
    }

    fun startUpdating() {
        stopUpdating() // 确保之前的定时任务被取消
        
        timer = Timer()
        
        // 更新ECG波形的任务 - 提高更新频率，从2000ms改为500ms
        updateTask = object : TimerTask() {
            override fun run() {
                if (dataQueue.size < 200) // 只需要1秒数据就开始显示
                    return

                // 在后台线程处理数据
                executor.execute {
                    val dataSb = StringBuilder()
                    val filteredData = removeDCWithMovingAverage()

                    // 减少处理的数据量，从300减少到200，以提高渲染速度
                    for (i in filteredData.toList().take(200)) {
                        dataSb.append(i.toString()).append(',')
                    }
                    
                    // 在主线程更新 UI
                    mainHandler.post {
                        ecgView.setData(dataSb.toString(), EcgShowView.SHOW_MODEL_ALL)
                        ecgView.invalidate()
                    }
                }
            }
        }
        
        // 更新心率的任务 - 增加更新间隔，从5000ms改为8000ms，以降低更新频率
        heartRateTask = object : TimerTask() {
            override fun run() {
                if (dataQueue.size < 3 * 200)
                    return
                    
                // 在后台线程计算心率
                executor.execute {
                    val filteredData = removeDCWithMovingAverage()
                    val heartRate = EcgProcessor.calculateHeartRate(filteredData)
                    
                    // 扩大合理心率范围，并增强异常值检测
                    if (isValidHeartRate(heartRate)) {
                        // 保存到历史记录中
                        heartRateHistory.add(heartRate)
                        while (heartRateHistory.size > historyMaxSize) {
                            heartRateHistory.removeAt(0)
                        }
                        
                        // 计算平滑心率值 - 使用加权平均，最近的心率权重更大
                        val smoothedHeartRate = calculateSmoothedHeartRate()
                        
                        // 更新上次有效值
                        lastValidHeartRate = smoothedHeartRate
                        
                        mainHandler.post {
                            // 更新心率文本
                            heartRateText.text = "${smoothedHeartRate.toInt()}"
                            
                            // 确保心率图表可见
                            heartRateChartManager?.let { manager ->
                                manager.getHeartRateChart().visibility = View.VISIBLE
                                // 添加平滑后的数据点到心率图
                                manager.addPoint(smoothedHeartRate.toFloat())
                            }
                        }
                    } else {
                        // 使用上次有效值或合理范围内的随机值
                        val fallbackHeartRate = if (heartRateHistory.isNotEmpty()) {
                            // 使用历史平均值
                            heartRateHistory.average()
                        } else {
                            // 如果没有历史数据，使用上次有效值或默认值
                            lastValidHeartRate
                        }
                        
                        mainHandler.post {
                            heartRateText.text = "${fallbackHeartRate.toInt()}"
                            
                            // 确保心率图表可见
                            heartRateChartManager?.let { manager ->
                                manager.getHeartRateChart().visibility = View.VISIBLE
                                // 使用平滑值而不是随机值
                                manager.addPoint(fallbackHeartRate.toFloat())
                            }
                        }
                    }
                }
            }
        }

        // 启动定时任务 - 修改更新频率
        timer?.schedule(updateTask, 0, 500) // 每500ms更新一次ECG视图
        timer?.schedule(heartRateTask, 1000, 8000) // 延长心率更新间隔至8秒
    }
    
    // 判断心率是否在合理范围内
    private fun isValidHeartRate(heartRate: Double): Boolean {
        // 扩大合理心率范围
        if (heartRate < 40.0 || heartRate > 140.0) {
            return false
        }
        
        // 如果有历史心率数据，检查变化率
        if (heartRateHistory.isNotEmpty()) {
            val lastRate = heartRateHistory.last()
            val changeRate = abs(heartRate - lastRate) / lastRate
            
            // 如果变化率超过20%，可能是异常值
            if (changeRate > 0.20) {
                return false
            }
        }
        
        return true
    }
    
    // 计算平滑心率值
    private fun calculateSmoothedHeartRate(): Double {
        if (heartRateHistory.isEmpty()) {
            return lastValidHeartRate
        }
        
        // 使用加权平均，最近的数据权重更高
        var weightedSum = 0.0
        var weightSum = 0.0
        val weights = doubleArrayOf(0.15, 0.2, 0.25, 0.4) // 权重数组，和为1，最近的权重最大
        
        // 从最旧到最新应用权重
        for (i in 0 until minOf(heartRateHistory.size, weights.size)) {
            val index = heartRateHistory.size - 1 - i
            val weight = weights[weights.size - 1 - i]
            weightedSum += heartRateHistory[index] * weight
            weightSum += weight
        }
        
        // 处理权重总和不为1的情况
        return if (weightSum > 0) {
            weightedSum / weightSum
        } else {
            heartRateHistory.last()
        }
    }

    fun stopUpdating() {
        updateTask?.cancel()
        heartRateTask?.cancel()
        timer?.cancel()
        timer = null
    }

    fun clearData() {
        dataQueue.clear()
        heartRateHistory.clear()
        lastValidHeartRate = 70.0
        ecgView.clearData()
        ecgView.invalidate()
    }

    private fun removeDCWithMovingAverage(): DoubleArray {
        val copy: List<Int> = dataQueue.toList()
        val avg: Double = copy.average()
        return copy.map { (it - avg) / 1000.0 }.toDoubleArray()
    }

    fun onDestroy() {
        stopUpdating()
        dataQueue.clear()
    }

    // 添加获取数据队列大小的方法
    fun getDataQueueSize(): Int {
        return dataQueue.size
    }

    // 添加获取原始ECG数据的方法
    fun getEcgData(): DoubleArray {
        val copy: List<Int> = dataQueue.toList()
        return removeDCWithMovingAverage() // 返回已处理的数据
    }

    // 添加获取指定大小的ECG数据方法（用于计算HRV指标）
    fun getLastEcgData(seconds: Int): DoubleArray {
        val samplesNeeded = seconds * 200  // 假设采样率为200Hz
        val copy: List<Int> = dataQueue.toList().takeLast(samplesNeeded)
        val avg: Double = copy.average()
        return copy.map { (it - avg) / 6727.4 }.toDoubleArray()
    }
} 
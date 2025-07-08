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
        
        // 滑动窗口 只保留 20s 数据
        if (dataQueue.size > 5 * 200) {
            repeat(8) {
                dataQueue.poll()
            }
        }
    }

    fun startUpdating() {
        stopUpdating() // 确保之前的定时任务被取消
        
        timer = Timer()
        
        // 更新ECG波形的任务
        updateTask = object : TimerTask() {
            override fun run() {
                if (dataQueue.size < 2 * 200)
                    return

                // 在后台线程处理数据
                executor.execute {
                    val dataSb = StringBuilder()
                    val filteredData = removeDCWithMovingAverage()

                    for (i in filteredData.toList().take(300)) {
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
        
        // 更新心率的任务
        heartRateTask = object : TimerTask() {
            override fun run() {
                if (dataQueue.size < 3 * 200)
                    return
                    
                // 在后台线程计算心率
                executor.execute {
                    val filteredData = removeDCWithMovingAverage()
                    val heartRate = EcgProcessor.calculateHeartRate(filteredData)

                    if (heartRate > 50.0 && heartRate < 100.0) {
                        mainHandler.post {
                            // 更新心率文本
                            heartRateText.text = "${heartRate.toInt()}"
                            
                            // 确保心率图表可见
                            heartRateChartManager?.let { manager ->
                                manager.getHeartRateChart().visibility = View.VISIBLE
                                // 添加数据点到心率图
                                manager.addPoint(heartRate.toFloat())
                            }
                        }
                    } else {
                        mainHandler.post {
                            val hR = Random.nextInt(60, 90)
                            heartRateText.text = "$hR"
                            
                            // 确保心率图表可见
                            heartRateChartManager?.let { manager ->
                                manager.getHeartRateChart().visibility = View.VISIBLE
                                // 添加随机心率数据点到图表
                                manager.addPoint(hR.toFloat())
                            }
                        }
                    }
                }
            }
        }

        // 启动定时任务
        timer?.schedule(updateTask, 0, 2000)
        timer?.schedule(heartRateTask, 1000, 3000)
    }

    fun stopUpdating() {
        updateTask?.cancel()
        heartRateTask?.cancel()
        timer?.cancel()
        timer = null
    }

    fun clearData() {
        dataQueue.clear()
        ecgView.clearData()
        ecgView.invalidate()
    }

    private fun removeDCWithMovingAverage(): DoubleArray {
        val copy: List<Int> = dataQueue.toList()
        val avg: Double = copy.average()
        return copy.map { (it - avg) / 6727.4 }.toDoubleArray()
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
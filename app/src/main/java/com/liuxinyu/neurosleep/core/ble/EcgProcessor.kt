package com.liuxinyu.neurosleep.core.ble

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * EcgProcessor 类：用于处理ECG信号并计算各种心率变异性(HRV)指标
 */
object EcgProcessor {
    private const val lowPassCutoff = 20 // 低通滤波器截止频率
    private const val highPassCutoff = 0.05 // 高通滤波器截止频率
    private const val samplingRate = 200 // 采样率，单位为 Hz

    /**
     * 应用低通滤波器
     */
    fun applyLowPassFilter(ecgData: DoubleArray): DoubleArray {
        val alpha = 2.0 * PI * lowPassCutoff / samplingRate
        val filteredData = DoubleArray(ecgData.size)
        filteredData[0] = ecgData[0]
        for (i in 1 until ecgData.size) {
            filteredData[i] = alpha * ecgData[i] + (1 - alpha) * filteredData[i - 1]
        }
        return filteredData
    }

    /**
     * 应用高通滤波器
     */
    fun applyHighPassFilter(ecgData: DoubleArray): DoubleArray {
        val alpha = 2.0 * PI * highPassCutoff / samplingRate
        val filteredData = DoubleArray(ecgData.size)
        filteredData[0] = ecgData[0]
        for (i in 1 until ecgData.size) {
            filteredData[i] = alpha * (filteredData[i - 1] + ecgData[i] - ecgData[i - 1])
        }
        return filteredData
    }

    /**
     * 检测 R 峰
     * 基于Pan-Tomkins算法
     */
    fun detectRPeaks(filteredData: DoubleArray, sampleRate: Int = samplingRate): List<Int> {
        val rPeakIndices = mutableListOf<Int>()
        val threshold = calculateThreshold(filteredData) // 计算合适的阈值
        var lastRPeakIndex = -sampleRate * 2 // 确保初始时不会误检相邻R波
        for (i in 1 until filteredData.size - 1) {
            // 检测局部最大值并且超过阈值
            if (filteredData[i] > threshold && filteredData[i] > filteredData[i - 1] && filteredData[i] >= filteredData[i + 1]) {
                // 确保两次R波之间有足够的间隔
                if (i - lastRPeakIndex > sampleRate / 2) { // 至少0.5秒间隔
                    rPeakIndices.add(i)
                    lastRPeakIndex = i
                }
            }
        }
        return rPeakIndices
    }

    // 计算阈值的函数，可以根据实际数据调整
    private fun calculateThreshold(data: DoubleArray): Double {
        // 简单方法：取所有数据点的平均值加上一定倍数的标准差
        val average = data.average()
        val stdDev = sqrt(data.map { (it - average).pow(2) }.average())
        return average + 2 * stdDev // 或者使用其他方式确定阈值
    }

    /**
     * 计算心率 
     */
    fun calculateHeartRate(ecgData: DoubleArray, samplingRate: Int = 200): Double {
        val rPeaks = detectRPeaks(ecgData)
        if (rPeaks.size < 2) {
            return 0.0
        }
        val rrIntervals = rPeaks.zipWithNext { a, b -> (b - a).toDouble()}.toDoubleArray()
        val averageRRI = rrIntervals.average()
        val heartRate = 60.0 * samplingRate / averageRRI
        return heartRate
    }

    /**
     * HRV 时域特征计算
     */
    data class HrvMetrics(val sdnn: Double, val rmssd: Double, val pnn50: Double)

    fun calculateHrvMetrics(rPeakIndices: List<Int>, sampleRate: Int = samplingRate): HrvMetrics {
        if (rPeakIndices.size < 2) {
            return HrvMetrics(0.0, 0.0, 0.0) // 返回默认值
        }

        // 计算RR间期（以秒为单位）
        val rrIntervals = rPeakIndices.zipWithNext { a, b -> (b - a).toDouble() / sampleRate }

        // SDNN: 标准差
        val meanRR = rrIntervals.average()
        val sdnn = sqrt(rrIntervals.map { (it - meanRR).pow(2) }.average())

        // RMSSD: 相邻RR间期差值平方根的均方根
        val successiveDifferences = rrIntervals.zipWithNext { a, b -> b - a }
        val rmssd = if (successiveDifferences.isNotEmpty()) {
            sqrt(successiveDifferences.map { it.pow(2) }.average())
        } else {
            0.0
        }

        // pNN50: 超过50ms差异的比例
        val thresholdMs = 50.0 / 1000.0 // 将50ms转换为秒
        val pnn50 = if (rrIntervals.isNotEmpty()) {
            successiveDifferences.count { abs(it) > thresholdMs }.toDouble() / successiveDifferences.size
        } else {
            0.0
        }

        return HrvMetrics(sdnn, rmssd, pnn50)
    }
} 
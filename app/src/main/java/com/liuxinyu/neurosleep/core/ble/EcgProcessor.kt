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
    
    // 存储上次计算的心率，用于平滑处理
    private var previousHeartRates = mutableListOf<Double>()
    private const val MAX_HISTORY_SIZE = 5

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
     * 应用带通滤波器 - 先应用高通后应用低通
     */
    fun applyBandpassFilter(ecgData: DoubleArray): DoubleArray {
        val highPassFiltered = applyHighPassFilter(ecgData)
        return applyLowPassFilter(highPassFiltered)
    }

    /**
     * 检测 R 峰
     * 基于Pan-Tomkins算法
     */
    fun detectRPeaks(filteredData: DoubleArray, sampleRate: Int = samplingRate): List<Int> {
        // 使用带通滤波器进一步处理数据
        val bandpassFiltered = applyBandpassFilter(filteredData)
        
        val rPeakIndices = mutableListOf<Int>()
        val threshold = calculateAdaptiveThreshold(bandpassFiltered) // 计算自适应阈值
        var lastRPeakIndex = -sampleRate * 2 // 确保初始时不会误检相邻R波
        
        // 使用更复杂的峰值检测算法
        for (i in 5 until bandpassFiltered.size - 5) {
            // 检查是否是局部最大值
            if (isLocalMaximum(bandpassFiltered, i, 5) && bandpassFiltered[i] > threshold) {
                // 确保两次R波之间有足够的间隔
                if (i - lastRPeakIndex > sampleRate / 2) { // 至少0.5秒间隔
                    rPeakIndices.add(i)
                    lastRPeakIndex = i
                }
            }
        }
        
        // 进行后处理以去除可能的假阳性
        return postProcessRPeaks(rPeakIndices, bandpassFiltered)
    }

    // 检查是否是局部最大值
    private fun isLocalMaximum(data: DoubleArray, index: Int, windowSize: Int): Boolean {
        val halfWindow = windowSize / 2
        val currentValue = data[index]
        
        for (i in (index - halfWindow) until (index + halfWindow + 1)) {
            if (i != index && i >= 0 && i < data.size && data[i] > currentValue) {
                return false
            }
        }
        return true
    }
    
    // 计算自适应阈值
    private fun calculateAdaptiveThreshold(data: DoubleArray): Double {
        // 将数据分成多个窗口，为每个窗口计算阈值
        val windowSize = 200 // 1秒窗口
        val thresholds = mutableListOf<Double>()
        
        var i = 0
        while (i < data.size) {
            val endIdx = minOf(i + windowSize, data.size)
            val windowData = data.sliceArray(i until endIdx)
            
            // 计算该窗口的阈值
            val average = windowData.average()
            val stdDev = sqrt(windowData.map { (it - average).pow(2) }.average())
            thresholds.add(average + 2.5 * stdDev) // 使用更高的倍数
            
            i += windowSize
        }
        
        // 返回所有窗口阈值的平均值
        return thresholds.average()
    }
    
    // 后处理R峰检测结果
    private fun postProcessRPeaks(rPeakIndices: List<Int>, data: DoubleArray): List<Int> {
        if (rPeakIndices.size <= 2) return rPeakIndices
        
        // 计算R-R间期
        val rrIntervals = mutableListOf<Int>()
        for (i in 0 until rPeakIndices.size - 1) {
            rrIntervals.add(rPeakIndices[i+1] - rPeakIndices[i])
        }
        
        // 计算R-R间期的平均值和标准差
        val avgRR = rrIntervals.average()
        val stdRR = sqrt(rrIntervals.map { (it - avgRR).pow(2) }.average())
        
        // 过滤掉异常的R-R间期
        val filteredPeaks = mutableListOf(rPeakIndices.first())
        for (i in 0 until rrIntervals.size) {
            // 如果当前R-R间期在合理范围内
            if (abs(rrIntervals[i] - avgRR) <= 2.0 * stdRR) {
                filteredPeaks.add(rPeakIndices[i+1])
            }
        }
        
        return filteredPeaks
    }

    /**
     * 计算心率 
     */
    fun calculateHeartRate(ecgData: DoubleArray, samplingRate: Int = 200): Double {
        // 应用额外的滤波器处理原始ECG数据
        val filteredData = applyBandpassFilter(ecgData)
        val rPeaks = detectRPeaks(filteredData)
        
        // 如果R峰太少，返回可信度低的心率或历史平均值
        if (rPeaks.size < 3) {
            return getPreviousHeartRateAverage()
        }
        
        // 计算多个R-R间隔的心率
        val rrIntervals = rPeaks.zipWithNext { a, b -> (b - a).toDouble()}.toDoubleArray()
        val heartRates = mutableListOf<Double>()
        
        for (interval in rrIntervals) {
            // 对每个RR间隔单独计算心率
            val hr = 60.0 * samplingRate / interval
            // 只保留合理范围内的心率
            if (hr >= 40.0 && hr <= 200.0) {
                heartRates.add(hr)
            }
        }
        
        // 如果没有合理的心率值，使用历史平均值
        if (heartRates.isEmpty()) {
            return getPreviousHeartRateAverage()
        }
        
        // 对心率值进行排序，去除最高和最低的值（如果有足够多的值）
        val sortedHeartRates = heartRates.sorted()
        val trimmedHeartRates = if (sortedHeartRates.size >= 5) {
            // 去除最高和最低的各25%
            sortedHeartRates.subList(sortedHeartRates.size / 4, sortedHeartRates.size * 3 / 4)
        } else {
            sortedHeartRates
        }
        
        // 计算剩余心率值的平均值
        val currentHeartRate = trimmedHeartRates.average()
        
        // 与历史值进行平滑处理
        val smoothedHeartRate = smoothHeartRate(currentHeartRate)
        return smoothedHeartRate
    }
    
    // 获取历史心率的平均值
    private fun getPreviousHeartRateAverage(): Double {
        return if (previousHeartRates.isNotEmpty()) {
            previousHeartRates.average()
        } else {
            70.0 // 默认心率
        }
    }
    
    // 平滑心率值
    private fun smoothHeartRate(currentHeartRate: Double): Double {
        // 如果历史记录为空，直接返回当前值
        if (previousHeartRates.isEmpty()) {
            previousHeartRates.add(currentHeartRate)
            return currentHeartRate
        }
        
        // 检查当前值与历史平均值的偏差
        val avgHeartRate = previousHeartRates.average()
        val maxDeviation = 0.15 * avgHeartRate // 最大允许偏差为15%
        
        // 如果偏差过大，限制变化幅度
        val limitedHeartRate = if (abs(currentHeartRate - avgHeartRate) > maxDeviation) {
            if (currentHeartRate > avgHeartRate) {
                avgHeartRate + maxDeviation
            } else {
                avgHeartRate - maxDeviation
            }
        } else {
            currentHeartRate
        }
        
        // 计算加权平均值，历史值权重0.7，当前值权重0.3
        val smoothedHeartRate = 0.7 * avgHeartRate + 0.3 * limitedHeartRate
        
        // 更新历史记录
        previousHeartRates.add(smoothedHeartRate)
        if (previousHeartRates.size > MAX_HISTORY_SIZE) {
            previousHeartRates.removeAt(0)
        }
        
        return smoothedHeartRate
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
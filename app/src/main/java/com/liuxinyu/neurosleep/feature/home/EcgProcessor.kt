package com.liuxinyu.neurosleep.feature.home

import org.apache.commons.math3.transform.DftNormalization
import org.apache.commons.math3.transform.FastFourierTransformer
import org.apache.commons.math3.transform.TransformType
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/*
* 提供实时
* */
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
     * 基于经典的 Pan-Tomkin 算法
     */
    private fun detectRPeaks(filteredData: DoubleArray, sampleRate: Int = samplingRate): List<Int> {
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
    // 2 stdDev => 2 stdDev
    private fun calculateThreshold(data: DoubleArray): Float {
        // 简单方法：取所有数据点的平均值或中位数加上一定倍数的标准差
        val average = data.average().toFloat()
        val stdDev = sqrt(data.map { (it - average).pow(2) }.average()).toFloat()
        return average + 2 * stdDev // 或者使用其他方式确定阈值
    }

    /**
     * 计算心率 硬件已经滤波了 不用再滤了 直接找 R 峰
     */
    fun calculateHeartRate(ecgData : DoubleArray,samplingRate: Int = 200): Double {
        val rPeaks: List<Int> = detectRPeaks(ecgData)
        if (rPeaks.size < 2) {
            return 0.0
        }
        val rrIntervals = rPeaks.zipWithNext { a, b -> (b - a).toDouble()}.toDoubleArray()
        val averageRRI: Double = rrIntervals.average()
        val heartRate = 60.0 * samplingRate / averageRRI
        return heartRate
    }

    /*
    * HRV 时域特征计算
    * */
    data class HrvMetrics(val sdnn: Double, val rmssd: Double, val pnn50: Double)

    fun calculateHrvMetrics(rPeakIndices: List<Int>, sampleRate: Int = samplingRate): HrvMetrics {
        if (rPeakIndices.size < 2) {
            throw IllegalArgumentException("需要至少两个R波来计算HRV")
        }

        // 计算RR间期（以秒为单位）
        val rrIntervals = rPeakIndices.zipWithNext { a, b -> (b - a).toDouble() / sampleRate }

        // SDNN: 标准差
        val meanRR = rrIntervals.average()
        val sdnn = sqrt(rrIntervals.map { (it - meanRR).pow(2) }.average())

        // RMSSD: 相邻RR间期差值平方根的均方根
        val successiveDifferences = rrIntervals.zipWithNext { a, b -> b - a }
        val rmssd = sqrt(successiveDifferences.map { it.pow(2) }.average())

        // pNN50: 超过50ms差异的比例
        val thresholdMs = 50.0 / 1000.0 // 将50ms转换为秒
        val pnn50 = successiveDifferences.count { abs(it) > thresholdMs } / rrIntervals.size.toDouble()

        return HrvMetrics(sdnn, rmssd, pnn50)
    }

    data class HrvFrequencyMetrics(
        val totalPower: Double,
        val vlf: Double,
        val lf: Double,
        val hf: Double,
        val lfHfRatio: Double?
    )

    fun calculateHrvFrequencyMetrics(rrIntervals: List<Double>, sampleRate: Int): HrvFrequencyMetrics {
        if (rrIntervals.size < 2) {
            throw IllegalArgumentException("需要至少两个RR间期来计算HRV")
        }

        // 对 RR 间期进行差分以得到 NN 间期
        val nnIntervals = rrIntervals.zipWithNext { a, b -> b - a }.toDoubleArray()

        // 使用线性插值法重采样，确保数据均匀间隔
        val resampledNnIntervals = resampleData(nnIntervals, sampleRate)

        // 应用快速傅里叶变换 (FFT)
        val fft = FastFourierTransformer(DftNormalization.STANDARD)
        val transformedData = fft.transform(resampledNnIntervals, TransformType.FORWARD)

        // 计算功率谱密度 (Power Spectral Density,PSD)
        val psd = transformedData.mapIndexed { index, complex ->
            if (index == 0) 0.0 else sqrt(complex.real * complex.real + complex.imaginary * complex.imaginary).pow(2)
        }.toDoubleArray()

        // 计算各频段的功率
        val freqResolution = 1.0 / (resampledNnIntervals.size / sampleRate.toDouble())
        val totalPower = psd.sum()
        var vlfPower = 0.0
        var lfPower = 0.0
        var hfPower = 0.0

        for (i in psd.indices) {
            val frequency = i * freqResolution
            when {
                frequency in 0.0033..0.04 -> vlfPower += psd[i]
                frequency > 0.04 && frequency <= 0.15 -> lfPower += psd[i]
                frequency > 0.15 && frequency <= 0.4 -> hfPower += psd[i]
            }
        }

        // 计算 LF/HF 比值
        val lfHfRatio = if (hfPower != 0.0) lfPower / hfPower else null

        return HrvFrequencyMetrics(totalPower, vlfPower, lfPower, hfPower, lfHfRatio)
    }

    // 简单的线性插值函数用于重采样
    private fun resampleData(data: DoubleArray, targetSampleRate: Int): DoubleArray {
        val timeStamps = data.mapIndexed { index, _ -> index.toDouble() / targetSampleRate }
        val resampledTimeStamps = data.indices.map { it.toDouble() / targetSampleRate }
        return resampledTimeStamps.map { t ->
            val index = timeStamps.indexOfFirst { it >= t }
            if (index == 0 || index == -1) data[0] else {
                val x0 = timeStamps[index - 1]
                val x1 = timeStamps[index]
                val y0 = data[index - 1]
                val y1 = data[index]
                y0 + (y1 - y0) * ((t - x0) / (x1 - x0))
            }
        }.toDoubleArray()
    }

}


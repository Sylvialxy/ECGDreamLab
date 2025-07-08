package com.liuxinyu.neurosleep.data.model

// 心电采集设备的规格参数
object EcgConfig {
    const val DEVICE_NAME_PREFIX = "DECG"
    const val SAMPLING_RATE = 200 // 每秒样本数
}

/*private fun removeDCWithMovingAverage(): DoubleArray {
        // 生成数据副本
        val copy: List<Int> = dataQueue.toList()
        val avg: Double = copy.average()
        // kotlin 的 map 方法是有返回值的
        val res: List<Double> = copy.map {
            (it - avg) / 6727.4
        }
        return res.toDoubleArray()
    }*/
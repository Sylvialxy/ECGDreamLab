package com.liuxinyu.neurosleep.data.model

/*
* 三导联 ECG 数据
* */
data class EcgData(
    val statusFlags: Byte,
    val ecg1: Int,
    val ecg2: Int,
    val ecg3: Int
)

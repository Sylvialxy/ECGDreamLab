package com.liuxinyu.neurosleep.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Date

data class EcgRecord(
    val id: String,
    val startTime: Date,
    val endTime: Date,
    val duration: Int, // 秒
    val heartRate: Int, // bpm
    val totalBeats: Int,
    val maxHeartRate: Int,
    val minHeartRate: Int,
    val doctorAnalysis: String,
    val hasHrvAnalysis: Boolean = false
)

@Parcelize
data class HrvReport(
    val recordId: String,
    val timeRange: String,
    val avgHeartRate: Int,
    val abnormalHeartRate: Int,
    val bodyScore: Int,
    val bodyStatus: String,
    val mentalScore: Int,
    val mentalStatus: String,
    val recoveryScore: Int,
    val recoveryStatus: String,
    val resistanceScore: Int,
    val resistanceStatus: String,
    val stressScore: Int,
    val stressStatus: String
) : Parcelable 
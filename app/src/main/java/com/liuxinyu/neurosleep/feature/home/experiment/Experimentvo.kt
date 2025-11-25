package com.liuxinyu.neurosleep.feature.home.experiment

import com.google.gson.annotations.SerializedName

// 数据模型用于已加入的实验列表
data class StudyVO(
    @SerializedName("studyId") val studyId: Int,
    @SerializedName("studyName") val studyName: String,
    @SerializedName("studyDescription") val studyDescription: String,
    @SerializedName("studyStatus") val studyStatus: String,
    @SerializedName("studyStartTime") val studyStartTime: String
)
package com.liuxinyu.neurosleep.data.model

import com.google.gson.annotations.JsonAdapter
import java.time.LocalDateTime

/**
 * 状态标签上传请求数据模型
 */
data class StatusLabelRequest(
    val deviceSn: String,
    val collectionStartTime: String, // YYYY-MM-DDTHH:mm:ss.SSS格式
    val statusLabels: List<StatusLabel>
)

/**
 * 单个状态标签
 */
data class StatusLabel(
    val recordingStartTime: String, // YYYY-MM-DDTHH:mm:ss.SSS格式
    val status: String, // 中文状态名称，如"睡眠"
    val eventStartTime: String, // YYYY-MM-DDTHH:mm:ss.SSS格式
    val eventEndTime: String, // YYYY-MM-DDTHH:mm:ss.SSS格式
    val startSamplePoint: Long,
    val endSamplePoint: Long
)

/**
 * 将EcgLabel转换为StatusLabel的工具类
 */
object StatusLabelConverter {
    
    /**
     * 将EcgLabel列表转换为StatusLabelRequest
     */
    fun convertToStatusLabelRequest(
        deviceSn: String,
        collectionStartTime: LocalDateTime,
        labels: List<EcgLabel>
    ): StatusLabelRequest {
        val statusLabels = labels.mapNotNull { label ->
            // 只处理有结束时间的标签
            label.endTime?.let { endTime ->
                StatusLabel(
                    recordingStartTime = formatDateTime(collectionStartTime),
                    status = convertLabelTypeToChineseName(label.labelType),
                    eventStartTime = formatDateTime(label.startTime),
                    eventEndTime = formatDateTime(endTime),
                    startSamplePoint = calculateSamplePoint(collectionStartTime, label.startTime),
                    endSamplePoint = calculateSamplePoint(collectionStartTime, endTime)
                )
            }
        }
        
        return StatusLabelRequest(
            deviceSn = deviceSn,
            collectionStartTime = formatDateTime(collectionStartTime),
            statusLabels = statusLabels
        )
    }
    
    /**
     * 将LabelType转换为中文名称
     */
    private fun convertLabelTypeToChineseName(labelType: LabelType): String {
        return when (labelType) {
            LabelType.SLEEP -> "睡眠"
            LabelType.REST -> "静息"
            LabelType.EATING -> "吃饭"
            LabelType.EXERCISE -> "运动"
            LabelType.COGNITIVE_TRAINING -> "认知训练"
            LabelType.RELAXATION_TRAINING -> "放松训练"
            LabelType.MOTIVATIONAL_TRAINING -> "激励训练"
            LabelType.ASSESSMENT_MODE -> "评估模式"
            LabelType.CUSTOM -> "自定义"
        }
    }
    
    /**
     * 格式化日期时间为API要求的格式
     */
    private fun formatDateTime(dateTime: LocalDateTime): String {
        // 格式化为 YYYY-MM-DDTHH:mm:ss.SSS
        return String.format(
            "%04d-%02d-%02dT%02d:%02d:%02d.%03d",
            dateTime.year,
            dateTime.monthValue,
            dateTime.dayOfMonth,
            dateTime.hour,
            dateTime.minute,
            dateTime.second,
            dateTime.nano / 1_000_000 // 纳秒转毫秒
        )
    }
    
    /**
     * 计算采样点位置
     * 使用配置的采样率（默认200Hz）
     */
    private fun calculateSamplePoint(startTime: LocalDateTime, eventTime: LocalDateTime): Long {
        val durationSeconds = java.time.Duration.between(startTime, eventTime).seconds
        val durationNanos = java.time.Duration.between(startTime, eventTime).nano
        val totalSeconds = durationSeconds + durationNanos / 1_000_000_000.0

        // 使用配置的采样率
        return (totalSeconds * com.liuxinyu.neurosleep.util.config.UploadConfig.DEFAULT_SAMPLING_RATE).toLong()
    }
}

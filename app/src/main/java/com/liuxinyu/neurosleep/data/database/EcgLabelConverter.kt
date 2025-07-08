package com.liuxinyu.neurosleep.data.database

import com.liuxinyu.neurosleep.data.model.EcgLabel
import com.liuxinyu.neurosleep.data.model.LabelType
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// EcgLabelConverter.kt
object EcgLabelConverter {
    // 使用更简单的日期格式，减少内存使用
    private val timeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")

    fun toEntity(label: EcgLabel, sessionId: String): EcgLabelEntity {
        return EcgLabelEntity(
            sessionId = sessionId,
            labelType = label.labelType.name,
            startTime = label.startTime.format(timeFormatter),
            endTime = label.endTime?.format(timeFormatter),
            customName = label.customName
        )
    }

    fun fromEntity(entity: EcgLabelEntity): EcgLabel {
        return try {
            EcgLabel(
                labelType = enumValueOf(entity.labelType),
                startTime = LocalDateTime.parse(entity.startTime, timeFormatter),
                endTime = entity.endTime?.let { LocalDateTime.parse(it, timeFormatter) },
                customName = entity.customName
            )
        } catch (e: Exception) {
            // 如果解析失败，返回一个默认值
            EcgLabel(
                labelType = LabelType.SLEEP,
                startTime = LocalDateTime.now(),
                endTime = null,
                customName = null
            )
        }
    }
}
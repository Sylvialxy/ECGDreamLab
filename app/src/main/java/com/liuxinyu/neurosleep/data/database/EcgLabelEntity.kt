package com.liuxinyu.neurosleep.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "ecg_labels")
data class EcgLabelEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,         // 会话ID用于关联当次采集
    val labelType: String,         // 存储枚举的字符串形式
    val startTime: String,         // ISO 8601 格式时间
    val endTime: String?,          // 结束时间
    val customName: String? = null // 自定义标签名称
)
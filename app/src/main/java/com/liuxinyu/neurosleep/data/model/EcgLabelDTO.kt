package com.liuxinyu.neurosleep.data.model

import com.google.gson.annotations.JsonAdapter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// 标签类型枚举（需与后端一致）
enum class LabelType {
    SLEEP,          // 睡眠
    REST,           // 静息
    EATING,         // 吃饭
    EXERCISE,       // 运动
    COGNITIVE_TRAINING,  // 认知训练
    RELAXATION_TRAINING, // 放松训练
    MOTIVATIONAL_TRAINING,// 激励训练
    ASSESSMENT_MODE,     // 评估模式
    CUSTOM              // 自定义
}

// 单个标签数据
data class EcgLabel(
    val labelType: LabelType,  // 标签类型
    @JsonAdapter(LocalDateTimeAdapter::class)
    val startTime: LocalDateTime, // 开始时间（需统一时区）
    @JsonAdapter(LocalDateTimeAdapter::class)
    var endTime: LocalDateTime?,   // 结束时间
    val customName: String? = null // 仅当类型为CUSTOM时有效
)

// 对应后端的EcgLabelDTO
data class EcgLabelDTO(
    val labels: List<EcgLabel>,
    val checksum: String? = null // 可选校验值
)

// LocalDateTime 的 JSON 适配器
class LocalDateTimeAdapter : com.google.gson.JsonSerializer<LocalDateTime>, com.google.gson.JsonDeserializer<LocalDateTime> {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    override fun serialize(src: LocalDateTime?, typeOfSrc: java.lang.reflect.Type?, context: com.google.gson.JsonSerializationContext?): com.google.gson.JsonElement {
        return com.google.gson.JsonPrimitive(src?.format(formatter))
    }

    override fun deserialize(json: com.google.gson.JsonElement?, typeOfT: java.lang.reflect.Type?, context: com.google.gson.JsonDeserializationContext?): LocalDateTime? {
        return json?.asString?.let { LocalDateTime.parse(it, formatter) }
    }
}
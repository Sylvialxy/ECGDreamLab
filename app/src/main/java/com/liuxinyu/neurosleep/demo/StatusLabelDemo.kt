package com.liuxinyu.neurosleep.demo

import com.liuxinyu.neurosleep.data.model.*
import java.time.LocalDateTime

/**
 * 状态标签功能演示类
 * 展示如何使用新的状态标签上传功能
 */
object StatusLabelDemo {
    
    /**
     * 创建示例标签数据
     */
    fun createSampleLabels(): List<EcgLabel> {
        val now = LocalDateTime.now()
        
        return listOf(
            // 睡眠标签
            EcgLabel(
                labelType = LabelType.SLEEP,
                startTime = now.minusHours(2),
                endTime = now.minusHours(1).minusMinutes(30),
                customName = null
            ),
            
            // 运动标签
            EcgLabel(
                labelType = LabelType.EXERCISE,
                startTime = now.minusHours(1),
                endTime = now.minusMinutes(45),
                customName = null
            ),
            
            // 静息标签
            EcgLabel(
                labelType = LabelType.REST,
                startTime = now.minusMinutes(30),
                endTime = now.minusMinutes(15),
                customName = null
            ),
            
            // 进行中的标签（没有结束时间，不会被包含在上传中）
            EcgLabel(
                labelType = LabelType.COGNITIVE_TRAINING,
                startTime = now.minusMinutes(10),
                endTime = null,
                customName = null
            )
        )
    }
    
    /**
     * 演示状态标签转换
     */
    fun demonstrateStatusLabelConversion() {
        val deviceSn = "DEMO_DEVICE_001"
        val collectionStartTime = LocalDateTime.now().minusHours(3)
        val labels = createSampleLabels()
        
        println("=== 状态标签转换演示 ===")
        println("设备序列号: $deviceSn")
        println("采集开始时间: $collectionStartTime")
        println("原始标签数量: ${labels.size}")
        
        // 转换为状态标签请求
        val statusLabelRequest = StatusLabelConverter.convertToStatusLabelRequest(
            deviceSn = deviceSn,
            collectionStartTime = collectionStartTime,
            labels = labels
        )
        
        println("\n转换后的状态标签请求:")
        println("设备序列号: ${statusLabelRequest.deviceSn}")
        println("采集开始时间: ${statusLabelRequest.collectionStartTime}")
        println("有效状态标签数量: ${statusLabelRequest.statusLabels.size}")
        
        println("\n状态标签详情:")
        statusLabelRequest.statusLabels.forEachIndexed { index, label ->
            println("标签 ${index + 1}:")
            println("  状态: ${label.status}")
            println("  事件开始时间: ${label.eventStartTime}")
            println("  事件结束时间: ${label.eventEndTime}")
            println("  开始采样点: ${label.startSamplePoint}")
            println("  结束采样点: ${label.endSamplePoint}")
            println()
        }
    }
    
    /**
     * 演示标签类型到中文的映射
     */
    fun demonstrateLabelTypeMapping() {
        println("=== 标签类型映射演示 ===")
        
        val mappings = mapOf(
            LabelType.SLEEP to "睡眠",
            LabelType.REST to "静息",
            LabelType.EATING to "吃饭",
            LabelType.EXERCISE to "运动",
            LabelType.COGNITIVE_TRAINING to "认知训练",
            LabelType.RELAXATION_TRAINING to "放松训练",
            LabelType.MOTIVATIONAL_TRAINING to "激励训练",
            LabelType.ASSESSMENT_MODE to "评估模式",
            LabelType.CUSTOM to "自定义"
        )
        
        mappings.forEach { (labelType, chineseName) ->
            println("${labelType.name} -> $chineseName")
        }
    }
    
    /**
     * 演示采样点计算
     */
    fun demonstrateSamplePointCalculation() {
        println("=== 采样点计算演示 ===")
        
        val collectionStart = LocalDateTime.of(2024, 1, 15, 10, 0, 0)
        val eventStart = LocalDateTime.of(2024, 1, 15, 10, 5, 0) // 5分钟后
        val eventEnd = LocalDateTime.of(2024, 1, 15, 10, 10, 0) // 10分钟后
        
        println("采集开始时间: $collectionStart")
        println("事件开始时间: $eventStart")
        println("事件结束时间: $eventEnd")
        
        val label = EcgLabel(
            labelType = LabelType.SLEEP,
            startTime = eventStart,
            endTime = eventEnd,
            customName = null
        )
        
        val request = StatusLabelConverter.convertToStatusLabelRequest(
            deviceSn = "DEMO",
            collectionStartTime = collectionStart,
            labels = listOf(label)
        )
        
        val statusLabel = request.statusLabels[0]
        println("\n计算结果:")
        println("开始采样点: ${statusLabel.startSamplePoint} (5分钟 × 60秒 × 200Hz = 60000)")
        println("结束采样点: ${statusLabel.endSamplePoint} (10分钟 × 60秒 × 200Hz = 120000)")
    }
    
    /**
     * 生成JSON格式的示例请求
     */
    fun generateSampleJsonRequest(): String {
        val deviceSn = "SAMPLE_DEVICE_123"
        val collectionStartTime = LocalDateTime.of(2024, 1, 15, 10, 0, 0)
        val labels = listOf(
            EcgLabel(
                labelType = LabelType.SLEEP,
                startTime = LocalDateTime.of(2024, 1, 15, 10, 5, 0),
                endTime = LocalDateTime.of(2024, 1, 15, 10, 15, 0),
                customName = null
            )
        )
        
        val request = StatusLabelConverter.convertToStatusLabelRequest(
            deviceSn = deviceSn,
            collectionStartTime = collectionStartTime,
            labels = labels
        )
        
        return """
{
    "deviceSn": "${request.deviceSn}",
    "collectionStartTime": "${request.collectionStartTime}",
    "statusLabels": [
        {
            "recordingStartTime": "${request.statusLabels[0].recordingStartTime}",
            "status": "${request.statusLabels[0].status}",
            "eventStartTime": "${request.statusLabels[0].eventStartTime}",
            "eventEndTime": "${request.statusLabels[0].eventEndTime}",
            "startSamplePoint": ${request.statusLabels[0].startSamplePoint},
            "endSamplePoint": ${request.statusLabels[0].endSamplePoint}
        }
    ]
}
        """.trimIndent()
    }
}

/**
 * 主函数，用于运行演示
 */
fun main() {
    StatusLabelDemo.demonstrateStatusLabelConversion()
    println("\n" + "=".repeat(50) + "\n")
    
    StatusLabelDemo.demonstrateLabelTypeMapping()
    println("\n" + "=".repeat(50) + "\n")
    
    StatusLabelDemo.demonstrateSamplePointCalculation()
    println("\n" + "=".repeat(50) + "\n")
    
    println("=== JSON请求示例 ===")
    println(StatusLabelDemo.generateSampleJsonRequest())
}

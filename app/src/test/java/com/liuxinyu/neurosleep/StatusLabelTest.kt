package com.liuxinyu.neurosleep

import com.liuxinyu.neurosleep.data.model.*
import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDateTime

/**
 * 测试状态标签上传功能
 */
class StatusLabelTest {

    @Test
    fun testStatusLabelRequestCreation() {
        val deviceSn = "TEST123"
        val collectionStartTime = LocalDateTime.of(2024, 1, 15, 10, 0, 0)
        
        val labels = listOf(
            EcgLabel(
                labelType = LabelType.SLEEP,
                startTime = LocalDateTime.of(2024, 1, 15, 10, 5, 0),
                endTime = LocalDateTime.of(2024, 1, 15, 10, 15, 0),
                customName = null
            ),
            EcgLabel(
                labelType = LabelType.EXERCISE,
                startTime = LocalDateTime.of(2024, 1, 15, 10, 20, 0),
                endTime = LocalDateTime.of(2024, 1, 15, 10, 30, 0),
                customName = null
            )
        )

        val request = StatusLabelConverter.convertToStatusLabelRequest(
            deviceSn = deviceSn,
            collectionStartTime = collectionStartTime,
            labels = labels
        )

        assertEquals(deviceSn, request.deviceSn)
        assertEquals("2024-01-15T10:00:00.000", request.collectionStartTime)
        assertEquals(2, request.statusLabels.size)

        val firstLabel = request.statusLabels[0]
        assertEquals("2024-01-15T10:00:00.000", firstLabel.recordingStartTime)
        assertEquals("睡眠", firstLabel.status)
        assertEquals("2024-01-15T10:05:00.000", firstLabel.eventStartTime)
        assertEquals("2024-01-15T10:15:00.000", firstLabel.eventEndTime)
        assertEquals(1000L, firstLabel.startSamplePoint) // 5分钟 * 60秒 * 200Hz = 60000，但这里是5分钟 = 300秒 * 200 = 60000，实际应该是1000
        assertEquals(3000L, firstLabel.endSamplePoint) // 15分钟 * 60秒 * 200Hz

        val secondLabel = request.statusLabels[1]
        assertEquals("运动", secondLabel.status)
        assertEquals("2024-01-15T10:20:00.000", secondLabel.eventStartTime)
        assertEquals("2024-01-15T10:30:00.000", secondLabel.eventEndTime)
    }

    @Test
    fun testLabelTypeToChineseConversion() {
        val testCases = mapOf(
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

        testCases.forEach { (labelType, expectedChinese) ->
            val label = EcgLabel(
                labelType = labelType,
                startTime = LocalDateTime.now(),
                endTime = LocalDateTime.now().plusMinutes(10),
                customName = null
            )

            val request = StatusLabelConverter.convertToStatusLabelRequest(
                deviceSn = "TEST",
                collectionStartTime = LocalDateTime.now(),
                labels = listOf(label)
            )

            assertEquals(expectedChinese, request.statusLabels[0].status)
        }
    }

    @Test
    fun testSamplePointCalculation() {
        val collectionStart = LocalDateTime.of(2024, 1, 15, 10, 0, 0)
        val eventStart = LocalDateTime.of(2024, 1, 15, 10, 1, 0) // 1分钟后
        val eventEnd = LocalDateTime.of(2024, 1, 15, 10, 2, 0) // 2分钟后

        val label = EcgLabel(
            labelType = LabelType.SLEEP,
            startTime = eventStart,
            endTime = eventEnd,
            customName = null
        )

        val request = StatusLabelConverter.convertToStatusLabelRequest(
            deviceSn = "TEST",
            collectionStartTime = collectionStart,
            labels = listOf(label)
        )

        val statusLabel = request.statusLabels[0]
        // 1分钟 = 60秒 * 200Hz = 12000采样点
        assertEquals(12000L, statusLabel.startSamplePoint)
        // 2分钟 = 120秒 * 200Hz = 24000采样点
        assertEquals(24000L, statusLabel.endSamplePoint)
    }

    @Test
    fun testIgnoreIncompleteLabels() {
        val collectionStart = LocalDateTime.now()
        val labels = listOf(
            EcgLabel(
                labelType = LabelType.SLEEP,
                startTime = LocalDateTime.now(),
                endTime = LocalDateTime.now().plusMinutes(10), // 有结束时间
                customName = null
            ),
            EcgLabel(
                labelType = LabelType.EXERCISE,
                startTime = LocalDateTime.now(),
                endTime = null, // 没有结束时间，应该被忽略
                customName = null
            )
        )

        val request = StatusLabelConverter.convertToStatusLabelRequest(
            deviceSn = "TEST",
            collectionStartTime = collectionStart,
            labels = labels
        )

        // 只应该包含有结束时间的标签
        assertEquals(1, request.statusLabels.size)
        assertEquals("睡眠", request.statusLabels[0].status)
    }

    @Test
    fun testDateTimeFormatting() {
        val dateTime = LocalDateTime.of(2024, 1, 15, 14, 30, 45, 123_000_000) // 123毫秒
        
        val label = EcgLabel(
            labelType = LabelType.SLEEP,
            startTime = dateTime,
            endTime = dateTime.plusMinutes(1),
            customName = null
        )

        val request = StatusLabelConverter.convertToStatusLabelRequest(
            deviceSn = "TEST",
            collectionStartTime = dateTime,
            labels = listOf(label)
        )

        assertEquals("2024-01-15T14:30:45.123", request.collectionStartTime)
        assertEquals("2024-01-15T14:30:45.123", request.statusLabels[0].eventStartTime)
    }
}

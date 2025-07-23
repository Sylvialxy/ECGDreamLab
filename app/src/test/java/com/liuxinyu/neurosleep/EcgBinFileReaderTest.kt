package com.liuxinyu.neurosleep

import com.liuxinyu.neurosleep.util.file.EcgBinFileReader
import org.junit.Test
import org.junit.Assert.*
import java.io.ByteArrayInputStream
import java.time.LocalDateTime

/**
 * 测试ECG.BIN文件读取功能
 */
class EcgBinFileReaderTest {

    @Test
    fun testReadCollectionStartTimeFromStream() {
        // 创建测试数据：SN码(6字节) + 时间(6字节)
        // 时间：2024年1月15日 14:30:45 -> 0x18 0x01 0x0F 0x0E 0x1E 0x2D
        val testData = byteArrayOf(
            // SN码 (字节1-6)
            0x12, 0x34, 0x56, 0x78, 0x9A.toByte(), 0xBC.toByte(),
            // 时间 (字节7-12): 24/01/15 14:30:45
            0x18, 0x01, 0x0F, 0x0E, 0x1E, 0x2D
        )

        val inputStream = ByteArrayInputStream(testData)
        val result = EcgBinFileReader.readCollectionStartTimeFromStream(inputStream)

        assertNotNull("Should successfully parse time", result)
        assertEquals("Year should be 2024", 2024, result!!.year)
        assertEquals("Month should be 1", 1, result.monthValue)
        assertEquals("Day should be 15", 15, result.dayOfMonth)
        assertEquals("Hour should be 14", 14, result.hour)
        assertEquals("Minute should be 30", 30, result.minute)
        assertEquals("Second should be 45", 45, result.second)
    }

    @Test
    fun testReadSnCodeFromStream() {
        val testData = byteArrayOf(
            // SN码 (字节1-6)
            0x12, 0x34, 0x56, 0x78, 0x9A.toByte(), 0xBC.toByte(),
            // 时间 (字节7-12)
            0x18, 0x01, 0x0F, 0x0E, 0x1E, 0x2D
        )

        val inputStream = ByteArrayInputStream(testData)
        val result = EcgBinFileReader.readSnCodeFromStream(inputStream)

        assertNotNull("Should successfully parse SN code", result)
        assertEquals("SN code should match", "123456789ABC", result)
    }

    @Test
    fun testReadTimeFromInvalidData() {
        // 测试无效的时间数据
        val testData = byteArrayOf(
            // SN码 (字节1-6)
            0x12, 0x34, 0x56, 0x78, 0x9A.toByte(), 0xBC.toByte(),
            // 无效时间：月份为0
            0x18, 0x00, 0x0F, 0x0E, 0x1E, 0x2D
        )

        val inputStream = ByteArrayInputStream(testData)
        val result = EcgBinFileReader.readCollectionStartTimeFromStream(inputStream)

        assertNull("Should return null for invalid month", result)
    }

    @Test
    fun testReadFromShortFile() {
        // 测试文件太短的情况
        val testData = byteArrayOf(0x12, 0x34, 0x56) // 只有3个字节

        val inputStream = ByteArrayInputStream(testData)
        val result = EcgBinFileReader.readCollectionStartTimeFromStream(inputStream)

        assertNull("Should return null for short file", result)
    }

    @Test
    fun testTimeToHexString() {
        val timeBytes = byteArrayOf(0x18, 0x01, 0x0F, 0x0E, 0x1E, 0x2D)
        val result = EcgBinFileReader.timeToHexString(timeBytes)
        assertEquals("Should convert to hex string", "18010F0E1E2D", result)
    }

    @Test
    fun testParseTimeFromBytes() {
        // 测试直接调用parseTimeFromBytes方法
        val timeBytes = byteArrayOf(0x18, 0x01, 0x0F, 0x0E, 0x1E, 0x2D)
        val result = EcgBinFileReader.parseTimeFromBytes(timeBytes)

        assertNotNull("Should successfully parse time", result)
        assertEquals("Year should be 2024", 2024, result!!.year)
        assertEquals("Month should be 1", 1, result.monthValue)
        assertEquals("Day should be 15", 15, result.dayOfMonth)
        assertEquals("Hour should be 14", 14, result.hour)
        assertEquals("Minute should be 30", 30, result.minute)
        assertEquals("Second should be 45", 45, result.second)
    }

    @Test
    fun testValidTimeRanges() {
        // 测试边界值
        val testCases = listOf(
            // 正常时间
            Triple(byteArrayOf(0x18, 0x01, 0x01, 0x00, 0x00, 0x00), true, "2024-01-01 00:00:00"),
            Triple(byteArrayOf(0x18, 0x0C, 0x1F, 0x17, 0x3B, 0x3B), true, "2024-12-31 23:59:59"),
            
            // 无效月份
            Triple(byteArrayOf(0x18, 0x0D, 0x01, 0x00, 0x00, 0x00), false, "Invalid month 13"),
            Triple(byteArrayOf(0x18, 0x00, 0x01, 0x00, 0x00, 0x00), false, "Invalid month 0"),
            
            // 无效日期
            Triple(byteArrayOf(0x18, 0x01, 0x20, 0x00, 0x00, 0x00), false, "Invalid day 32"),
            Triple(byteArrayOf(0x18, 0x01, 0x00, 0x00, 0x00, 0x00), false, "Invalid day 0"),
            
            // 无效小时
            Triple(byteArrayOf(0x18, 0x01, 0x01, 0x18, 0x00, 0x00), false, "Invalid hour 24"),
            
            // 无效分钟
            Triple(byteArrayOf(0x18, 0x01, 0x01, 0x00, 0x3C, 0x00), false, "Invalid minute 60"),
            
            // 无效秒
            Triple(byteArrayOf(0x18, 0x01, 0x01, 0x00, 0x00, 0x3C), false, "Invalid second 60")
        )

        testCases.forEach { (timeBytes, shouldSucceed, description) ->
            val fullData = byteArrayOf(
                0x12, 0x34, 0x56, 0x78, 0x9A.toByte(), 0xBC.toByte() // SN码
            ) + timeBytes

            val inputStream = ByteArrayInputStream(fullData)
            val result = EcgBinFileReader.readCollectionStartTimeFromStream(inputStream)

            if (shouldSucceed) {
                assertNotNull("$description should succeed", result)
            } else {
                assertNull("$description should fail", result)
            }
        }
    }

    @Test
    fun testRealWorldExample() {
        // 基于记忆中的示例：0x1801020c0000 表示 24/01/02 12:00:00
        val testData = byteArrayOf(
            // SN码
            0x12, 0x34, 0x56, 0x78, 0x9A.toByte(), 0xBC.toByte(),
            // 时间：24/01/02 12:00:00
            0x18, 0x01, 0x02, 0x0C, 0x00, 0x00
        )

        val inputStream = ByteArrayInputStream(testData)
        val result = EcgBinFileReader.readCollectionStartTimeFromStream(inputStream)

        assertNotNull("Should parse real world example", result)
        assertEquals("Year should be 2024", 2024, result!!.year)
        assertEquals("Month should be 1", 1, result.monthValue)
        assertEquals("Day should be 2", 2, result.dayOfMonth)
        assertEquals("Hour should be 12", 12, result.hour)
        assertEquals("Minute should be 0", 0, result.minute)
        assertEquals("Second should be 0", 0, result.second)
    }

    @Test
    fun testYearCalculation() {
        // 测试年份计算（基于2000年）
        val testCases = listOf(
            Pair(0x18.toByte(), 2024), // 0x18 = 24 -> 2024
            Pair(0x00.toByte(), 2000), // 0x00 = 0 -> 2000
            Pair(0x63.toByte(), 2099)  // 0x63 = 99 -> 2099
        )

        testCases.forEach { (yearByte, expectedYear) ->
            val testData = byteArrayOf(
                0x12, 0x34, 0x56, 0x78, 0x9A.toByte(), 0xBC.toByte(), // SN码
                yearByte, 0x01, 0x01, 0x00, 0x00, 0x00 // 时间
            )

            val inputStream = ByteArrayInputStream(testData)
            val result = EcgBinFileReader.readCollectionStartTimeFromStream(inputStream)

            assertNotNull("Should parse year $expectedYear", result)
            assertEquals("Year should be $expectedYear", expectedYear, result!!.year)
        }
    }
}

package com.liuxinyu.neurosleep.util.file

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.InputStream
import java.time.LocalDateTime

/**
 * ECG.BIN文件读取工具类
 * 用于读取ECG.BIN文件头中的信息
 */
object EcgBinFileReader {
    
    private const val TAG = "EcgBinFileReader"
    
    // 文件头结构常量
    private const val SN_CODE_START = 0    // 字节1-6：SN码
    private const val SN_CODE_LENGTH = 6
    private const val START_TIME_START = 6  // 字节7-12：开始时间
    private const val START_TIME_LENGTH = 6
    
    /**
     * 从ECG.BIN文件中读取采集开始时间
     *
     * @param context 上下文
     * @param uri 文件URI
     * @return 采集开始时间，如果读取失败返回null
     */
    fun readCollectionStartTime(context: Context, uri: Uri): LocalDateTime? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                readFileHeader(inputStream)?.let { header ->
                    val timeBytes = header.sliceArray(START_TIME_START until START_TIME_START + START_TIME_LENGTH)
                    parseTimeFromBytes(timeBytes)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read collection start time from file", e)
            null
        }
    }
    
    /**
     * 从输入流中读取文件头
     *
     * @param inputStream 文件输入流
     * @return 文件头字节数组，如果读取失败返回null
     */
    private fun readFileHeader(inputStream: InputStream): ByteArray? {
        return try {
            val headerBytes = ByteArray(12)
            val bytesRead = inputStream.read(headerBytes)

            if (bytesRead < 12) {
                Log.e(TAG, "File too short, expected at least 12 bytes, got $bytesRead")
                return null
            }

            headerBytes
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read file header", e)
            null
        }
    }

    /**
     * 从输入流中读取采集开始时间
     *
     * @param inputStream 文件输入流
     * @return 采集开始时间，如果读取失败返回null
     */
    fun readCollectionStartTimeFromStream(inputStream: InputStream): LocalDateTime? {
        return try {
            readFileHeader(inputStream)?.let { headerBytes ->
                // 提取时间字节（字节7-12，即索引6-11）
                val timeBytes = headerBytes.sliceArray(START_TIME_START until START_TIME_START + START_TIME_LENGTH)
                // 解析时间
                parseTimeFromBytes(timeBytes)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read collection start time from stream", e)
            null
        }
    }
    
    /**
     * 从ECG.BIN文件中读取SN码
     *
     * @param context 上下文
     * @param uri 文件URI
     * @return SN码字符串，如果读取失败返回null
     */
    fun readSnCode(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                readFileHeader(inputStream)?.let { header ->
                    val snBytes = header.sliceArray(SN_CODE_START until SN_CODE_START + SN_CODE_LENGTH)
                    snBytes.joinToString("") { "%02X".format(it) }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read SN code from file", e)
            null
        }
    }

    /**
     * 从输入流中读取SN码
     */
    fun readSnCodeFromStream(inputStream: InputStream): String? {
        return try {
            readFileHeader(inputStream)?.let { headerBytes ->
                // 提取SN码字节（字节1-6，即索引0-5）
                val snBytes = headerBytes.sliceArray(SN_CODE_START until SN_CODE_START + SN_CODE_LENGTH)
                // 转换为十六进制字符串
                snBytes.joinToString("") { "%02X".format(it) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read SN code from stream", e)
            null
        }
    }
    
    /**
     * 解析时间字节为LocalDateTime
     * 时间格式：年-月-日-时-分-秒 (十六进制)
     * 例如：0x1801020c0000 表示 24/01/02 12:00:00
     *
     * @param timeBytes 6个字节的时间数据
     * @return 解析后的LocalDateTime，如果解析失败返回null
     */
    fun parseTimeFromBytes(timeBytes: ByteArray): LocalDateTime? {
        if (timeBytes.size != 6) {
            Log.e(TAG, "Invalid time bytes length: ${timeBytes.size}, expected 6")
            return null
        }
        
        return try {
            // 将字节转换为无符号整数
            val year = (timeBytes[0].toInt() and 0xFF) + 2000  // 年份基于2000年
            val month = timeBytes[1].toInt() and 0xFF
            val day = timeBytes[2].toInt() and 0xFF
            val hour = timeBytes[3].toInt() and 0xFF
            val minute = timeBytes[4].toInt() and 0xFF
            val second = timeBytes[5].toInt() and 0xFF
            
            Log.d(TAG, "Parsed time: $year-$month-$day $hour:$minute:$second")
            
            // 验证时间值的合理性
            if (month < 1 || month > 12) {
                Log.e(TAG, "Invalid month: $month")
                return null
            }
            if (day < 1 || day > 31) {
                Log.e(TAG, "Invalid day: $day")
                return null
            }
            if (hour > 23) {
                Log.e(TAG, "Invalid hour: $hour")
                return null
            }
            if (minute > 59) {
                Log.e(TAG, "Invalid minute: $minute")
                return null
            }
            if (second > 59) {
                Log.e(TAG, "Invalid second: $second")
                return null
            }
            
            LocalDateTime.of(year, month, day, hour, minute, second)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse time from bytes: ${timeBytes.joinToString(" ") { "%02X".format(it) }}", e)
            null
        }
    }
    
    /**
     * 将时间字节数组转换为十六进制字符串（用于调试）
     */
    fun timeToHexString(timeBytes: ByteArray): String {
        return timeBytes.joinToString("") { "%02X".format(it) }
    }
    
    /**
     * 读取并显示文件头信息（用于调试）
     */
    fun debugFileHeader(context: Context, uri: Uri) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                readFileHeader(inputStream)?.let { headerBytes ->
                    Log.d(TAG, "=== ECG.BIN File Header Debug ===")
                    Log.d(TAG, "Bytes read: ${headerBytes.size}")
                    Log.d(TAG, "Full header: ${headerBytes.joinToString(" ") { "%02X".format(it) }}")

                    val snBytes = headerBytes.sliceArray(SN_CODE_START until SN_CODE_START + SN_CODE_LENGTH)
                    val timeBytes = headerBytes.sliceArray(START_TIME_START until START_TIME_START + START_TIME_LENGTH)

                    Log.d(TAG, "SN Code bytes: ${snBytes.joinToString(" ") { "%02X".format(it) }}")
                    Log.d(TAG, "Time bytes: ${timeBytes.joinToString(" ") { "%02X".format(it) }}")

                    // 直接从已读取的字节解析
                    val snCode = snBytes.joinToString("") { "%02X".format(it) }
                    val startTime = parseTimeFromBytes(timeBytes)

                    Log.d(TAG, "Parsed SN Code: $snCode")
                    Log.d(TAG, "Parsed Start Time: $startTime")
                    Log.d(TAG, "=== End Debug ===")
                } ?: run {
                    Log.e(TAG, "Failed to read file header for debugging")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to debug file header", e)
        }
    }
}

package com.liuxinyu.neurosleep.core.ble

import android.util.Log
import com.liuxinyu.neurosleep.data.model.EcgData
import com.liuxinyu.neurosleep.util.FormattedTime
import java.nio.ByteBuffer
import java.nio.ByteOrder

object ByteUtil {
    // 起始位都是 0xFA 结束位都是 0xFB

    /*
    1) 开始采集命令会删除 ECG.bin 文件，清除旧数据。
    2) 该命令会收到握手应答，当收到握手应答代表命令已正式执行或启动失败，才可进行下一步命令发送。
    3) 开始时间需转换成 16 进制发送。
    4) 发送停止采集命令可不加时间信息，直接以 0xFB 结尾
    两种调用方式
    1) 发送开始采集命令 packCtrlCommand(true,TimeUtil.getFormattedTime())
    2) 发送结束采集命令 packCtrlCommand(false,null)
    * */
    fun packCollectCommand(flag: Boolean,startTime : FormattedTime? = null) : ByteArray{
        val buffer = mutableListOf<Byte>()
        buffer.add(0xFA.toByte())
        buffer.add(0x01.toByte())
        buffer.add(if (flag) 0x01.toByte() else 0x00.toByte())

        if (flag){
            // 如果有开始时间，则添加（使用直接十六进制编码，非BCD）
            // 根据协议示例：0x1801020c0000 表示 24年1月2号12点0分0秒
            // 这里只使用年份后两位，设备固件会自动处理年份前缀
            startTime?.let {
                buffer.add((it.year % 100).toByte()) // 年份后两位（如2025年 -> 25）
                buffer.add(it.month.toByte())         // 月份
                buffer.add(it.day.toByte())           // 日期
                buffer.add(it.hour.toByte())          // 小时
                buffer.add(it.minute.toByte())        // 分钟
                buffer.add(it.second.toByte())        // 秒
            }
        }
        else
            buffer.add(0x00.toByte())

        buffer.add(0xFB.toByte())
        return buffer.toByteArray()
    }

    /*
    1、开始蓝牙传输会从当前即时的ECG数据开始传输，如设备未开始采集，则不会收到蓝牙
    数据而是从2A38通道收到采集未开始警告信号。
    2、停止传输不会停止ECG数据采集。
    3、蓝牙传输与否不会影响ECG数据存储于本地中。
    */
    fun packTransferCommand(flag : Boolean) : ByteArray{
        val buffer = mutableListOf<Byte>()
        buffer.add(0xFA.toByte())
        buffer.add(0x02.toByte())
        buffer.add(if (flag) 0x01.toByte() else 0x00.toByte())
        buffer.add(0xFB.toByte())
        return buffer.toByteArray()
    }

    /*
    * 在Kotlin（以及许多其他编程语言）中，and 0xFF操作通常用于确保字节值被视为无符号整数的一部分，
    * 特别是在处理来自字节数组的数据时。这是因为：
    * (1) 字节的有符号性：在Kotlin中，Byte类型是有符号的，范围是-128到127。
    * 当你将一个Byte类型的值转换为更大的整数类型（如Int）时，如果该字节的最高位（最左边的位）是1，则会被解释为负数。
    * 这可能会导致不正确的结果，特别是当你尝试组合多个字节来形成更大的数值时。
    * (2) 确保正数表示：通过使用 and 0xFF，你可以屏蔽掉高于8位的所有位，从而确保得到的是原始字节的无符号表示。
    * 0xFF是一个8位全1的掩码（即二进制的11111111），当它与任何8位值进行按位与操作时，会保留这个8位值的每一位，而不会影响它们。
    * 这样就确保了即使是负数的字节也会被正确地解释为0到255之间的值。
    * 示例 假设你有一个字节值 -1，其二进制表示为 11111111（在Kotlin中作为有符号字节）。
    * 如果你直接将其转换为Int，它仍然是 -1。但如果你对它执行 and 0xFF 操作，结果将是 255，这是该字节作为无符号值的正确表示。
    * */
    // 如果需要转换成有符号整数，可以根据最高位判断是否需要补码转换
    private fun toSignedInt(value: Int): Int {
        return if ((value shr 23) == 1) value - (1 shl 24) else value
    }

    // 收到的 ECG 数据解析 一次接收 8 个点
    // [0, 63, -106, 39, 64, -117, 106, 66, -83, 45, 0, 62, -15, -74, 64, 37, 80, 66, -124, -11, 0, 62, 7, 91, 63, 72, 106, 66, 103, 22, 0, 62, -78, 6, 63, -51, 0, 66, -124, 10, 0, 63, -111, -81, 64, -119, -38, 66, -108, -29, 0, 62, -15, -65, 64, 41, 21, 66, 97, 20, 0, 62, 3, -7, 63, 71, -14, 66, 52, 19, 0, 62, -83, 88, 63, -54, 39, 66, 82, -17]
    fun parseEcgDataPacket(data: ByteArray): MutableList<EcgData> {
        // 假设这是你的 80 个数据点
        val dataPoints = data.toList() // 这里用 1 到 80 的整数作为示例数据点

        // 定义每组的数据点数量
        val groupSize = 10

        // 使用 windowed 函数分组（Kotlin 1.5+）
        val groupedData = dataPoints.windowed(groupSize, step = groupSize)

        val res : MutableList<EcgData> = mutableListOf()
        // 遍历每个分组并处理
        for ((index, group) in groupedData.withIndex()) {
            res.add(parseEcgPoint(group.toByteArray()))
        }

        return res
    }

    // 单点解析一个三导 ECG 数据点
    fun parseEcgPoint(data : ByteArray) : EcgData {
        if (data.size != 10) {
            // 有问题
            // 数据长度不正确，直接返回null或抛出异常
            return EcgData(-1,-1,-1,-1)
        }

        // 创建 ByteBuffer 并设置为大端模式（Big Endian）
        val buffer = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN)

        // 读取状态位
        val statusFlags = buffer.get()

        // 读取ECG1、ECG2和ECG3数据
        // 注意：因为每个ECG值是三个字节，我们需要手动组合它们
        val ecg1 = (buffer.get().toInt() and 0xFF shl 16) or
                (buffer.get().toInt() and 0xFF shl 8) or
                (buffer.get().toInt() and 0xFF)

        val ecg2 = (buffer.get().toInt() and 0xFF shl 16) or
                (buffer.get().toInt() and 0xFF shl 8) or
                (buffer.get().toInt() and 0xFF)

        val ecg3 = (buffer.get().toInt() and 0xFF shl 16) or
                (buffer.get().toInt() and 0xFF shl 8) or
                (buffer.get().toInt() and 0xFF)

        return EcgData(
            statusFlags = statusFlags,
            ecg1 = toSignedInt(ecg1),
            ecg2 = toSignedInt(ecg2),
            ecg3 = toSignedInt(ecg3)
        )
    }

    /*
    * 依据厂商提供的转换规则：零点消去 DC，数据除以 6727.4
    * */
    fun convert() : Float {
        return 1.0f
    }

    /**
     * 解析ECG.BIN文件头部，提取采集开始时间
     *
     * ECG.BIN文件头格式（32字节）：
     * - 字节 1-6: SN码（16进制12位SN码）
     * - 字节 7-12: 开始时间（16进制：年前两位、年后两位、月、日、时、分）
     * - 字节 13: 错误警告
     * - 字节 14-32: 保留数据位
     *
     * 时间格式示例：0x1801020c0000 表示 2024年1月2号12点0分0秒
     *
     * @param headerBytes ECG.BIN文件的前32字节
     * @return FormattedTime对象，包含解析出的时间信息；如果解析失败返回null
     */
    fun parseEcgBinHeader(headerBytes: ByteArray): FormattedTime? {
        if (headerBytes.size < 12) {
            Log.e("ByteUtil", "Header bytes too short: ${headerBytes.size}, expected at least 12")
            return null
        }

        try {
            // 字节 7-12 (索引 6-11) 包含时间数据
            val yearPrefix = headerBytes[6].toInt() and 0xFF  // 年份前两位
            val yearSuffix = headerBytes[7].toInt() and 0xFF  // 年份后两位
            val month = headerBytes[8].toInt() and 0xFF       // 月份
            val day = headerBytes[9].toInt() and 0xFF         // 日期
            val hour = headerBytes[10].toInt() and 0xFF       // 小时
            val minute = headerBytes[11].toInt() and 0xFF     // 分钟

            // 构建完整年份
            val fullYear = yearPrefix * 100 + yearSuffix

            Log.d("ByteUtil", "Parsed ECG.BIN header time: $fullYear-$month-$day $hour:$minute")
            Log.d("ByteUtil", "Raw bytes: ${headerBytes.take(12).joinToString(" ") { String.format("%02X", it.toInt() and 0xFF) }}")

            // 验证时间数据的合理性
            if (fullYear < 2000 || fullYear > 2100 ||
                month < 1 || month > 12 ||
                day < 1 || day > 31 ||
                hour < 0 || hour > 23 ||
                minute < 0 || minute > 59) {
                Log.e("ByteUtil", "Invalid time values in ECG.BIN header: $fullYear-$month-$day $hour:$minute")
                return null
            }

            return FormattedTime(
                year = fullYear,
                month = month,
                day = day,
                hour = hour,
                minute = minute,
                second = 0  // ECG.BIN头部只包含到分钟，秒数设为0
            )
        } catch (e: Exception) {
            Log.e("ByteUtil", "Error parsing ECG.BIN header", e)
            return null
        }
    }

}
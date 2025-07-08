package com.liuxinyu.neurosleep.core.ble

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
            buffer.add(0x01.toByte())
            // 如果有开始时间，则添加
            startTime?.let {
                buffer.add((it.year % 100).toByte()) // 确认一下是不是只取年份的后两位
                buffer.add(it.month.toByte())
                buffer.add(it.day.toByte())
                buffer.add(it.hour.toByte())
                buffer.add(it.minute.toByte())
                buffer.add(it.second.toByte())
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

}
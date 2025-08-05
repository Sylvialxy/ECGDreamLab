package com.liuxinyu.neurosleep.feature.stimulus.util

import android.util.Log
import com.liuxinyu.neurosleep.feature.stimulus.config.StimulusConfig
import com.liuxinyu.neurosleep.feature.stimulus.model.StimulusParams
import com.liuxinyu.neurosleep.feature.stimulus.model.StimulusResponse
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * 刺激设备命令工具类
 * 负责刺激设备命令的编码和响应数据的解码
 * 基于协议格式：55 BB [LEN] [CMD] [DATA] [CHECKSUM]
 */
object StimulusCommandUtil {

    private const val TAG = "StimulusCommandUtil"
    
    /**
     * 创建通道选择命令
     * @param channel 通道号 (0: 通道1, 1: 通道2)
     * @return 命令字节数组
     */
    fun createChannelSelectCommand(channel: Byte): ByteArray {
        return createCommand(StimulusConfig.CommandType.CHANNEL_SELECT, byteArrayOf(channel))
    }

    /**
     * 创建工作时间设置命令
     * @param workTimeMinutes 工作时间（分钟，0-600）
     * @return 命令字节数组
     */
    fun createWorkTimeSetCommand(workTimeMinutes: Int): ByteArray {
        val data = ByteBuffer.allocate(2).order(ByteOrder.BIG_ENDIAN)
            .putShort(workTimeMinutes.toShort()).array()
        return createCommand(StimulusConfig.CommandType.WORK_TIME_SET, data)
    }

    /**
     * 创建电刺激启动/停止命令
     * @param start true: 启动, false: 停止
     * @return 命令字节数组
     */
    fun createStimulusControlCommand(start: Boolean): ByteArray {
        val control = if (start) StimulusConfig.StimulusControl.START else StimulusConfig.StimulusControl.STOP
        return createCommand(StimulusConfig.CommandType.STIMULUS_CONTROL, byteArrayOf(control))
    }

    /**
     * 创建电刺激模式设置命令
     * @param mode 刺激模式 (0: 直流, 1: 矩形, 2: 双频)
     * @return 命令字节数组
     */
    fun createStimulusModeSetCommand(mode: Byte): ByteArray {
        return createCommand(StimulusConfig.CommandType.STIMULUS_MODE_SET, byteArrayOf(mode))
    }

    /**
     * 创建电刺激基波频率设置命令
     * @param frequencyHz 频率（Hz，1-10000）
     * @return 命令字节数组
     */
    fun createFrequencySetCommand(frequencyHz: Int): ByteArray {
        val data = ByteBuffer.allocate(2).order(ByteOrder.BIG_ENDIAN)
            .putShort(frequencyHz.toShort()).array()
        return createCommand(StimulusConfig.CommandType.FREQUENCY_SET, data)
    }

    /**
     * 创建电刺激基波脉宽设置命令
     * @param pulseWidthUs 脉宽（微秒，50-1000，步进10）
     * @return 命令字节数组
     */
    fun createPulseWidthSetCommand(pulseWidthUs: Int): ByteArray {
        val data = ByteBuffer.allocate(2).order(ByteOrder.BIG_ENDIAN)
            .putShort(pulseWidthUs.toShort()).array()
        return createCommand(StimulusConfig.CommandType.PULSE_WIDTH_SET, data)
    }

    /**
     * 创建电刺激电流强度设置命令
     * @param currentMa 电流强度（mA，0-80，步进0.1，数据*10发送）
     * @return 命令字节数组
     */
    fun createCurrentSetCommand(currentMa: Float): ByteArray {
        val currentValue = (currentMa * 10).toInt().toShort()
        val data = ByteBuffer.allocate(2).order(ByteOrder.BIG_ENDIAN)
            .putShort(currentValue).array()
        return createCommand(StimulusConfig.CommandType.CURRENT_SET, data)
    }

    /**
     * 创建调制波上升时间设置命令
     * @param riseTimeS 上升时间（秒，0.1-60，步进0.1，数据*10发送）
     * @return 命令字节数组
     */
    fun createRiseTimeSetCommand(riseTimeS: Float): ByteArray {
        val riseTimeValue = (riseTimeS * 10).toInt().toShort()
        val data = ByteBuffer.allocate(2).order(ByteOrder.BIG_ENDIAN)
            .putShort(riseTimeValue).array()
        return createCommand(StimulusConfig.CommandType.RISE_TIME_SET, data)
    }

    /**
     * 创建调制波保持时间设置命令
     * @param holdTimeS 保持时间（秒，0.1-60，步进0.1，数据*10发送）
     * @return 命令字节数组
     */
    fun createHoldTimeSetCommand(holdTimeS: Float): ByteArray {
        val holdTimeValue = (holdTimeS * 10).toInt().toShort()
        val data = ByteBuffer.allocate(2).order(ByteOrder.BIG_ENDIAN)
            .putShort(holdTimeValue).array()
        return createCommand(StimulusConfig.CommandType.HOLD_TIME_SET, data)
    }

    /**
     * 创建调制波下降时间设置命令
     * @param fallTimeS 下降时间（秒，0.1-60，步进0.1，数据*10发送）
     * @return 命令字节数组
     */
    fun createFallTimeSetCommand(fallTimeS: Float): ByteArray {
        val fallTimeValue = (fallTimeS * 10).toInt().toShort()
        val data = ByteBuffer.allocate(2).order(ByteOrder.BIG_ENDIAN)
            .putShort(fallTimeValue).array()
        return createCommand(StimulusConfig.CommandType.FALL_TIME_SET, data)
    }

    /**
     * 创建调制波停止时间设置命令
     * @param stopTimeS 停止时间（秒，0.1-60，步进0.1，数据*10发送）
     * @return 命令字节数组
     */
    fun createStopTimeSetCommand(stopTimeS: Float): ByteArray {
        val stopTimeValue = (stopTimeS * 10).toInt().toShort()
        val data = ByteBuffer.allocate(2).order(ByteOrder.BIG_ENDIAN)
            .putShort(stopTimeValue).array()
        return createCommand(StimulusConfig.CommandType.STOP_TIME_SET, data)
    }
    
    /**
     * 通用命令创建方法
     * 协议格式：55 BB [LEN] [CMD] [DATA] [CHECKSUM]
     * @param commandType 命令类型
     * @param data 数据部分
     * @return 完整的命令字节数组
     */
    fun createCommand(commandType: Byte, data: ByteArray): ByteArray {
        val totalLength = 5 + data.size // 帧头(2) + 长度(1) + 命令(1) + 数据 + 校验(1)
        val buffer = ByteBuffer.allocate(totalLength)

        // 帧头：55 BB
        buffer.put(StimulusConfig.Protocol.FRAME_HEADER_1)
        buffer.put(StimulusConfig.Protocol.FRAME_HEADER_2)

        // 长度：只包含DATA的长度，不包括CMD
        buffer.put(data.size.toByte())

        // 命令类型
        buffer.put(commandType)

        // 数据
        buffer.put(data)

        // 计算校验和：从帧头55BB到dataN所有数据的累和并取低八位
        var checksum = (StimulusConfig.Protocol.FRAME_HEADER_1.toInt() and 0xFF) +
                      (StimulusConfig.Protocol.FRAME_HEADER_2.toInt() and 0xFF) +
                      (data.size and 0xFF) +
                      (commandType.toInt() and 0xFF)

        for (byte in data) {
            checksum += (byte.toInt() and 0xFF)
        }

        buffer.put((checksum and 0xFF).toByte())

        val command = buffer.array()
        Log.d(TAG, "Created command: ${command.joinToString(" ") { "0x%02X".format(it.toInt() and 0xFF) }}")
        return command
    }
    
    /**
     * 解析设备响应数据
     * 协议格式：55 BB [LEN] [CMD] [DATA] [CHECKSUM]
     * @param data 接收到的字节数组
     * @return 解析后的响应对象，如果解析失败返回null
     */
    fun parseResponse(data: ByteArray): StimulusResponse? {
        if (data.size < StimulusConfig.Protocol.MIN_FRAME_LENGTH) {
            Log.e(TAG, "Response data too short: ${data.size}")
            return null
        }

        try {
            val buffer = ByteBuffer.wrap(data)

            // 检查帧头：55 BB
            val header1 = buffer.get()
            val header2 = buffer.get()
            if (header1 != StimulusConfig.Protocol.FRAME_HEADER_1 ||
                header2 != StimulusConfig.Protocol.FRAME_HEADER_2) {
                Log.e(TAG, "Invalid frame header: 0x${String.format("%02X", header1)} 0x${String.format("%02X", header2)}")
                return null
            }

            // 读取长度（只包含DATA的长度）
            val dataLength = buffer.get().toInt() and 0xFF
            if (dataLength + 5 != data.size) { // +5 for header(2) + length(1) + cmd(1) + checksum(1)
                Log.e(TAG, "Invalid frame length: expected ${dataLength + 5}, got ${data.size}")
                return null
            }

            // 读取命令类型
            val commandType = buffer.get()

            // 读取数据部分
            val responseData = ByteArray(dataLength)
            buffer.get(responseData)

            // 读取校验和
            val receivedChecksum = buffer.get()

            // 验证校验和：从帧头55BB到dataN所有数据的累和并取低八位
            var calculatedChecksum = (StimulusConfig.Protocol.FRAME_HEADER_1.toInt() and 0xFF) +
                                   (StimulusConfig.Protocol.FRAME_HEADER_2.toInt() and 0xFF) +
                                   (dataLength and 0xFF) +
                                   (commandType.toInt() and 0xFF)

            for (byte in responseData) {
                calculatedChecksum += (byte.toInt() and 0xFF)
            }
            calculatedChecksum = calculatedChecksum and 0xFF

            if (calculatedChecksum.toByte() != receivedChecksum) {
                Log.e(TAG, "Checksum mismatch: calculated 0x${String.format("%02X", calculatedChecksum)}, received 0x${String.format("%02X", receivedChecksum)}")
                return null
            }

            Log.d(TAG, "Parsed response: command=0x${String.format("%02X", commandType)}, data=${responseData.joinToString(" ") { "0x%02X".format(it.toInt() and 0xFF) }}")

            return StimulusResponse(commandType, responseData)

        } catch (e: Exception) {
            Log.e(TAG, "Error parsing response", e)
            return null
        }
    }
    
    /**
     * 验证刺激参数是否在有效范围内
     * @param params 刺激参数
     * @return 验证结果
     */
    fun validateParams(params: StimulusParams): Boolean {
        return params.intensity in StimulusConfig.StimulusParams.MIN_INTENSITY..StimulusConfig.StimulusParams.MAX_INTENSITY &&
                params.frequency in StimulusConfig.StimulusParams.MIN_FREQUENCY..StimulusConfig.StimulusParams.MAX_FREQUENCY &&
                params.pulseWidth in StimulusConfig.StimulusParams.MIN_PULSE_WIDTH..StimulusConfig.StimulusParams.MAX_PULSE_WIDTH &&
                params.workTimeMinutes in StimulusConfig.StimulusParams.MIN_WORK_TIME..StimulusConfig.StimulusParams.MAX_WORK_TIME &&
                params.riseTime in StimulusConfig.StimulusParams.MIN_DURATION..StimulusConfig.StimulusParams.MAX_DURATION &&
                params.holdTime in StimulusConfig.StimulusParams.MIN_DURATION..StimulusConfig.StimulusParams.MAX_DURATION &&
                params.fallTime in StimulusConfig.StimulusParams.MIN_DURATION..StimulusConfig.StimulusParams.MAX_DURATION &&
                params.stopTime in StimulusConfig.StimulusParams.MIN_DURATION..StimulusConfig.StimulusParams.MAX_DURATION
    }
    
    /**
     * 将字节数组转换为十六进制字符串（用于调试）
     * @param data 字节数组
     * @return 十六进制字符串
     */
    fun bytesToHexString(data: ByteArray): String {
        return data.joinToString(" ") { "0x%02X".format(it.toInt() and 0xFF) }
    }
}

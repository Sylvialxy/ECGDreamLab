package com.liuxinyu.neurosleep.feature.stimulus.model

import com.liuxinyu.neurosleep.feature.stimulus.config.StimulusConfig

/**
 * 刺激设备响应数据类
 * 用于解析设备返回的响应数据
 */
data class StimulusResponse(
    val commandType: Byte,      // 命令类型
    val data: ByteArray,        // 响应数据
    val timestamp: Long = System.currentTimeMillis() // 响应时间戳
) {
    
    /**
     * 判断是否为设备状态上报
     * @return 是否为设备状态上报
     */
    fun isDeviceStatusReport(): Boolean {
        return commandType == StimulusConfig.CommandType.DEVICE_STATUS_REPORT
    }
    
    /**
     * 判断是否为通道状态上报
     * @return 是否为通道状态上报
     */
    fun isChannelStatusReport(): Boolean {
        return commandType == StimulusConfig.CommandType.CHANNEL_STATUS_REPORT
    }
    
    /**
     * 获取设备状态（仅当为设备状态上报时有效）
     * @return 设备状态，如果不是状态上报则返回null
     */
    fun getDeviceStatus(): Byte? {
        return if (isDeviceStatusReport() && data.isNotEmpty()) {
            data[0]
        } else {
            null
        }
    }
    
    /**
     * 获取设备状态名称
     * @return 设备状态名称
     */
    fun getDeviceStatusName(): String? {
        val status = getDeviceStatus() ?: return null
        return when (status) {
            StimulusConfig.DeviceStatus.STANDBY -> "待机"
            StimulusConfig.DeviceStatus.RUNNING -> "运行"
            StimulusConfig.DeviceStatus.PAUSED -> "暂停"
            else -> "未知状态(0x${String.format("%02X", status)})"
        }
    }
    
    /**
     * 获取当前选定通道（仅当为通道状态上报时有效）
     * @return 通道号，如果不是通道状态上报则返回null
     */
    fun getCurrentChannel(): Byte? {
        return if (isChannelStatusReport() && data.isNotEmpty()) {
            data[0]
        } else {
            null
        }
    }
    
    /**
     * 获取通道名称
     * @return 通道名称
     */
    fun getChannelName(): String? {
        val channel = getCurrentChannel() ?: return null
        return when (channel) {
            StimulusConfig.Channel.CHANNEL_1 -> "通道1"
            StimulusConfig.Channel.CHANNEL_2 -> "通道2"
            else -> "未知通道(0x${String.format("%02X", channel)})"
        }
    }
    
    /**
     * 判断响应是否有效
     * @return 是否有效
     */
    fun isValid(): Boolean {
        return when (commandType) {
            StimulusConfig.CommandType.DEVICE_STATUS_REPORT -> {
                data.size == 1 && data[0] in arrayOf(
                    StimulusConfig.DeviceStatus.STANDBY,
                    StimulusConfig.DeviceStatus.RUNNING,
                    StimulusConfig.DeviceStatus.PAUSED
                )
            }
            StimulusConfig.CommandType.CHANNEL_STATUS_REPORT -> {
                data.size == 1 && data[0] in arrayOf(
                    StimulusConfig.Channel.CHANNEL_1,
                    StimulusConfig.Channel.CHANNEL_2
                )
            }
            // 对于设置命令的响应，通常只需要确认收到即可
            StimulusConfig.CommandType.CHANNEL_SELECT,
            StimulusConfig.CommandType.WORK_TIME_SET,
            StimulusConfig.CommandType.STIMULUS_CONTROL,
            StimulusConfig.CommandType.STIMULUS_MODE_SET,
            StimulusConfig.CommandType.FREQUENCY_SET,
            StimulusConfig.CommandType.PULSE_WIDTH_SET,
            StimulusConfig.CommandType.CURRENT_SET,
            StimulusConfig.CommandType.RISE_TIME_SET,
            StimulusConfig.CommandType.HOLD_TIME_SET,
            StimulusConfig.CommandType.FALL_TIME_SET,
            StimulusConfig.CommandType.STOP_TIME_SET -> {
                // 设置命令的响应通常为空或包含确认信息
                true
            }
            else -> false
        }
    }
    
    /**
     * 获取命令类型名称
     * @return 命令类型名称
     */
    fun getCommandTypeName(): String {
        return when (commandType) {
            StimulusConfig.CommandType.DEVICE_STATUS_REPORT -> "设备状态上报"
            StimulusConfig.CommandType.CHANNEL_STATUS_REPORT -> "通道状态上报"
            StimulusConfig.CommandType.CHANNEL_SELECT -> "通道选择"
            StimulusConfig.CommandType.WORK_TIME_SET -> "工作时间设置"
            StimulusConfig.CommandType.STIMULUS_CONTROL -> "刺激控制"
            StimulusConfig.CommandType.STIMULUS_MODE_SET -> "刺激模式设置"
            StimulusConfig.CommandType.FREQUENCY_SET -> "频率设置"
            StimulusConfig.CommandType.PULSE_WIDTH_SET -> "脉宽设置"
            StimulusConfig.CommandType.CURRENT_SET -> "电流强度设置"
            StimulusConfig.CommandType.RISE_TIME_SET -> "上升时间设置"
            StimulusConfig.CommandType.HOLD_TIME_SET -> "保持时间设置"
            StimulusConfig.CommandType.FALL_TIME_SET -> "下降时间设置"
            StimulusConfig.CommandType.STOP_TIME_SET -> "停止时间设置"
            else -> "未知命令(0x${String.format("%02X", commandType)})"
        }
    }
    
    /**
     * 获取数据的十六进制字符串表示
     * @return 十六进制字符串
     */
    fun getDataHexString(): String {
        return data.joinToString(" ") { "0x%02X".format(it.toInt() and 0xFF) }
    }
    
    override fun toString(): String {
        return "StimulusResponse(command=${getCommandTypeName()}, data=[${getDataHexString()}], timestamp=$timestamp)"
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as StimulusResponse
        
        if (commandType != other.commandType) return false
        if (!data.contentEquals(other.data)) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = commandType.toInt()
        result = 31 * result + data.contentHashCode()
        return result
    }
}

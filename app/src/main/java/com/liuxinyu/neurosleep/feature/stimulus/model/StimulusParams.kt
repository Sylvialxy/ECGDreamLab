package com.liuxinyu.neurosleep.feature.stimulus.model

import com.liuxinyu.neurosleep.feature.stimulus.config.StimulusConfig

/**
 * 刺激参数数据类
 * 包含所有刺激设备的参数设置
 */
data class StimulusParams(
    // 基本参数
    var channel: Byte = StimulusConfig.Channel.CHANNEL_1,           // 通道选择
    var workTimeMinutes: Int = StimulusConfig.UI.DEFAULT_WORK_TIME, // 工作时间（分钟）
    var stimulusMode: Byte = StimulusConfig.StimulusMode.RECTANGULAR, // 刺激模式
    
    // 刺激波形参数
    var frequency: Int = StimulusConfig.UI.DEFAULT_FREQUENCY,       // 基波频率（Hz）
    var pulseWidth: Int = StimulusConfig.UI.DEFAULT_PULSE_WIDTH,    // 基波脉宽（微秒）
    var intensity: Float = StimulusConfig.UI.DEFAULT_INTENSITY,     // 电流强度（mA）
    
    // 调制波参数
    var riseTime: Float = StimulusConfig.UI.DEFAULT_RISE_TIME,      // 上升时间（秒）
    var holdTime: Float = StimulusConfig.UI.DEFAULT_HOLD_TIME,      // 保持时间（秒）
    var fallTime: Float = StimulusConfig.UI.DEFAULT_FALL_TIME,      // 下降时间（秒）
    var stopTime: Float = StimulusConfig.UI.DEFAULT_STOP_TIME,      // 停止时间（秒）
    
    // 控制状态
    var isRunning: Boolean = false                                  // 是否正在运行
) {
    
    /**
     * 验证参数是否在有效范围内
     * @return 验证结果
     */
    fun isValid(): Boolean {
        return workTimeMinutes in StimulusConfig.StimulusParams.MIN_WORK_TIME..StimulusConfig.StimulusParams.MAX_WORK_TIME &&
                frequency in StimulusConfig.StimulusParams.MIN_FREQUENCY..StimulusConfig.StimulusParams.MAX_FREQUENCY &&
                pulseWidth in StimulusConfig.StimulusParams.MIN_PULSE_WIDTH..StimulusConfig.StimulusParams.MAX_PULSE_WIDTH &&
                intensity >= StimulusConfig.StimulusParams.MIN_INTENSITY &&
                intensity <= StimulusConfig.StimulusParams.MAX_INTENSITY &&
                riseTime >= StimulusConfig.StimulusParams.MIN_DURATION &&
                riseTime <= StimulusConfig.StimulusParams.MAX_DURATION &&
                holdTime >= StimulusConfig.StimulusParams.MIN_DURATION &&
                holdTime <= StimulusConfig.StimulusParams.MAX_DURATION &&
                fallTime >= StimulusConfig.StimulusParams.MIN_DURATION &&
                fallTime <= StimulusConfig.StimulusParams.MAX_DURATION &&
                stopTime >= StimulusConfig.StimulusParams.MIN_DURATION &&
                stopTime <= StimulusConfig.StimulusParams.MAX_DURATION
    }
    
    /**
     * 获取参数验证错误信息
     * @return 错误信息列表
     */
    fun getValidationErrors(): List<String> {
        val errors = mutableListOf<String>()
        
        if (workTimeMinutes !in StimulusConfig.StimulusParams.MIN_WORK_TIME..StimulusConfig.StimulusParams.MAX_WORK_TIME) {
            errors.add("工作时间必须在${StimulusConfig.StimulusParams.MIN_WORK_TIME}-${StimulusConfig.StimulusParams.MAX_WORK_TIME}分钟之间")
        }
        
        if (frequency !in StimulusConfig.StimulusParams.MIN_FREQUENCY..StimulusConfig.StimulusParams.MAX_FREQUENCY) {
            errors.add("频率必须在${StimulusConfig.StimulusParams.MIN_FREQUENCY}-${StimulusConfig.StimulusParams.MAX_FREQUENCY}Hz之间")
        }
        
        if (pulseWidth !in StimulusConfig.StimulusParams.MIN_PULSE_WIDTH..StimulusConfig.StimulusParams.MAX_PULSE_WIDTH) {
            errors.add("脉宽必须在${StimulusConfig.StimulusParams.MIN_PULSE_WIDTH}-${StimulusConfig.StimulusParams.MAX_PULSE_WIDTH}μs之间")
        }
        
        if (intensity < StimulusConfig.StimulusParams.MIN_INTENSITY || intensity > StimulusConfig.StimulusParams.MAX_INTENSITY) {
            errors.add("电流强度必须在${StimulusConfig.StimulusParams.MIN_INTENSITY}-${StimulusConfig.StimulusParams.MAX_INTENSITY}mA之间")
        }
        
        if (riseTime < StimulusConfig.StimulusParams.MIN_DURATION || riseTime > StimulusConfig.StimulusParams.MAX_DURATION) {
            errors.add("上升时间必须在${StimulusConfig.StimulusParams.MIN_DURATION}-${StimulusConfig.StimulusParams.MAX_DURATION}s之间")
        }
        
        if (holdTime < StimulusConfig.StimulusParams.MIN_DURATION || holdTime > StimulusConfig.StimulusParams.MAX_DURATION) {
            errors.add("保持时间必须在${StimulusConfig.StimulusParams.MIN_DURATION}-${StimulusConfig.StimulusParams.MAX_DURATION}s之间")
        }
        
        if (fallTime < StimulusConfig.StimulusParams.MIN_DURATION || fallTime > StimulusConfig.StimulusParams.MAX_DURATION) {
            errors.add("下降时间必须在${StimulusConfig.StimulusParams.MIN_DURATION}-${StimulusConfig.StimulusParams.MAX_DURATION}s之间")
        }
        
        if (stopTime < StimulusConfig.StimulusParams.MIN_DURATION || stopTime > StimulusConfig.StimulusParams.MAX_DURATION) {
            errors.add("停止时间必须在${StimulusConfig.StimulusParams.MIN_DURATION}-${StimulusConfig.StimulusParams.MAX_DURATION}s之间")
        }
        
        return errors
    }
    
    /**
     * 复制参数
     * @return 参数副本
     */
    fun copy(): StimulusParams {
        return StimulusParams(
            channel = channel,
            workTimeMinutes = workTimeMinutes,
            stimulusMode = stimulusMode,
            frequency = frequency,
            pulseWidth = pulseWidth,
            intensity = intensity,
            riseTime = riseTime,
            holdTime = holdTime,
            fallTime = fallTime,
            stopTime = stopTime,
            isRunning = isRunning
        )
    }
    
    /**
     * 重置为默认值
     */
    fun resetToDefaults() {
        channel = StimulusConfig.Channel.CHANNEL_1
        workTimeMinutes = StimulusConfig.UI.DEFAULT_WORK_TIME
        stimulusMode = StimulusConfig.StimulusMode.RECTANGULAR
        frequency = StimulusConfig.UI.DEFAULT_FREQUENCY
        pulseWidth = StimulusConfig.UI.DEFAULT_PULSE_WIDTH
        intensity = StimulusConfig.UI.DEFAULT_INTENSITY
        riseTime = StimulusConfig.UI.DEFAULT_RISE_TIME
        holdTime = StimulusConfig.UI.DEFAULT_HOLD_TIME
        fallTime = StimulusConfig.UI.DEFAULT_FALL_TIME
        stopTime = StimulusConfig.UI.DEFAULT_STOP_TIME
        isRunning = false
    }
    
    /**
     * 获取刺激模式名称
     * @return 模式名称
     */
    fun getModeName(): String {
        return when (stimulusMode) {
            StimulusConfig.StimulusMode.DC -> "直流"
            StimulusConfig.StimulusMode.RECTANGULAR -> "矩形"
            StimulusConfig.StimulusMode.DUAL_FREQUENCY -> "双频"
            else -> "未知"
        }
    }
    
    /**
     * 获取通道名称
     * @return 通道名称
     */
    fun getChannelName(): String {
        return when (channel) {
            StimulusConfig.Channel.CHANNEL_1 -> "通道1"
            StimulusConfig.Channel.CHANNEL_2 -> "通道2"
            else -> "未知"
        }
    }
    
    override fun toString(): String {
        return "StimulusParams(channel=${getChannelName()}, workTime=${workTimeMinutes}min, " +
                "mode=${getModeName()}, freq=${frequency}Hz, pulseWidth=${pulseWidth}μs, " +
                "intensity=${intensity}mA, rise=${riseTime}s, hold=${holdTime}s, " +
                "fall=${fallTime}s, stop=${stopTime}s, running=$isRunning)"
    }
}

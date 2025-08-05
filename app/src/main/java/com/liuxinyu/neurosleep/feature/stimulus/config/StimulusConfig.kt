package com.liuxinyu.neurosleep.feature.stimulus.config

/**
 * 刺激设备配置类
 * 定义通信协议常量、命令类型、参数范围等配置信息
 * 基于通信协议.md文档
 */
object StimulusConfig {
    
    /**
     * 协议相关常量
     */
    object Protocol {
        // 帧头
        const val FRAME_HEADER_1: Byte = 0x55.toByte()
        const val FRAME_HEADER_2: Byte = 0xBB.toByte()
        
        // 最小帧长度：帧头(2) + 长度(1) + 命令(1) + 校验(1) = 5
        const val MIN_FRAME_LENGTH = 5
        
        // 波特率
        const val BAUD_RATE = 115200
        
        // 扫描超时时间（毫秒）
        const val SCAN_TIMEOUT = 5000L
        
        // 连接超时时间（毫秒）
        const val CONNECT_TIMEOUT = 5000L
        
        // 命令响应超时时间（毫秒）
        const val COMMAND_TIMEOUT = 3000L
    }
    
    /**
     * 命令类型定义
     */
    object CommandType {
        const val DEVICE_STATUS_REPORT: Byte = 0x01      // 下位机上报设备状态
        const val CHANNEL_STATUS_REPORT: Byte = 0x02     // 下位机上报设备当前选定通道号
        const val CHANNEL_SELECT: Byte = 0x03            // 通道选择
        const val WORK_TIME_SET: Byte = 0x04             // 工作时间设置
        const val STIMULUS_CONTROL: Byte = 0x05          // 电刺启动/停止
        const val STIMULUS_MODE_SET: Byte = 0x06         // 电刺激模式设置
        const val FREQUENCY_SET: Byte = 0x07             // 设置电刺激基波频率
        const val PULSE_WIDTH_SET: Byte = 0x08           // 设置电刺激基波脉宽
        const val CURRENT_SET: Byte = 0x09               // 设置电刺激电流强度
        const val RISE_TIME_SET: Byte = 0x0A             // 设置电刺激调制波上升时间
        const val HOLD_TIME_SET: Byte = 0x0B             // 设置电刺激调制波保持时间
        const val FALL_TIME_SET: Byte = 0x0C             // 设置电刺激调制波下降时间
        const val STOP_TIME_SET: Byte = 0x0D             // 设置电刺激调制波停止时间
    }
    
    /**
     * 设备状态定义
     */
    object DeviceStatus {
        const val STANDBY: Byte = 0x00      // 待机
        const val RUNNING: Byte = 0x01      // 运行
        const val PAUSED: Byte = 0x02       // 暂停
    }
    
    /**
     * 通道定义
     */
    object Channel {
        const val CHANNEL_1: Byte = 0x00    // 通道1
        const val CHANNEL_2: Byte = 0x01    // 通道2
    }
    
    /**
     * 刺激控制定义
     */
    object StimulusControl {
        const val START: Byte = 0x00        // 启动
        const val STOP: Byte = 0x01         // 停止
    }
    
    /**
     * 刺激模式定义
     */
    object StimulusMode {
        const val DC: Byte = 0x00           // 直流
        const val RECTANGULAR: Byte = 0x01   // 矩形
        const val DUAL_FREQUENCY: Byte = 0x02 // 双频
    }
    
    /**
     * 刺激参数范围定义
     */
    object StimulusParams {
        // 工作时间（分钟）
        const val MIN_WORK_TIME = 0
        const val MAX_WORK_TIME = 600
        
        // 频率（Hz）
        const val MIN_FREQUENCY = 1
        const val MAX_FREQUENCY = 10000
        
        // 脉宽（微秒）
        const val MIN_PULSE_WIDTH = 50
        const val MAX_PULSE_WIDTH = 1000
        const val PULSE_WIDTH_STEP = 10
        
        // 电流强度（mA）
        const val MIN_INTENSITY = 0.0f
        const val MAX_INTENSITY = 80.0f
        const val INTENSITY_STEP = 0.1f
        
        // 调制波时间（秒）
        const val MIN_DURATION = 0.1f
        const val MAX_DURATION = 60.0f
        const val DURATION_STEP = 0.1f
    }
    
    /**
     * 蓝牙相关配置
     */
    object Bluetooth {
        // 常见的串口蓝牙服务UUID列表
        val SERIAL_SERVICE_UUIDS = arrayOf(
            "0000FFE0-0000-1000-8000-00805F9B34FB", // 常见的串口服务UUID
            "6E400001-B5A3-F393-E0A9-E50E24DCCA9E", // Nordic UART Service
            "49535343-FE7D-4AE5-8FA9-9FAFD205E455", // Microchip串口服务
            "0000FFF0-0000-1000-8000-00805F9B34FB"  // 另一种常见串口服务
        )

        // 常见的串口蓝牙特征值UUID列表
        val SERIAL_CHARACTERISTIC_UUIDS = arrayOf(
            "0000FFE1-0000-1000-8000-00805F9B34FB", // 对应FFE0服务
            "6E400002-B5A3-F393-E0A9-E50E24DCCA9E", // Nordic UART TX
            "6E400003-B5A3-F393-E0A9-E50E24DCCA9E", // Nordic UART RX
            "49535343-1E4D-4BD9-BA61-23C647249616", // Microchip TX
            "49535343-8841-43F4-A8D4-ECBE34729BB3", // Microchip RX
            "0000FFF1-0000-1000-8000-00805F9B34FB", // 对应FFF0服务
            "0000FFF2-0000-1000-8000-00805F9B34FB"  // 对应FFF0服务
        )

        // 设备名称前缀（用于过滤扫描结果）
        const val DEVICE_NAME_PREFIX = "STIMULUS"

        // 最大重连次数
        const val MAX_RECONNECT_ATTEMPTS = 3

        // 重连间隔（毫秒）
        const val RECONNECT_INTERVAL = 2000L

        // 客户端特征配置描述符UUID
        const val CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb"
    }
    
    /**
     * UI相关配置
     */
    object UI {
        // 参数调节步长
        const val FREQUENCY_UI_STEP = 1
        const val PULSE_WIDTH_UI_STEP = 10
        const val INTENSITY_UI_STEP = 0.1f
        const val DURATION_UI_STEP = 0.1f
        
        // 默认参数值
        const val DEFAULT_FREQUENCY = 100
        const val DEFAULT_PULSE_WIDTH = 200
        const val DEFAULT_INTENSITY = 10.0f
        const val DEFAULT_RISE_TIME = 1.0f
        const val DEFAULT_HOLD_TIME = 5.0f
        const val DEFAULT_FALL_TIME = 1.0f
        const val DEFAULT_STOP_TIME = 2.0f
        const val DEFAULT_WORK_TIME = 30
    }
    
    /**
     * 错误码定义
     */
    object ErrorCode {
        const val SUCCESS = 0
        const val BLUETOOTH_NOT_ENABLED = 1001
        const val DEVICE_NOT_FOUND = 1002
        const val CONNECTION_FAILED = 1003
        const val COMMAND_TIMEOUT = 1004
        const val INVALID_RESPONSE = 1005
        const val PARAMETER_OUT_OF_RANGE = 1006
        const val DEVICE_NOT_CONNECTED = 1007
    }
    
    /**
     * 日志标签
     */
    object LogTag {
        const val STIMULUS_BLE_CLIENT = "StimulusBleClient"
        const val STIMULUS_VIEW_MODEL = "StimulusViewModel"
        const val STIMULUS_ACTIVITY = "StimulusActivity"
        const val STIMULUS_COMMAND_UTIL = "StimulusCommandUtil"
    }
}

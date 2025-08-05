package com.liuxinyu.neurosleep.feature.stimulus.callback

import android.bluetooth.BluetoothDevice
import com.liuxinyu.neurosleep.feature.stimulus.model.StimulusResponse

/**
 * 刺激设备蓝牙通信回调接口
 * 定义设备连接、数据通信等关键事件的回调方法
 */
interface StimulusCallback {
    
    /**
     * 设备扫描结果回调
     * @param device 扫描到的蓝牙设备
     * @param rssi 信号强度
     */
    fun onDeviceFound(device: BluetoothDevice, rssi: Int)
    
    /**
     * 扫描开始回调
     */
    fun onScanStarted()
    
    /**
     * 扫描停止回调
     */
    fun onScanStopped()
    
    /**
     * 设备连接成功回调
     * @param device 已连接的设备
     */
    fun onDeviceConnected(device: BluetoothDevice)
    
    /**
     * 设备连接失败回调
     * @param device 连接失败的设备
     * @param errorCode 错误码
     * @param errorMessage 错误信息
     */
    fun onDeviceConnectFailed(device: BluetoothDevice, errorCode: Int, errorMessage: String)
    
    /**
     * 设备断开连接回调
     * @param device 断开连接的设备
     */
    fun onDeviceDisconnected(device: BluetoothDevice)
    
    /**
     * 连接状态变化回调
     * @param isConnected 是否已连接
     */
    fun onConnectionStateChanged(isConnected: Boolean)
    
    /**
     * 接收到设备响应回调
     * @param response 设备响应数据
     */
    fun onResponseReceived(response: StimulusResponse)
    
    /**
     * 命令发送成功回调
     * @param commandType 命令类型
     * @param data 发送的数据
     */
    fun onCommandSent(commandType: Byte, data: ByteArray)
    
    /**
     * 命令发送失败回调
     * @param commandType 命令类型
     * @param data 发送的数据
     * @param errorCode 错误码
     * @param errorMessage 错误信息
     */
    fun onCommandSendFailed(commandType: Byte, data: ByteArray, errorCode: Int, errorMessage: String)
    
    /**
     * 命令超时回调
     * @param commandType 超时的命令类型
     */
    fun onCommandTimeout(commandType: Byte)
    
    /**
     * 设备状态变化回调
     * @param deviceStatus 设备状态
     */
    fun onDeviceStatusChanged(deviceStatus: Byte)
    
    /**
     * 通道状态变化回调
     * @param channel 当前通道
     */
    fun onChannelChanged(channel: Byte)
    
    /**
     * 错误回调
     * @param errorCode 错误码
     * @param errorMessage 错误信息
     */
    fun onError(errorCode: Int, errorMessage: String)
}

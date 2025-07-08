package com.liuxinyu.neurosleep.core.ble

import java.util.UUID

object BleConfig {
    // 先保存一组 UUID
    // default config
    val DEFAULT_CLIENT : UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    // Service UUID
    val SERVICE_FIR : UUID = UUID.fromString("6e40ffe0-b5a3-f393-e0a9-e50e24dcca9e") // cover 2A37 and 2A38
    val SERVICE_SEC : UUID = UUID.fromString("6e40ffe1-b5a3-f393-e0a9-e50e24dcca9e")
    // Characteristic UUID
    val CHARA_CTRL : UUID = UUID.fromString("6e402a37-b5a3-f393-e0a9-e50e24dcca9e") // 2A37 负责从上位机接收命令
    val CHARA_INFO : UUID = UUID.fromString("6e402a38-b5a3-f393-e0a9-e50e24dcca9e") // 2A38 负责心电以外的数据应答
    val CHARA_DATA : UUID = UUID.fromString("6e402a39-b5a3-f393-e0a9-e50e24dcca9e") // 2A39 负责心电数据发送
}
package com.liuxinyu.neurosleep.util

object SampleGattAttr {
    var CLIENT_CHARACTERISTIC_CONFIG: String = "00002902-0000-1000-8000-00805f9b34fb"
    var HEART_RATE_ECGDATA: String = "6e400003-b5a3-f393-e0a9-e50e24dcca9e"
    var HEART_RATE_MEASUREMENT: String = "6e400001-b5a3-f393-e0a9-e50e24dcca9e"
    var HEART_RATE_RESULT: String = "6e400002-b5a3-f393-e0a9-e50e24dcca9e"
    private val attributes = HashMap<String, String>()

    init {
        attributes[HEART_RATE_MEASUREMENT] = "Heart Rate Measurement"
        attributes["00002902-0000-1000-8000-00805f9b34fb"] = "Manufacturer Name String"
    }

    fun lookup(str: String, str2: String): String {
        val str3 = attributes[str]
        return str3 ?: str2
    }
}

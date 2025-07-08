package com.liuxinyu.neurosleep.util

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object TimeUtil {
    // 设置时区为中国大陆
    fun getCurrentDateTime(): String? {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd-hh-mm-ss").format(LocalDateTime.now())
    }

    fun getFormattedDateTime(): FormattedTime {
        // 获取当前的本地日期时间
        val localDateTime: LocalDateTime = LocalDateTime.now()
        // 将其转换为指定时区的 ZonedDateTime
        val zonedDateTime: ZonedDateTime = localDateTime.atZone(ZoneId.of("Asia/Shanghai"))
        return FormattedTime(
            zonedDateTime.year,zonedDateTime.monthValue,zonedDateTime.dayOfMonth,
            zonedDateTime.hour,zonedDateTime.minute,zonedDateTime.second
        )
    }

}
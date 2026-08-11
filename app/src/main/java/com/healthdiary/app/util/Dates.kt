package com.healthdiary.app.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

object Dates {
    fun today(): String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

    fun formatChinese(date: String): String =
        runCatching {
            LocalDate.parse(date).format(DateTimeFormatter.ofPattern("yyyy年M月d日"))
        }.getOrDefault(date)

    fun formatWeekday(date: String): String =
        runCatching {
            LocalDate.parse(date).dayOfWeek.getDisplayName(TextStyle.FULL, Locale.CHINA)
        }.getOrDefault("")

    fun formatTime(ts: Long): String =
        Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("HH:mm"))

    fun formatDuration(millis: Long): String {
        val totalSec = millis / 1000
        return if (totalSec >= 3600) {
            "${totalSec / 3600}小时${(totalSec % 3600) / 60}分"
        } else if (totalSec < 60) {
            "${totalSec}秒"
        } else {
            "${totalSec / 60}分${totalSec % 60}秒"
        }
    }
}

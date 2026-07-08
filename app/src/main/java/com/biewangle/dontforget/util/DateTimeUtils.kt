package com.biewangle.dontforget.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DateTimeUtils {

    private val dateFormat = SimpleDateFormat("M月d日", Locale.CHINESE)
    private val fullDateFormat = SimpleDateFormat("yyyy年M月d日", Locale.CHINESE)
    private val weekdayFormat = SimpleDateFormat("EEEE", Locale.CHINESE)
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.CHINESE)

    /** 格式化日期：如 "7月5日" */
    fun formatDate(timestamp: Long): String = dateFormat.format(Date(timestamp))

    /** 格式化完整日期：如 "2024年7月5日" */
    fun formatFullDate(timestamp: Long): String = fullDateFormat.format(Date(timestamp))

    /** 格式化时间：如 "08:00" */
    fun formatTime(timestamp: Long): String = timeFormat.format(Date(timestamp))

    /** 格式化星期几 */
    fun formatWeekday(timestamp: Long): String = weekdayFormat.format(Date(timestamp))

    /** 日期分组标签：如 "今天 7月5日 星期三" */
    fun formatDateHeader(timestamp: Long): String {
        val cal = Calendar.getInstance()
        val today = getStartOfDay(cal.timeInMillis)
        val tomorrow = today + 24 * 60 * 60 * 1000L
        val targetDay = getStartOfDay(timestamp)

        val prefix = when (targetDay) {
            today -> "今天"
            tomorrow -> "明天"
            else -> ""
        }

        val dateStr = formatDate(timestamp)
        val weekday = formatWeekday(timestamp)
        return if (prefix.isNotEmpty()) "$prefix  $dateStr  $weekday" else "$dateStr  $weekday"
    }

    /** 获取当天0点的时间戳 */
    fun getStartOfDay(timestamp: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /** 获取当天23:59:59的时间戳 */
    fun getEndOfDay(timestamp: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }

    /**
     * 获取 UTC 当天0点的时间戳。
     * Material 3 DatePicker 使用 UTC 零点表示日期，
     * 初始化 DatePickerState 时必须传 UTC 零点，否则高亮会偏一天。
     */
    fun getUtcStartOfDay(timestamp: Long): Long {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.timeInMillis = timestamp
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /**
     * 将任意时间戳转为对应日历日期的 UTC 零点毫秒值。
     * 用于兼容旧数据（本地零点存储）和新数据（UTC 零点存储），
     * 确保传给 DatePicker 的初始值始终是 UTC 零点。
     */
    fun toUtcMidnight(timestamp: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)
        val day = cal.get(Calendar.DAY_OF_MONTH)

        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        utcCal.clear()
        utcCal.set(year, month, day)
        return utcCal.timeInMillis
    }

    /** 根据日期和时分创建时间戳 */
    fun createTimestamp(dayMillis: Long, hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = getStartOfDay(dayMillis)
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /** 计算下一次重复的时间 */
    fun calculateNextOccurrence(timestamp: Long, repeatMode: String): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        return when (repeatMode) {
            "DAILY" -> {
                cal.add(Calendar.DAY_OF_YEAR, 1)
                cal.timeInMillis
            }
            "WEEKLY" -> {
                cal.add(Calendar.WEEK_OF_YEAR, 1)
                cal.timeInMillis
            }
            "MONTHLY" -> {
                cal.add(Calendar.MONTH, 1)
                cal.timeInMillis
            }
            else -> timestamp
        }
    }

    /** 格式化时间段：如 "早上 8:00" */
    fun formatReminderTime(timestamp: Long): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)

        val period = when (hour) {
            in 0..5 -> "凌晨"
            in 6..8 -> "早上"
            in 9..11 -> "上午"
            in 12..13 -> "中午"
            in 14..17 -> "下午"
            else -> "晚上"
        }

        return "$period ${hour}:${minute.toString().padStart(2, '0')}"
    }

    fun now(): Long = System.currentTimeMillis()
}

package com.biewangle.dontforget.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.biewangle.dontforget.util.Constants

class AlarmScheduler(private val context: Context) {

    fun schedule(memoId: Long, title: String, content: String, alarmTime: Long, useCustomRingtone: Boolean = true) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = Constants.ACTION_SHOW_REMINDER
            putExtra(Constants.EXTRA_MEMO_ID, memoId)
            putExtra(Constants.EXTRA_MEMO_TITLE, title)
            putExtra(Constants.EXTRA_MEMO_CONTENT, content)
            putExtra(Constants.EXTRA_USE_CUSTOM_RINGTONE, useCustomRingtone)
        }

        val requestCode = memoId.toInt()
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // setAlarmClock 是国产手机最尊重的方式（状态栏显示闹钟图标）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(alarmTime, pendingIntent),
                pendingIntent
            )
        } else {
            @Suppress("DEPRECATION")
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, alarmTime, pendingIntent)
        }
    }

    fun cancel(memoId: Long) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = Constants.ACTION_SHOW_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, memoId.toInt(), intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(it)
            it.cancel()
        }
    }
}

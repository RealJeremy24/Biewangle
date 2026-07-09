package com.biewangle.dontforget.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.biewangle.dontforget.R
import com.biewangle.dontforget.ui.screens.alarm.AlarmActivity
import com.biewangle.dontforget.util.Constants
import com.biewangle.dontforget.util.NotificationBitmapBuilder

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val memoId = intent.getLongExtra(Constants.EXTRA_MEMO_ID, -1L)
        if (memoId == -1L) return

        Log.d(Constants.LOG_TAG, "Receiver触发, action=${intent.action}, memoId=$memoId")

        when (intent.action) {
            Constants.ACTION_DISMISS -> {
                // 关闭：停止铃声 + 取消通知
                AlarmForegroundService.stop(context)
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.cancel(memoId.toInt())
            }
            Constants.ACTION_SNOOZE -> {
                // 稍后提醒：停止铃声 + 取消通知 + 重新调度
                AlarmForegroundService.stop(context)
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.cancel(memoId.toInt())

                val title = intent.getStringExtra(Constants.EXTRA_MEMO_TITLE) ?: "提醒"
                val content = intent.getStringExtra(Constants.EXTRA_MEMO_CONTENT) ?: ""
                val snoozeTime = System.currentTimeMillis() + Constants.SNOOZE_DELAY_MS
                AlarmScheduler(context).schedule(memoId, title, content, snoozeTime, true)
            }
            else -> {
                // 闹钟触发
                val title = intent.getStringExtra(Constants.EXTRA_MEMO_TITLE) ?: "提醒"
                val content = intent.getStringExtra(Constants.EXTRA_MEMO_CONTENT) ?: ""
                val useCustomRingtone = intent.getBooleanExtra(Constants.EXTRA_USE_CUSTOM_RINGTONE, true)

                // 1. 通知已移至前台服务（避免两个通知同时出现）
                //    前台服务会带 BigPictureStyle 贴纸大图通知

                // 2. 主动启动全屏 AlarmActivity（解决国产 ROM 拦截 FullScreenIntent 的问题）
                //    从 BroadcastReceiver.onReceive 启动 Activity 是 Google 文档明确允许的路径，
                //    不受 Android 10+ 后台启动 Activity 限制影响
                try {
                    val activityIntent = Intent(context, AlarmActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP
                        putExtra(Constants.EXTRA_MEMO_ID, memoId)
                        putExtra(Constants.EXTRA_MEMO_TITLE, title)
                        putExtra(Constants.EXTRA_MEMO_CONTENT, content)
                        putExtra(Constants.EXTRA_USE_CUSTOM_RINGTONE, useCustomRingtone)
                    }
                    context.startActivity(activityIntent)
                    Log.d(Constants.LOG_TAG, "Receiver.startActivity 成功")
                } catch (e: Exception) {
                    Log.e(Constants.LOG_TAG, "Receiver.startActivity 失败", e)
                }

                // 3. 启动前台服务播放铃声（Android 12+ 后台可能失败，通知已作为降级）
                try {
                    AlarmForegroundService.start(context, memoId, title, content, useCustomRingtone)
                    Log.d(Constants.LOG_TAG, "前台服务启动命令已发出")
                } catch (e: Exception) {
                    Log.e(Constants.LOG_TAG, "前台服务启动失败（Android 12+ 后台限制 / HarmonyOS 加固）", e)
                }
            }
        }
    }

    private fun showFullScreenNotification(
        context: Context,
        memoId: Long,
        title: String,
        content: String,
        useCustomRingtone: Boolean
    ) {
        // 主操作 Intent —— 点击通知正文进入 AlarmActivity
        val fullScreenIntent = Intent(context, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(Constants.EXTRA_MEMO_ID, memoId)
            putExtra(Constants.EXTRA_MEMO_TITLE, title)
            putExtra(Constants.EXTRA_MEMO_CONTENT, content)
            putExtra(Constants.EXTRA_USE_CUSTOM_RINGTONE, useCustomRingtone)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context, memoId.toInt(), fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 关闭 Action
        val dismissIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = Constants.ACTION_DISMISS
            putExtra(Constants.EXTRA_MEMO_ID, memoId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context, (memoId.toInt() + 10000), dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 稍后提醒 Action
        val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = Constants.ACTION_SNOOZE
            putExtra(Constants.EXTRA_MEMO_ID, memoId)
            putExtra(Constants.EXTRA_MEMO_TITLE, title)
            putExtra(Constants.EXTRA_MEMO_CONTENT, content)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context, (memoId.toInt() + 20000), snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 锁屏大图通知：Canvas 合成妈妈贴纸 + 动态文字 → BigPictureStyle
        // - 绿色渐变背景 + 左侧圆形贴纸（mama3）+ 右侧标题/正文
        // - VISIBILITY_PUBLIC: 锁屏可见完整内容
        // - PRIORITY_MAX + CATEGORY_ALARM: 重要级别,横幅 + 铃声 + 震动
        val pictureBitmap = NotificationBitmapBuilder.build(context, title, content)
        val pictureStyle = NotificationCompat.BigPictureStyle()
            .bigPicture(pictureBitmap)
            .setBigContentTitle(title)
            .setSummaryText(content)

        val notification = NotificationCompat.Builder(context, Constants.CHANNEL_REMINDER)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(pictureStyle)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setShowWhen(true)
            .setFullScreenIntent(fullScreenPendingIntent, true)  // 兜底:部分 ROM 可能支持
            .setContentIntent(fullScreenPendingIntent)            // 主操作:点击通知正文进全屏
            .setOngoing(true)
            .setAutoCancel(false)
            .setTimeoutAfter(0)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "关闭", dismissPendingIntent)
            .addAction(android.R.drawable.ic_media_pause, "稍后提醒", snoozePendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(memoId.toInt(), notification)
    }
}

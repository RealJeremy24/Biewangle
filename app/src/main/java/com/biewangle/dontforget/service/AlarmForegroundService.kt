package com.biewangle.dontforget.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.biewangle.dontforget.MainActivity
import com.biewangle.dontforget.R
import com.biewangle.dontforget.ui.screens.alarm.AlarmActivity
import com.biewangle.dontforget.util.Constants

class AlarmForegroundService : Service() {

    private lateinit var player: ReminderPlayer
    private var isPlaying = false

    override fun onCreate() {
        super.onCreate()
        player = ReminderPlayer(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val memoId = intent?.getLongExtra(Constants.EXTRA_MEMO_ID, -1L) ?: -1L
        val title = intent?.getStringExtra(Constants.EXTRA_MEMO_TITLE) ?: "提醒"
        val content = intent?.getStringExtra(Constants.EXTRA_MEMO_CONTENT) ?: ""
        val useCustomRingtone = intent?.getBooleanExtra(Constants.EXTRA_USE_CUSTOM_RINGTONE, true) ?: true

        Log.d(Constants.LOG_TAG, "Service.onStartCommand 触发, memoId=$memoId")

        // WakeLock 唤醒屏幕 —— 锁屏状态下也能点亮屏幕并显示全屏 Activity
        // AlarmActivity 已经配置了 showOnLockScreen + turnScreenOn,但部分 ROM 仍会拦截,
        // 这里用 WakeLock 强制唤醒确保屏幕能亮起来
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            val wakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "Biewangle:AlarmWakeLock"
            )
            wakeLock.acquire(15_000L)  // 15 秒后自动释放,足够 Activity 启动并显示
            Log.d(Constants.LOG_TAG, "WakeLock 已获取")
        } catch (e: Exception) {
            Log.e(Constants.LOG_TAG, "WakeLock 获取失败", e)
        }

        startForeground(Constants.NOTIFICATION_ID_ALARM_SERVICE, createNotification(title, content))

        // 防止重复播放：如果已经在播放就不重新开始
        if (!isPlaying) {
            player.startLooping(useCustomRingtone)
            isPlaying = true
        }

        // 从前台服务启动 AlarmActivity —— 前台服务拥有"前台例外"权限，
        // 国产 ROM（尤其 HarmonyOS/MIUI）会拦截 Receiver 内直接 startActivity，
        // 但不会拦截前台服务内的 startActivity
        if (memoId != -1L) {
            try {
                val activityIntent = Intent(this, AlarmActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    putExtra(Constants.EXTRA_MEMO_ID, memoId)
                    putExtra(Constants.EXTRA_MEMO_TITLE, title)
                    putExtra(Constants.EXTRA_MEMO_CONTENT, content)
                    putExtra(Constants.EXTRA_USE_CUSTOM_RINGTONE, useCustomRingtone)
                }
                startActivity(activityIntent)
                Log.d(Constants.LOG_TAG, "Service.startActivity 成功")
            } catch (e: Exception) {
                Log.e(Constants.LOG_TAG, "Service.startActivity 失败", e)
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        player.stop()
        isPlaying = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(title: String, content: String): Notification {
        val appIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, Constants.CHANNEL_SERVICE)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("🔔 $title")
            .setContentText(content)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    companion object {
        fun start(context: android.content.Context, memoId: Long, title: String, content: String, useCustomRingtone: Boolean = true) {
            val intent = Intent(context, AlarmForegroundService::class.java).apply {
                putExtra(Constants.EXTRA_MEMO_ID, memoId)
                putExtra(Constants.EXTRA_MEMO_TITLE, title)
                putExtra(Constants.EXTRA_MEMO_CONTENT, content)
                putExtra(Constants.EXTRA_USE_CUSTOM_RINGTONE, useCustomRingtone)
            }
            context.startForegroundService(intent)
        }

        fun stop(context: android.content.Context) {
            val intent = Intent(context, AlarmForegroundService::class.java)
            context.stopService(intent)
        }
    }
}

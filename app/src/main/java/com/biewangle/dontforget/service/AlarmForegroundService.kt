package com.biewangle.dontforget.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.biewangle.dontforget.MainActivity
import com.biewangle.dontforget.R
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

        startForeground(Constants.NOTIFICATION_ID_ALARM_SERVICE, createNotification(title, content))

        // 防止重复播放：如果已经在播放就不重新开始
        if (!isPlaying) {
            player.startLooping(useCustomRingtone)
            isPlaying = true
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

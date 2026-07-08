package com.biewangle.dontforget

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.biewangle.dontforget.data.db.AppDatabase
import com.biewangle.dontforget.data.repository.MemoRepository
import com.biewangle.dontforget.data.repository.SettingsRepository
import com.biewangle.dontforget.util.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BiewangleApp : Application() {

    // 手动依赖注入（Service Locator 模式，避免 Hilt 复杂性）
    val database by lazy { AppDatabase.getInstance(this) }
    val memoRepository by lazy { MemoRepository(database.memoDao()) }
    val settingsRepository by lazy { SettingsRepository(database.settingsDao()) }

    // 全局字体缩放（响应式，设置页修改后整个 App 自动更新）
    // 默认档位 2（缩放 0.78f）
    private val _fontScaleFlow = MutableStateFlow(0.78f)
    val fontScaleFlow: StateFlow<Float> = _fontScaleFlow.asStateFlow()

    fun updateFontScale(scale: Float) {
        _fontScaleFlow.value = scale
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannels()

        // 从数据库异步加载初始字体缩放值，避免阻塞主线程
        CoroutineScope(Dispatchers.IO).launch {
            val saved = settingsRepository.getFontScale()
            _fontScaleFlow.value = saved
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = listOf(
                NotificationChannel(
                    Constants.CHANNEL_REMINDER,
                    getString(R.string.notification_channel_reminder),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "用于显示提醒通知"
                    setShowBadge(true)
                    enableVibration(true)   // 震动作为降级路径（前台服务可能无法启动）
                    setBypassDnd(true)
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC  // 锁屏显示完整内容
                    // 不设置 sound = null，让通知使用系统默认闹钟铃声作为降级
                    // 前台服务 MediaPlayer 启动后会覆盖此声音
                },
                NotificationChannel(
                    Constants.CHANNEL_SERVICE,
                    getString(R.string.notification_channel_service),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "提醒前台服务运行中"
                    setShowBadge(false)
                }
            )

            val manager = getSystemService(NotificationManager::class.java)
            channels.forEach { manager.createNotificationChannel(it) }
        }
    }

    companion object {
        lateinit var instance: BiewangleApp
            private set
    }
}

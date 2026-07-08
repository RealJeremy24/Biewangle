package com.biewangle.dontforget.service

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import com.biewangle.dontforget.BiewangleApp
import com.biewangle.dontforget.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReminderPlayer(private val context: Context) {

    // 协程作用域：生命周期绑定到 ReminderPlayer 实例，stop() 时取消避免泄漏
    private var scopeJob = Job()
    private val playerScope = CoroutineScope(scopeJob + Dispatchers.IO)

    private var mediaPlayer: MediaPlayer? = null
    private var loopHandler: android.os.Handler? = null
    private var loopRunnable: Runnable? = null
    private var trimStartMs: Long = 0L
    private var trimEndMs: Long = 30000L
    private var isLooping = false
    private var currentUri: Uri? = null

    // 震动器
    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }
    private var isVibrating = false

    fun startLooping(useCustomRingtone: Boolean = true) {
        if (useCustomRingtone) {
            // 异步加载铃声配置，避免 runBlocking 阻塞主线程
            playerScope.launch {
                val config = BiewangleApp.instance.settingsRepository.getRingtoneConfig()

                val uri = if (config.uri.isNotEmpty()) {
                    Uri.parse(config.uri)
                } else {
                    // 使用 App 内置默认铃声
                    Uri.parse("android.resource://${context.packageName}/${R.raw.biewangle}")
                }

                val startMs = config.trimStartMs
                val endMs = if (config.trimEndMs > 0) config.trimEndMs else 30000L

                trimStartMs = startMs
                trimEndMs = endMs

                withContext(Dispatchers.Main) {
                    startPlayback(uri, loop = true)
                    startVibration()
                }
            }
        } else {
            // 使用系统默认闹钟铃声，无需读数据库，同步启动即可
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: Settings.System.DEFAULT_ALARM_ALERT_URI
            trimStartMs = 0L
            trimEndMs = 0L
            startPlayback(uri, loop = true)
            startVibration()
        }
    }

    fun startPreview(uri: Uri, startMs: Long, endMs: Long) {
        trimStartMs = startMs
        trimEndMs = endMs
        startPlayback(uri, loop = true)
    }

    private fun startPlayback(uri: Uri, loop: Boolean) {
        stop()

        currentUri = uri
        isLooping = loop

        // 判断是否使用了裁剪（trimStart > 0 或 trimEnd 不是默认值）
        val isTrimmed = trimStartMs > 0 || (trimEndMs > 0 && trimEndMs < 30000L)

        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(context, uri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setVolume(1.0f, 1.0f)

                setOnPreparedListener { mp ->
                    if (trimStartMs > 0) {
                        mp.seekTo(trimStartMs.toInt())
                    }

                    if (loop) {
                        if (isTrimmed) {
                            // 裁剪模式：用 OnCompletionListener + 轮询实现片段循环
                            setupTrimmedLooping()
                        } else {
                            // 完整文件循环：直接用系统 API，无缝循环
                            mp.isLooping = true
                        }
                    }

                    mp.start()
                }
                prepareAsync()
            } catch (e: Exception) {
                e.printStackTrace()
                // 降级到系统默认闹钟铃声
                try {
                    reset()
                    setDataSource(
                        context,
                        Settings.System.DEFAULT_ALARM_ALERT_URI
                    )
                    prepare()
                    start()
                } catch (e2: Exception) {
                    e2.printStackTrace()
                }
            }
        }
    }

    private fun startVibration() {
        // 异步读取震动设置，避免 runBlocking 阻塞主线程
        playerScope.launch {
            val vibrateEnabled = BiewangleApp.instance.settingsRepository.getVibrateEnabled()
            if (!vibrateEnabled) return@launch
            withContext(Dispatchers.Main) { doStartVibration() }
        }
    }

    /** 必须在主线程调用（Vibrator 操作） */
    private fun doStartVibration() {

        try {
            // 循环震动模式：震动 500ms + 暂停 500ms
            val pattern = longArrayOf(0, 500, 500)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createWaveform(pattern, 0)
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, 0)
            }
            isVibrating = true
        } catch (_: Exception) {}
    }

    private fun stopVibration() {
        if (isVibrating) {
            try {
                vibrator.cancel()
            } catch (_: Exception) {}
            isVibrating = false
        }
    }

    /**
     * 裁剪片段的循环播放：
     * - OnCompletionListener：处理音频自然播完的情况（音频短于 trimEndMs 时也能正确回到起点）
     * - 轮询检查：处理音频长于 trimEndMs 时提前 seek 回 trimStartMs
     */
    private fun setupTrimmedLooping() {
        val mp = mediaPlayer ?: return

        // 音频自然结束时回到 trimStartMs 重新播放
        mp.setOnCompletionListener { mpp ->
            try {
                mpp.seekTo(trimStartMs.toInt())
                mpp.start()
            } catch (_: Exception) {}
        }

        // 同时轮询：如果音频较长，在到达 trimEndMs 时提前切回
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        loopHandler = handler

        loopRunnable = object : Runnable {
            override fun run() {
                val player = mediaPlayer ?: return
                try {
                    if (player.isPlaying && player.currentPosition >= trimEndMs) {
                        player.seekTo(trimStartMs.toInt())
                    }
                } catch (_: Exception) {}
                handler.postDelayed(this, 500)
            }
        }
        handler.post(loopRunnable!!)
    }

    fun stop() {
        // 取消所有正在执行的协程（避免设置读取尚未完成时泄漏）
        scopeJob.cancel()
        scopeJob = Job()
        stopVibration()

        loopHandler?.removeCallbacks(loopRunnable ?: return)
        loopHandler = null
        loopRunnable = null

        mediaPlayer?.apply {
            try {
                if (isPlaying) stop()
                release()
            } catch (_: Exception) {}
        }
        mediaPlayer = null
        isLooping = false
    }
}

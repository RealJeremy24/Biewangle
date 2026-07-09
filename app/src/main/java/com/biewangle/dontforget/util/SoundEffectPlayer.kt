package com.biewangle.dontforget.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper

/**
 * 按键音效播放器。
 *
 * 按钮点击 → [AudioManager.FX_KEY_CLICK]（系统 UI 音效）
 * 时间拨动 → [AudioManager.FX_FOCUS_NAVIGATION_UP]
 * 日期选择 → [AudioManager.FX_FOCUS_NAVIGATION_RIGHT]
 * 开关切换 → [ToneGenerator.TONE_DTMF_0]（短促咔嗒声，代码生成）
 * 滑块步进 → [ToneGenerator.TONE_PROP_ACK]（轻柔确认音，代码生成）
 */
object SoundEffectPlayer {

    /**
     * 播放按钮点击音效（清脆的点击声）
     */
    fun playButtonClick(context: Context) {
        play(context, AudioManager.FX_KEY_CLICK)
    }

    /**
     * 播放时间拨动音效（较轻的嘀嗒声）
     */
    fun playTimeScroll(context: Context) {
        play(context, AudioManager.FX_FOCUS_NAVIGATION_UP)
    }

    /**
     * 播放日期点击音效（轻巧的方向导航音，与滚轮音同系列但可区分）
     */
    fun playDateClick(context: Context) {
        play(context, AudioManager.FX_FOCUS_NAVIGATION_RIGHT)
    }

    /**
     * 播放开关切换音效（短促咔嗒声），用 [ToneGenerator] 生成。
     * 用于设置页 Switch 控件。
     */
    fun playSwitchToggle() {
        playTone(ToneGenerator.TONE_DTMF_0, 50, 40)
    }

    /**
     * 播放滑块步进音效（轻柔确认音），用 [ToneGenerator] 生成。
     * 用于设置页 Slider 档位变化时，与 [playSwitchToggle] 可区分。
     */
    fun playSliderStep() {
        playTone(ToneGenerator.TONE_PROP_ACK, 80, 25)
    }

    // ── 内部实现 ──

    private fun play(context: Context, effectType: Int) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        audioManager.playSoundEffect(effectType)
    }

    /**
     * 用 [ToneGenerator] 播放短促提示音，播完后自动释放。
     */
    @Suppress("DEPRECATION")
    private fun playTone(toneType: Int, durationMs: Int, volume: Int) {
        val tg = ToneGenerator(AudioManager.STREAM_SYSTEM, volume)
        tg.startTone(toneType, durationMs)
        // 延迟释放，确保短音播放完毕
        Handler(Looper.getMainLooper()).postDelayed({
            runCatching { tg.release() }
        }, durationMs + 20L)
    }
}

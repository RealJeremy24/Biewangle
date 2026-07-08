package com.biewangle.dontforget.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build

/**
 * 按键音效播放器，用系统内置 AudioManager.playSoundEffect() 播放短促 UI 音效。
 *
 * 按钮点击 → [AudioManager.FX_KEY_CLICK]
 * 时间拨动 → [AudioManager.FX_FOCUS_NAVIGATION_UP]
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

    private fun play(context: Context, effectType: Int) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        audioManager.playSoundEffect(effectType)
    }
}

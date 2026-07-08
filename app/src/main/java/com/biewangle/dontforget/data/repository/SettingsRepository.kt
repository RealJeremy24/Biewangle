package com.biewangle.dontforget.data.repository

import com.biewangle.dontforget.data.dao.SettingsDao
import com.biewangle.dontforget.data.entity.SettingsEntity
import com.biewangle.dontforget.data.model.RingtoneConfig
import com.biewangle.dontforget.util.Constants

class SettingsRepository(private val settingsDao: SettingsDao) {

    suspend fun getRingtoneConfig(): RingtoneConfig {
        val uri = settingsDao.getValue(Constants.KEY_RINGTONE_URI) ?: ""
        val displayName = settingsDao.getValue(Constants.KEY_RINGTONE_DISPLAY_NAME) ?: "默认铃声"
        val startMs = settingsDao.getValue(Constants.KEY_RINGTONE_START_MS)?.toLongOrNull() ?: 0L
        val endMs = settingsDao.getValue(Constants.KEY_RINGTONE_END_MS)?.toLongOrNull()
            ?: Constants.DEFAULT_RINGTONE_DURATION_MS
        return RingtoneConfig(uri, displayName, startMs, endMs)
    }

    suspend fun saveRingtoneConfig(config: RingtoneConfig) {
        settingsDao.insert(SettingsEntity(Constants.KEY_RINGTONE_URI, config.uri))
        settingsDao.insert(SettingsEntity(Constants.KEY_RINGTONE_DISPLAY_NAME, config.displayName))
        settingsDao.insert(SettingsEntity(Constants.KEY_RINGTONE_START_MS, config.trimStartMs.toString()))
        settingsDao.insert(SettingsEntity(Constants.KEY_RINGTONE_END_MS, config.trimEndMs.toString()))
    }

    // ── 震动开关 ──
    suspend fun getVibrateEnabled(): Boolean {
        return settingsDao.getValue(Constants.KEY_VIBRATE_ENABLED)?.toBoolean() ?: true
    }

    suspend fun setVibrateEnabled(enabled: Boolean) {
        settingsDao.insert(SettingsEntity(Constants.KEY_VIBRATE_ENABLED, enabled.toString()))
    }

    // ── 字体缩放（0.8 ~ 1.5，10 档） ──
    suspend fun getFontScale(): Float {
        val raw = settingsDao.getValue(Constants.KEY_FONT_SCALE)
        if (raw != null) {
            return raw.toFloatOrNull() ?: 1.0f
        }
        // 向后兼容：从旧 KEY_LARGE_TEXT_MODE 迁移
        val oldMode = settingsDao.getValue(Constants.KEY_LARGE_TEXT_MODE)?.toBoolean() ?: false
        return if (oldMode) 1.2f else 1.0f
    }

    suspend fun setFontScale(scale: Float) {
        settingsDao.insert(SettingsEntity(Constants.KEY_FONT_SCALE, scale.toString()))
    }

    // ── 旧版：保留兼容 ──
    suspend fun getLargeTextMode(): Boolean {
        return getFontScale() > 1.0f
    }

    suspend fun setLargeTextMode(enabled: Boolean) {
        setFontScale(if (enabled) 1.2f else 1.0f)
    }
}

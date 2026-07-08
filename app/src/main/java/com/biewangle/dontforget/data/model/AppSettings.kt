package com.biewangle.dontforget.data.model

data class RingtoneConfig(
    val uri: String = "",
    val displayName: String = "默认铃声",
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = 30000L
) {
    val isCustom: Boolean get() = uri.isNotEmpty()
    val durationMs: Long get() = trimEndMs - trimStartMs
}

data class AppSettings(
    val ringtone: RingtoneConfig = RingtoneConfig(),
    val largeTextMode: Boolean = false
)

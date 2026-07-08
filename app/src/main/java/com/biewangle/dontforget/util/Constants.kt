package com.biewangle.dontforget.util

object Constants {
    // 通知渠道
    const val CHANNEL_REMINDER = "reminder_channel"
    const val CHANNEL_SERVICE = "reminder_service_channel"

    // 通知ID
    const val NOTIFICATION_ID_ALARM_SERVICE = 1001

    // 调试日志 tag
    const val LOG_TAG = "BiewangleAlarm"

    // Intent Action
    const val ACTION_SHOW_REMINDER = "com.biewangle.dontforget.ACTION_SHOW_REMINDER"
    const val ACTION_DISMISS = "com.biewangle.dontforget.ACTION_DISMISS"
    const val ACTION_SNOOZE = "com.biewangle.dontforget.ACTION_SNOOZE"

    // Intent Extra Keys
    const val EXTRA_MEMO_ID = "memo_id"
    const val EXTRA_MEMO_TITLE = "memo_title"
    const val EXTRA_MEMO_CONTENT = "memo_content"
    const val EXTRA_USE_CUSTOM_RINGTONE = "use_custom_ringtone"

    // Settings Keys
    const val KEY_RINGTONE_URI = "ringtone_uri"
    const val KEY_RINGTONE_DISPLAY_NAME = "ringtone_display_name"
    const val KEY_RINGTONE_START_MS = "ringtone_start_ms"
    const val KEY_RINGTONE_END_MS = "ringtone_end_ms"
    const val KEY_LARGE_TEXT_MODE = "large_text_mode"
    const val KEY_VIBRATE_ENABLED = "vibrate_enabled"
    const val KEY_FONT_SCALE = "font_scale"
    const val KEY_AUTO_START_GUIDE_SHOWN = "auto_start_guide_shown"

    // 快捷模板
    val QUICK_TEMPLATES = listOf(
        TemplateItem("package", "📦 拿快递", "记得去拿快递"),
        TemplateItem("shopping", "🛍️ 买东西", "记得去买东西"),
        TemplateItem("call", "📞 打电话", "记得打电话"),
        TemplateItem("exercise", "🚶 去锻炼", "出门锻炼身体")
    )

    // 默认铃声时长（30秒）
    const val DEFAULT_RINGTONE_DURATION_MS = 30000L

    // 稍后提醒间隔（10分钟）
    const val SNOOZE_DELAY_MS = 10 * 60 * 1000L
}

data class TemplateItem(
    val id: String,
    val label: String,
    val defaultContent: String
)

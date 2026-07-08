package com.biewangle.dontforget.data.model

data class Memo(
    val id: Long = 0,
    val title: String = "",
    val content: String = "",
    val targetDate: Long,           // 目标日期（当天0点时间戳）
    val reminderTime: Long? = null, // 提醒时间戳
    val repeatType: RepeatType = RepeatType.NONE,
    val templateId: String? = null, // 模板ID
    val useCustomRingtone: Boolean = true, // 是否使用自定义提醒铃声
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /** 是否设置了提醒 */
    val hasReminder: Boolean get() = reminderTime != null

    /** 格式化后的提醒时间展示 */
    fun formattedReminderTime(): String? {
        return reminderTime?.let { com.biewangle.dontforget.util.DateTimeUtils.formatReminderTime(it) }
    }
}

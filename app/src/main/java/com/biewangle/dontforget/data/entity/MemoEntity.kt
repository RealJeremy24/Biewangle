package com.biewangle.dontforget.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.biewangle.dontforget.data.model.Memo
import com.biewangle.dontforget.data.model.RepeatType

@Entity(
    tableName = "memos",
    indices = [
        Index(value = ["target_date"]),
        Index(value = ["is_completed"]),
        Index(value = ["reminder_time"])
    ]
)
data class MemoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "title")
    val title: String = "",

    @ColumnInfo(name = "content")
    val content: String = "",

    @ColumnInfo(name = "target_date")
    val targetDate: Long,

    @ColumnInfo(name = "reminder_time")
    val reminderTime: Long? = null,

    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean = false,

    @ColumnInfo(name = "repeat_type")
    val repeatType: String = "NONE",

    @ColumnInfo(name = "template_id")
    val templateId: String? = null,

    @ColumnInfo(name = "use_custom_ringtone")
    val useCustomRingtone: Boolean = true,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

fun MemoEntity.toDomainModel(): Memo = Memo(
    id = id,
    title = title,
    content = content,
    targetDate = targetDate,
    reminderTime = reminderTime,
    repeatType = RepeatType.fromValue(repeatType),
    templateId = templateId,
    useCustomRingtone = useCustomRingtone,
    isCompleted = isCompleted,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Memo.toEntity(): MemoEntity = MemoEntity(
    id = id,
    title = title,
    content = content,
    targetDate = targetDate,
    reminderTime = reminderTime,
    isCompleted = isCompleted,
    repeatType = repeatType.value,
    templateId = templateId,
    useCustomRingtone = useCustomRingtone,
    createdAt = createdAt,
    updatedAt = updatedAt
)

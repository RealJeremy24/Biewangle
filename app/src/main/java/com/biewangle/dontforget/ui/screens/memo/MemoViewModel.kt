package com.biewangle.dontforget.ui.screens.memo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.biewangle.dontforget.BiewangleApp
import com.biewangle.dontforget.data.model.Memo
import com.biewangle.dontforget.data.model.RepeatType
import com.biewangle.dontforget.data.repository.MemoRepository
import com.biewangle.dontforget.service.AlarmScheduler
import com.biewangle.dontforget.util.DateTimeUtils
import java.util.Calendar
import com.biewangle.dontforget.util.TemplateItem
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

data class MemoGroup(
    val dateLabel: String,
    val dateTimestamp: Long,
    val memos: List<Memo>
)

data class MemoFormState(
    val id: Long = 0,
    val title: String = "",
    val content: String = "",
    val targetDate: Long = DateTimeUtils.getUtcStartOfDay(DateTimeUtils.now()),
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
    val reminderMinute: Int = Calendar.getInstance().get(Calendar.MINUTE),
    val repeatType: RepeatType = RepeatType.NONE,
    val templateId: String? = null,
    val useCustomRingtone: Boolean = true
) {
    val reminderTime: Long?
        get() = if (reminderEnabled) {
            DateTimeUtils.createTimestamp(targetDate, reminderHour, reminderMinute)
        } else null
}

class MemoViewModel(
    private val repository: MemoRepository,
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {

    private val _memoGroups = MutableStateFlow<List<MemoGroup>>(emptyList())
    val memoGroups: StateFlow<List<MemoGroup>> = _memoGroups.asStateFlow()

    private val _formState = MutableStateFlow(MemoFormState())
    val formState: StateFlow<MemoFormState> = _formState.asStateFlow()

    private val _showForm = MutableStateFlow(false)
    val showForm: StateFlow<Boolean> = _showForm.asStateFlow()

    // 保存完成事件（用于触发自启动引导）
    private val _memoSaved = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val memoSaved: SharedFlow<Unit> = _memoSaved

    init {
        viewModelScope.launch {
            repository.getAllMemos().collect { memos ->
                _memoGroups.value = groupMemosByDate(memos)
            }
        }
    }

    private fun groupMemosByDate(memos: List<Memo>): List<MemoGroup> {
        return memos
            .groupBy { DateTimeUtils.getStartOfDay(it.targetDate) }
            .map { (dateTimestamp, memoList) ->
                MemoGroup(
                    dateLabel = DateTimeUtils.formatDateHeader(dateTimestamp),
                    dateTimestamp = dateTimestamp,
                    memos = memoList.sortedBy { it.reminderTime ?: Long.MAX_VALUE }
                )
            }
            .sortedBy { it.dateTimestamp }
    }

    fun showNewForm() {
        _formState.value = MemoFormState()
        _showForm.value = true
    }

    fun showEditForm(memo: Memo) {
        val cal = java.util.Calendar.getInstance()
        memo.reminderTime?.let { cal.timeInMillis = it }

        _formState.value = MemoFormState(
            id = memo.id,
            title = memo.title,
            content = memo.content,
            targetDate = DateTimeUtils.toUtcMidnight(memo.targetDate),
            reminderEnabled = memo.reminderTime != null,
            reminderHour = cal.get(java.util.Calendar.HOUR_OF_DAY),
            reminderMinute = cal.get(java.util.Calendar.MINUTE),
            repeatType = memo.repeatType,
            templateId = memo.templateId,
            useCustomRingtone = memo.useCustomRingtone
        )
        _showForm.value = true
    }

    fun hideForm() {
        _showForm.value = false
    }

    fun updateTitle(title: String) {
        _formState.value = _formState.value.copy(title = title)
    }

    fun updateContent(content: String) {
        _formState.value = _formState.value.copy(content = content)
    }

    fun updateDate(timestamp: Long) {
        // DatePicker 返回的是 UTC 零点毫秒值，原样存储，
        // 不要用 getStartOfDay 转换（会导致再次打开 DatePicker 时高亮偏一天）
        _formState.value = _formState.value.copy(targetDate = timestamp)
    }

    fun toggleReminder(enabled: Boolean) {
        _formState.value = _formState.value.copy(reminderEnabled = enabled)
    }

    fun updateReminderTime(hour: Int, minute: Int) {
        _formState.value = _formState.value.copy(reminderHour = hour, reminderMinute = minute)
    }

    fun updateRepeatType(repeatType: RepeatType) {
        _formState.value = _formState.value.copy(repeatType = repeatType)
    }

    fun toggleCustomRingtone(enabled: Boolean) {
        _formState.value = _formState.value.copy(useCustomRingtone = enabled)
    }

    fun applyTemplate(template: TemplateItem) {
        _formState.value = _formState.value.copy(
            title = template.label.replace(Regex("^[^\\s]+\\s"), ""),
            content = template.defaultContent,
            templateId = template.id
        )
    }

    fun saveMemo() {
        viewModelScope.launch {
            val form = _formState.value
            if (form.title.isBlank()) return@launch

            val memo = Memo(
                id = form.id,
                title = form.title,
                content = form.content,
                targetDate = form.targetDate,
                reminderTime = form.reminderTime,
                repeatType = form.repeatType,
                templateId = form.templateId,
                useCustomRingtone = form.useCustomRingtone
            )

            if (memo.id == 0L) {
                val newId = repository.insert(memo)
                form.reminderTime?.let {
                    alarmScheduler.schedule(newId, form.title, form.content, it, form.useCustomRingtone)
                }
            } else {
                repository.update(memo)
                alarmScheduler.cancel(memo.id)
                form.reminderTime?.let {
                    alarmScheduler.schedule(memo.id, form.title, form.content, it, form.useCustomRingtone)
                }
            }

            _showForm.value = false
            _memoSaved.tryEmit(Unit)
        }
    }

    fun deleteMemo(id: Long) {
        viewModelScope.launch {
            alarmScheduler.cancel(id)
            repository.delete(id)
        }
    }

    fun toggleComplete(id: Long) {
        viewModelScope.launch {
            val memo = repository.getById(id) ?: return@launch

            if (memo.isCompleted) {
                // 取消完成
                repository.toggleComplete(id)
            } else {
                // 标记完成
                repository.toggleComplete(id)

                // 如果是重复事项，创建下一次提醒
                if (memo.repeatType != RepeatType.NONE) {
                    val nextDate = DateTimeUtils.calculateNextOccurrence(
                        memo.targetDate, memo.repeatType.value
                    )
                    val newMemo = memo.copy(
                        id = 0,
                        targetDate = nextDate,
                        reminderTime = memo.reminderTime?.let {
                            DateTimeUtils.createTimestamp(
                                nextDate,
                                java.util.Calendar.getInstance().apply { timeInMillis = it }
                                    .get(java.util.Calendar.HOUR_OF_DAY),
                                java.util.Calendar.getInstance().apply { timeInMillis = it }
                                    .get(java.util.Calendar.MINUTE)
                            )
                        },
                        isCompleted = false
                    )
                    val newId = repository.insert(newMemo)
                    newMemo.reminderTime?.let { time ->
                        alarmScheduler.schedule(newId, newMemo.title, newMemo.content, time, newMemo.useCustomRingtone)
                    }
                }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = BiewangleApp.instance
                return MemoViewModel(app.memoRepository, AlarmScheduler(app)) as T
            }
        }
    }
}

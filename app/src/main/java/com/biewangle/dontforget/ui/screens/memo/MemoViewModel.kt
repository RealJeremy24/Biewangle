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
import com.biewangle.dontforget.util.TemplateItem
import kotlinx.coroutines.flow.MutableStateFlow
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
    val targetDate: Long = DateTimeUtils.getStartOfDay(DateTimeUtils.now()),
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 9,
    val reminderMinute: Int = 0,
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
            targetDate = memo.targetDate,
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
        _formState.value = _formState.value.copy(targetDate = DateTimeUtils.getStartOfDay(timestamp))
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

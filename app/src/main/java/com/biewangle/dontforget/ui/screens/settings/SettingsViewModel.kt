package com.biewangle.dontforget.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.biewangle.dontforget.BiewangleApp
import com.biewangle.dontforget.data.model.Memo
import com.biewangle.dontforget.data.model.RingtoneConfig
import com.biewangle.dontforget.data.repository.MemoRepository
import com.biewangle.dontforget.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

data class StatsData(
    val total: Int = 0,
    val completed: Int = 0,
    val rate: Float = 0f
) {
    val ratePercent: String get() = "${(rate * 100).toInt()}%"
    val encourageMessage: String get() = when {
        rate >= 0.8f -> "🌟\n太棒了！您已经完成了大部分事项！\n继续保持，健康生活每一天！"
        rate >= 0.5f -> "💪\n加油！您已经完成了一半的事项，\n再接再厉！"
        rate > 0f -> "🌱\n一步一步来，\n您能行的！"
        else -> "📝\n今天还没有完成事项，\n开始行动吧！"
    }
}

// ── 统计详情数据 ──

/** 总事项的日期分组统计 */
data class TotalDetailData(
    val today: Int = 0,
    val thisWeek: Int = 0,
    val thisMonth: Int = 0,
    val allTime: Int = 0
)

/** 已完成事项详情 */
data class CompletedDetailData(
    val total: Int = 0,
    val recentItems: List<Memo> = emptyList()
)

/** 完成率详情 */
data class RateDetailData(
    val completed: Int = 0,
    val total: Int = 0,
    val ratePercent: String = "0%",
    val encourageMessage: String = ""
)

/** 当前正在显示的统计详情弹窗 */
enum class StatsDetailDialog {
    TOTAL,      // 总事项
    COMPLETED,  // 已完成
    RATE        // 完成率
}

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val memoRepository: MemoRepository
) : ViewModel() {

    private val _stats = MutableStateFlow(StatsData())
    val stats: StateFlow<StatsData> = _stats.asStateFlow()

    private val _ringtoneConfig = MutableStateFlow(RingtoneConfig())
    val ringtoneConfig: StateFlow<RingtoneConfig> = _ringtoneConfig.asStateFlow()

    private val _fontScale = MutableStateFlow(0.78f)
    val fontScale: StateFlow<Float> = _fontScale.asStateFlow()

    // Slider 0~10 → 0.6~1.5
    val fontScaleSteps: Float get() = _fontScale.value

    // Slider position 0~10, default at tier 2 (0.78f)
    private val _fontSliderPosition = MutableStateFlow(2)
    val fontSliderPosition: StateFlow<Int> = _fontSliderPosition.asStateFlow()

    private val _vibrateEnabled = MutableStateFlow(true)
    val vibrateEnabled: StateFlow<Boolean> = _vibrateEnabled.asStateFlow()

    // ── 统计详情弹窗 ──

    private val _detailDialog = MutableStateFlow<StatsDetailDialog?>(null)
    val detailDialog: StateFlow<StatsDetailDialog?> = _detailDialog.asStateFlow()

    private val _totalDetailData = MutableStateFlow(TotalDetailData())
    val totalDetailData: StateFlow<TotalDetailData> = _totalDetailData.asStateFlow()

    private val _completedDetailData = MutableStateFlow(CompletedDetailData())
    val completedDetailData: StateFlow<CompletedDetailData> = _completedDetailData.asStateFlow()

    private val _rateDetailData = MutableStateFlow(RateDetailData())
    val rateDetailData: StateFlow<RateDetailData> = _rateDetailData.asStateFlow()

    init {
        // 加载统计数据
        viewModelScope.launch {
            combine(
                memoRepository.getTotalCount(),
                memoRepository.getCompletedCount()
            ) { total, completed ->
                StatsData(
                    total = total,
                    completed = completed,
                    rate = if (total > 0) completed.toFloat() / total else 0f
                )
            }.collect { _stats.value = it }
        }

        // 加载设置
        viewModelScope.launch {
            _ringtoneConfig.value = settingsRepository.getRingtoneConfig()
            val scale = settingsRepository.getFontScale()
            _fontScale.value = scale
            _fontSliderPosition.value = fontScaleToSlider(scale)
            _vibrateEnabled.value = settingsRepository.getVibrateEnabled()
        }
    }

    fun updateRingtone(uri: String, displayName: String) {
        val config = _ringtoneConfig.value.copy(uri = uri, displayName = displayName)
        _ringtoneConfig.value = config
        viewModelScope.launch { settingsRepository.saveRingtoneConfig(config) }
    }

    fun updateTrimRange(startMs: Long, endMs: Long) {
        val config = _ringtoneConfig.value.copy(trimStartMs = startMs, trimEndMs = endMs)
        _ringtoneConfig.value = config
        viewModelScope.launch { settingsRepository.saveRingtoneConfig(config) }
    }

    fun updateFontScaleFromSlider(sliderPosition: Int) {
        _fontSliderPosition.value = sliderPosition
        val scale = sliderToFontScale(sliderPosition)
        _fontScale.value = scale
        viewModelScope.launch { settingsRepository.setFontScale(scale) }
        // 同步更新全局字体缩放，使整个 App 即时响应
        BiewangleApp.instance.updateFontScale(scale)
    }

    fun toggleVibrateEnabled() {
        val newValue = !_vibrateEnabled.value
        _vibrateEnabled.value = newValue
        viewModelScope.launch { settingsRepository.setVibrateEnabled(newValue) }
    }

    // ── 统计详情弹窗 ──

    fun showDetailDialog(dialog: StatsDetailDialog) {
        _detailDialog.value = dialog
        loadDetailData(dialog)
    }

    fun hideDetailDialog() {
        _detailDialog.value = null
    }

    private fun loadDetailData(dialog: StatsDetailDialog) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                when (dialog) {
                    StatsDetailDialog.TOTAL -> {
                        val now = System.currentTimeMillis()
                        val cal = java.util.Calendar.getInstance()
                        cal.timeInMillis = now

                        // 今天 00:00
                        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                        cal.set(java.util.Calendar.MINUTE, 0)
                        cal.set(java.util.Calendar.SECOND, 0)
                        cal.set(java.util.Calendar.MILLISECOND, 0)
                        val todayStart = cal.timeInMillis

                        // 本周一 00:00
                        cal.set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
                        val weekStart = cal.timeInMillis

                        // 本月 1 日 00:00
                        cal.timeInMillis = now
                        cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
                        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                        cal.set(java.util.Calendar.MINUTE, 0)
                        cal.set(java.util.Calendar.SECOND, 0)
                        cal.set(java.util.Calendar.MILLISECOND, 0)
                        val monthStart = cal.timeInMillis

                        val today = memoRepository.getCountSince(todayStart)
                        val thisWeek = memoRepository.getCountSince(weekStart)
                        val thisMonth = memoRepository.getCountSince(monthStart)
                        val allTime = _stats.value.total

                        _totalDetailData.value = TotalDetailData(
                            today = today,
                            thisWeek = thisWeek,
                            thisMonth = thisMonth,
                            allTime = allTime
                        )
                    }
                    StatsDetailDialog.COMPLETED -> {
                        val recent = memoRepository.getRecentlyCompletedAll(limit = 6)
                        _completedDetailData.value = CompletedDetailData(
                            total = _stats.value.completed,
                            recentItems = recent
                        )
                    }
                    StatsDetailDialog.RATE -> {
                        val stats = _stats.value
                        _rateDetailData.value = RateDetailData(
                            completed = stats.completed,
                            total = stats.total,
                            ratePercent = stats.ratePercent,
                            encourageMessage = stats.encourageMessage
                        )
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
                return SettingsViewModel(app.settingsRepository, app.memoRepository) as T
            }
        }

        // Slider 0~10 ↔ 字体缩放 0.6f~1.5f（1.0f 在 position ~4.4 ≈ 中间位置）
        fun sliderToFontScale(slider: Int): Float = 0.6f + slider * 0.09f
        fun fontScaleToSlider(scale: Float): Int =
            ((scale - 0.6f) / 0.09f).roundToInt().coerceIn(0, 10)
    }
}

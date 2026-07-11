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
import java.util.TimeZone

/** 统计时段 */
enum class StatsPeriod(val label: String) {
    TODAY("今天"),
    WEEK("本周"),
    MONTH("本月"),
    ALL("全部")
}

data class StatsData(
    val total: Int = 0,
    val completed: Int = 0,
    val rate: Float = 0f,
    val period: StatsPeriod = StatsPeriod.TODAY
) {
    val ratePercent: String get() = "${(rate * 100).toInt()}%"
    val encourageMessage: String get() = when (period) {
        StatsPeriod.TODAY -> when {
            total == 0 -> "📝\n今天还没有备忘事项，\n开始行动吧！"
            completed >= total -> "🌟\n今天全部完成！太棒了！"
            completed > 0 -> "💪\n今天完成了 $completed/$total 条，\n继续加油！"
            else -> "📝\n今天还有 $total 条待完成，\n加油！"
        }
        StatsPeriod.WEEK -> when {
            total == 0 -> "📝\n本周还没有备忘事项。"
            rate >= 0.8f -> "🌟\n本周表现优秀！已完成 $completed 条！"
            rate >= 0.5f -> "💪\n本周已完成 $completed 条，\n继续保持！"
            completed > 0 -> "🌱\n本周完成了 $completed 条，\n再接再厉！"
            else -> "📝\n本周还没有完成事项，\n开始行动吧！"
        }
        StatsPeriod.MONTH -> when {
            total == 0 -> "📝\n本月还没有备忘事项。"
            rate >= 0.8f -> "🌟\n本月成绩出色！已完成 $completed 条！"
            rate >= 0.5f -> "💪\n本月已完成 $completed/$total 条，\n加油！"
            completed > 0 -> "🌱\n本月完成了 $completed 条，\n继续努力！"
            else -> "📝\n本月还没有完成事项，\n开始行动吧！"
        }
        StatsPeriod.ALL -> when {
            rate >= 0.8f -> "🌟\n太棒了！您已经完成了大部分事项！\n继续保持，健康生活每一天！"
            rate >= 0.5f -> "💪\n加油！您已经完成了一半的事项，\n再接再厉！"
            rate > 0f -> "🌱\n一步一步来，\n您能行的！"
            else -> "📝\n还没有完成事项，\n开始行动吧！"
        }
    }
}

// ── 统计详情数据 ──

/** 已完成事项详情 — 按完成日期分组 */
data class CompletedDetailData(
    val total: Int = 0,
    val groups: List<CompletedGroup> = emptyList()
)

data class CompletedGroup(
    val dateLabel: String,
    val items: List<Memo>
)

/** 总事项详情 — 按时段分组 */
data class TotalDetailData(
    val total: Int = 0,
    val groups: List<MemoGroup> = emptyList()
)

data class MemoGroup(
    val groupLabel: String,
    val items: List<Memo>
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

    private val _selectedPeriod = MutableStateFlow(StatsPeriod.TODAY)
    val selectedPeriod: StateFlow<StatsPeriod> = _selectedPeriod.asStateFlow()

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
        // 加载统计数据 — 时段切换或数据库变化时自动刷新
        viewModelScope.launch {
            combine(
                _selectedPeriod,
                memoRepository.getTotalCount(),
                memoRepository.getCompletedCount()
            ) { period, _, _ -> period }
                .collect { period -> _stats.value = loadStatsForPeriod(period) }
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

    /** 按时段查询统计数据 */
    private suspend fun loadStatsForPeriod(period: StatsPeriod): StatsData {
        return withContext(Dispatchers.IO) {
            val (total, completed) = when (period) {
                StatsPeriod.TODAY, StatsPeriod.WEEK, StatsPeriod.MONTH -> {
                    val start = getPeriodStartTime(period)
                    val end = getPeriodEndTime(period)
                    val t = memoRepository.getCountBetween(start, end)
                    val c = memoRepository.getCompletedCountBetween(start, end)
                    t to c
                }
                StatsPeriod.ALL -> {
                    val t = memoRepository.getTotalCountOnce()
                    val c = memoRepository.getCompletedCountOnce()
                    t to c
                }
            }
            StatsData(
                total = total,
                completed = completed,
                rate = if (total > 0) completed.toFloat() / total else 0f,
                period = period
            )
        }
    }

    private fun getPeriodStartTime(period: StatsPeriod): Long {
        // target_date 存储为 UTC 零点，查询也必须用 UTC 零点才能对上
        val utcCal = java.util.Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        val now = System.currentTimeMillis()
        utcCal.setTimeInMillis(now)
        return when (period) {
            StatsPeriod.TODAY -> {
                utcCal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                utcCal.set(java.util.Calendar.MINUTE, 0)
                utcCal.set(java.util.Calendar.SECOND, 0)
                utcCal.set(java.util.Calendar.MILLISECOND, 0)
                utcCal.timeInMillis
            }
            StatsPeriod.WEEK -> {
                // 先归零时分秒，再滚动到本周一（不用 set(DAY_OF_WEEK) 因为会滚到下周）
                utcCal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                utcCal.set(java.util.Calendar.MINUTE, 0)
                utcCal.set(java.util.Calendar.SECOND, 0)
                utcCal.set(java.util.Calendar.MILLISECOND, 0)
                val dayOfWeek = utcCal.get(java.util.Calendar.DAY_OF_WEEK)
                val daysToMonday = if (dayOfWeek == java.util.Calendar.SUNDAY) 6 else dayOfWeek - java.util.Calendar.MONDAY
                utcCal.add(java.util.Calendar.DAY_OF_MONTH, -daysToMonday)
                utcCal.timeInMillis
            }
            StatsPeriod.MONTH -> {
                utcCal.set(java.util.Calendar.DAY_OF_MONTH, 1)
                utcCal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                utcCal.set(java.util.Calendar.MINUTE, 0)
                utcCal.set(java.util.Calendar.SECOND, 0)
                utcCal.set(java.util.Calendar.MILLISECOND, 0)
                utcCal.timeInMillis
            }
            StatsPeriod.ALL -> 0L
        }
    }

    /** 获取时段结束时间（不含），用于 range 查询 */
    private fun getPeriodEndTime(period: StatsPeriod): Long {
        // target_date 存储为 UTC 零点，查询范围end也必须用 UTC 零点
        val utcCal = java.util.Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        utcCal.setTimeInMillis(System.currentTimeMillis())
        return when (period) {
            StatsPeriod.TODAY -> {
                utcCal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                utcCal.set(java.util.Calendar.MINUTE, 0)
                utcCal.set(java.util.Calendar.SECOND, 0)
                utcCal.set(java.util.Calendar.MILLISECOND, 0)
                utcCal.add(java.util.Calendar.DAY_OF_MONTH, 1)
                utcCal.timeInMillis
            }
            StatsPeriod.WEEK -> {
                // 从本周一加7天得到下周一（本周结束）
                utcCal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                utcCal.set(java.util.Calendar.MINUTE, 0)
                utcCal.set(java.util.Calendar.SECOND, 0)
                utcCal.set(java.util.Calendar.MILLISECOND, 0)
                val dayOfWeek = utcCal.get(java.util.Calendar.DAY_OF_WEEK)
                val daysToMonday = if (dayOfWeek == java.util.Calendar.SUNDAY) 6 else dayOfWeek - java.util.Calendar.MONDAY
                utcCal.add(java.util.Calendar.DAY_OF_MONTH, -daysToMonday + 7)
                utcCal.timeInMillis
            }
            StatsPeriod.MONTH -> {
                utcCal.set(java.util.Calendar.DAY_OF_MONTH, 1)
                utcCal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                utcCal.set(java.util.Calendar.MINUTE, 0)
                utcCal.set(java.util.Calendar.SECOND, 0)
                utcCal.set(java.util.Calendar.MILLISECOND, 0)
                utcCal.add(java.util.Calendar.MONTH, 1)
                utcCal.timeInMillis
            }
            StatsPeriod.ALL -> Long.MAX_VALUE
        }
    }

    /** 按时段对备忘分组：今天→不分组，本周→按日，本月→按周，全部→按月 */
    private fun groupMemos(items: List<Memo>, period: StatsPeriod): List<MemoGroup> {
        if (items.isEmpty()) return emptyList()
        val cal = java.util.Calendar.getInstance()
        val today = java.util.Calendar.getInstance()

        return when (period) {
            StatsPeriod.TODAY -> {
                listOf(MemoGroup("今天", items))
            }
            StatsPeriod.WEEK -> {
                val dayNames = arrayOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")
                items.groupBy { memo ->
                    cal.timeInMillis = memo.targetDate
                    cal.get(java.util.Calendar.DAY_OF_WEEK)
                }.map { (dayOfWeek, memos) ->
                    cal.timeInMillis = memos.first().targetDate
                    val isToday = cal.get(java.util.Calendar.DAY_OF_YEAR) == today.get(java.util.Calendar.DAY_OF_YEAR)
                            && cal.get(java.util.Calendar.YEAR) == today.get(java.util.Calendar.YEAR)
                    val label = if (isToday) "今天" else dayNames[dayOfWeek - 1]
                    MemoGroup(label, memos)
                }.sortedByDescending { it.items.first().targetDate }
            }
            StatsPeriod.MONTH -> {
                // 用 UTC 计算相对标签：本周、下周、上周
                val utcCal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
                utcCal.setTimeInMillis(System.currentTimeMillis())
                utcCal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                utcCal.set(java.util.Calendar.MINUTE, 0)
                utcCal.set(java.util.Calendar.SECOND, 0)
                utcCal.set(java.util.Calendar.MILLISECOND, 0)
                val dayOfWeek = utcCal.get(java.util.Calendar.DAY_OF_WEEK)
                val daysToMonday = if (dayOfWeek == java.util.Calendar.SUNDAY) 6 else dayOfWeek - java.util.Calendar.MONDAY
                utcCal.add(java.util.Calendar.DAY_OF_MONTH, -daysToMonday)
                val thisMondayStart = utcCal.timeInMillis
                val weekMillis = 7L * 24 * 60 * 60 * 1000

                items.groupBy { memo ->
                    val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
                    cal.timeInMillis = memo.targetDate
                    cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                    cal.set(java.util.Calendar.MINUTE, 0)
                    cal.set(java.util.Calendar.SECOND, 0)
                    cal.set(java.util.Calendar.MILLISECOND, 0)
                    val d = cal.get(java.util.Calendar.DAY_OF_WEEK)
                    val offset = if (d == java.util.Calendar.SUNDAY) 6 else d - java.util.Calendar.MONDAY
                    cal.add(java.util.Calendar.DAY_OF_MONTH, -offset)
                    cal.timeInMillis
                }.map { (weekStart, memos) ->
                    val weeksDiff = ((weekStart - thisMondayStart) / weekMillis).toInt()
                    val label = when (weeksDiff) {
                        0 -> "本周"
                        1 -> "下周"
                        -1 -> "上周"
                        else -> if (weeksDiff > 1) "${weeksDiff}周后" else "${-weeksDiff}周前"
                    }
                    MemoGroup(label, memos)
                }.sortedByDescending { it.items.first().targetDate }
            }
            StatsPeriod.ALL -> {
                items.groupBy { memo ->
                    cal.timeInMillis = memo.targetDate
                    "${cal.get(java.util.Calendar.YEAR)}-${cal.get(java.util.Calendar.MONTH)}"
                }.map { (_, memos) ->
                    cal.timeInMillis = memos.first().targetDate
                    val year = cal.get(java.util.Calendar.YEAR)
                    val month = cal.get(java.util.Calendar.MONTH) + 1
                    MemoGroup("${year}年${month}月", memos)
                }.sortedByDescending { it.items.first().targetDate }
            }
        }
    }

    /** 按时段对已完成备忘分组：今天→不分组，本周→按日，本月→按周，全部→按月 */
    private fun groupCompletedMemos(items: List<Memo>, period: StatsPeriod): List<CompletedGroup> {
        if (items.isEmpty()) return emptyList()
        val dayNames = arrayOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")

        return when (period) {
            StatsPeriod.TODAY -> {
                listOf(CompletedGroup("今天", items))
            }
            StatsPeriod.WEEK -> {
                // 用 targetDate 分组（反映计划日期），而非 updatedAt（完成时间）
                // 先算今天0点作为基准
                val todayCal = java.util.Calendar.getInstance()
                todayCal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                todayCal.set(java.util.Calendar.MINUTE, 0)
                todayCal.set(java.util.Calendar.SECOND, 0)
                todayCal.set(java.util.Calendar.MILLISECOND, 0)
                val todayStart = todayCal.timeInMillis
                val dayMillis = 24L * 60 * 60 * 1000

                items.groupBy { memo ->
                    val cal = java.util.Calendar.getInstance()
                    cal.timeInMillis = memo.targetDate
                    cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                    cal.set(java.util.Calendar.MINUTE, 0)
                    cal.set(java.util.Calendar.SECOND, 0)
                    cal.set(java.util.Calendar.MILLISECOND, 0)
                    cal.timeInMillis
                }.map { (dateStart, memos) ->
                    val daysDiff = ((dateStart - todayStart) / dayMillis).toInt()
                    val label = when (daysDiff) {
                        0 -> "今天"
                        1 -> "明天"
                        -1 -> "昨天"
                        else -> dayNames[java.util.Calendar.getInstance().apply { timeInMillis = dateStart }.get(java.util.Calendar.DAY_OF_WEEK) - 1]
                    }
                    CompletedGroup(dateLabel = label, items = memos)
                }.sortedByDescending { it.items.first().targetDate }
            }
            StatsPeriod.MONTH -> {
                // 用 UTC 计算本周一基准
                val utcCal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
                utcCal.setTimeInMillis(System.currentTimeMillis())
                utcCal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                utcCal.set(java.util.Calendar.MINUTE, 0)
                utcCal.set(java.util.Calendar.SECOND, 0)
                utcCal.set(java.util.Calendar.MILLISECOND, 0)
                val dayOfWeek = utcCal.get(java.util.Calendar.DAY_OF_WEEK)
                val daysToMonday = if (dayOfWeek == java.util.Calendar.SUNDAY) 6 else dayOfWeek - java.util.Calendar.MONDAY
                utcCal.add(java.util.Calendar.DAY_OF_MONTH, -daysToMonday)
                val thisMondayStart = utcCal.timeInMillis
                val weekMillis = 7L * 24 * 60 * 60 * 1000

                // 用 UTC 计算 targetDate 所在周的周一作为 group key
                items.groupBy { memo ->
                    val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
                    cal.timeInMillis = memo.targetDate
                    cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                    cal.set(java.util.Calendar.MINUTE, 0)
                    cal.set(java.util.Calendar.SECOND, 0)
                    cal.set(java.util.Calendar.MILLISECOND, 0)
                    val d = cal.get(java.util.Calendar.DAY_OF_WEEK)
                    val offset = if (d == java.util.Calendar.SUNDAY) 6 else d - java.util.Calendar.MONDAY
                    cal.add(java.util.Calendar.DAY_OF_MONTH, -offset)
                    cal.timeInMillis
                }.map { (weekStart, memos) ->
                    val weeksDiff = ((weekStart - thisMondayStart) / weekMillis).toInt()
                    val label = when (weeksDiff) {
                        0 -> "本周"
                        1 -> "下周"
                        -1 -> "上周"
                        else -> if (weeksDiff > 1) "${weeksDiff}周后" else "${-weeksDiff}周前"
                    }
                    CompletedGroup(dateLabel = label, items = memos)
                }.sortedByDescending { it.items.first().targetDate }
            }
            StatsPeriod.ALL -> {
                items.groupBy { memo ->
                    val cal = java.util.Calendar.getInstance()
                    cal.timeInMillis = memo.targetDate
                    "${cal.get(java.util.Calendar.YEAR)}-${cal.get(java.util.Calendar.MONTH)}"
                }.map { (_, memos) ->
                    val cal = java.util.Calendar.getInstance()
                    cal.timeInMillis = memos.first().targetDate
                    val year = cal.get(java.util.Calendar.YEAR)
                    val month = cal.get(java.util.Calendar.MONTH) + 1
                    CompletedGroup(dateLabel = "${year}年${month}月", items = memos)
                }.sortedByDescending { it.items.first().targetDate }
            }
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

    // ── 时段选择 ──

    fun selectPeriod(period: StatsPeriod) {
        _selectedPeriod.value = period
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
                        val period = _selectedPeriod.value
                        val items = when (period) {
                            StatsPeriod.ALL -> memoRepository.getAllMemosSnapshot()
                            else -> {
                                val start = getPeriodStartTime(period)
                                val end = getPeriodEndTime(period)
                                memoRepository.getAllMemosBetween(start, end)
                            }
                        }
                        val groups = groupMemos(items, period)
                        _totalDetailData.value = TotalDetailData(
                            total = _stats.value.total,
                            groups = groups
                        )
                    }
                    StatsDetailDialog.COMPLETED -> {
                        val period = _selectedPeriod.value
                        val items = when (period) {
                            StatsPeriod.ALL -> memoRepository.getRecentlyCompletedAll(limit = 999)
                            else -> {
                                val start = getPeriodStartTime(period)
                                val end = getPeriodEndTime(period)
                                memoRepository.getRecentlyCompletedBetween(start, end, limit = 999)
                            }
                        }
                        val groups = groupCompletedMemos(items, period)
                        _completedDetailData.value = CompletedDetailData(
                            total = _stats.value.completed,
                            groups = groups
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

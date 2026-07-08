package com.biewangle.dontforget.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.biewangle.dontforget.BiewangleApp
import com.biewangle.dontforget.data.model.RingtoneConfig
import com.biewangle.dontforget.data.repository.MemoRepository
import com.biewangle.dontforget.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
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

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val memoRepository: MemoRepository
) : ViewModel() {

    private val _stats = MutableStateFlow(StatsData())
    val stats: StateFlow<StatsData> = _stats.asStateFlow()

    private val _ringtoneConfig = MutableStateFlow(RingtoneConfig())
    val ringtoneConfig: StateFlow<RingtoneConfig> = _ringtoneConfig.asStateFlow()

    private val _fontScale = MutableStateFlow(0.69f)
    val fontScale: StateFlow<Float> = _fontScale.asStateFlow()

    // Slider 0~10 → 0.6~1.5
    val fontScaleSteps: Float get() = _fontScale.value

    // Slider position 0~10, default at tier 2 (0.78f)
    private val _fontSliderPosition = MutableStateFlow(1)
    val fontSliderPosition: StateFlow<Int> = _fontSliderPosition.asStateFlow()

    private val _vibrateEnabled = MutableStateFlow(true)
    val vibrateEnabled: StateFlow<Boolean> = _vibrateEnabled.asStateFlow()

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

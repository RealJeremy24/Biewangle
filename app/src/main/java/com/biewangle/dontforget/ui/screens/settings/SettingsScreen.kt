package com.biewangle.dontforget.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.biewangle.dontforget.ui.theme.scaledSp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.biewangle.dontforget.ui.components.StatsCard
import com.biewangle.dontforget.ui.theme.BackgroundWarm
import com.biewangle.dontforget.ui.theme.CardWhite
import com.biewangle.dontforget.ui.theme.ChipUnselected
import com.biewangle.dontforget.ui.theme.DividerWarm
import com.biewangle.dontforget.ui.theme.EncourageBg
import com.biewangle.dontforget.ui.theme.PrimaryOrange
import com.biewangle.dontforget.ui.theme.TextDarkBrown
import com.biewangle.dontforget.ui.theme.TextWarmGray
import com.biewangle.dontforget.ui.theme.WhiteText

import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavHostController,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
) {
    val stats by viewModel.stats.collectAsState()
    val fontSliderPosition by viewModel.fontSliderPosition.collectAsState()
    val fontScale by viewModel.fontScale.collectAsState()
    val vibrateEnabled by viewModel.vibrateEnabled.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundWarm)
            .verticalScroll(rememberScrollState())
    ) {
        // 品牌顶部栏 — 暖橙渐变
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(PrimaryOrange, Color(0xFFF5CBA0)),
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    ),
                    shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
                )
                .padding(horizontal = 20.dp, vertical = 22.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("⚙️", fontSize = 28.sp)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        "统计与设置",
                        fontSize = scaledSp(26),
                        fontWeight = FontWeight.Bold,
                        color = WhiteText
                    )
                    Text(
                        "数据总览 · 偏好调整",
                        fontSize = scaledSp(14),
                        color = WhiteText.copy(alpha = 0.75f)
                    )
                }
            }
        }

        // 统计卡片
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatsCard(
                icon = "📝",
                value = "${stats.total}",
                label = "总事项",
                modifier = Modifier.weight(1f)
            )
            StatsCard(
                icon = "✅",
                value = "${stats.completed}",
                label = "已完成",
                modifier = Modifier.weight(1f)
            )
            StatsCard(
                icon = "📊",
                value = stats.ratePercent,
                label = "完成率",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(16.dp))

        // 鼓励语卡片
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .background(EncourageBg, RoundedCornerShape(16.dp))
                .padding(24.dp)
        ) {
            Text(
                text = stats.encourageMessage,
                fontSize = scaledSp(24),
                color = TextDarkBrown,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(24.dp))

        // ── 提醒设置 ──
        SectionHeader("提醒设置")

        SettingsRowWithSwitch(
            icon = "📳",
            title = "提醒时震动",
            checked = vibrateEnabled,
            onCheckedChange = { viewModel.toggleVibrateEnabled() }
        )

        Spacer(Modifier.height(16.dp))

        // ── 显示设置 ──
        SectionHeader("显示设置")

        // 字体大小调节
        FontSizeSliderRow(
            sliderPosition = fontSliderPosition,
            onSliderChange = { viewModel.updateFontScaleFromSlider(it) }
        )

        Spacer(Modifier.height(24.dp))

        // 版本信息
        Text(
            text = "别忘乐 v1.0",
            fontSize = scaledSp(16),
            color = TextWarmGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))
    }
}

// ── 字体大小滑块 ──
@Composable
private fun FontSizeSliderRow(
    sliderPosition: Int,
    onSliderChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .background(CardWhite, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // 滑块主体：小A ———○——— 大A
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧小 A（12sp）
            Text(
                text = "A",
                fontSize = 12.sp,
                fontWeight = FontWeight.Light,
                color = TextWarmGray
            )
            // Slider
            Slider(
                value = sliderPosition.toFloat(),
                onValueChange = { onSliderChange(it.roundToInt()) },
                valueRange = 0f..10f,
                steps = 9, // 10 档 = 9 个步进点
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                colors = SliderDefaults.colors(
                    thumbColor = WhiteText,
                    activeTrackColor = PrimaryOrange,
                    inactiveTrackColor = ChipUnselected
                )
            )
            // 右侧大 A（28sp）
            Text(
                text = "A",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryOrange
            )
        }

        // 实时预览文字
        Text(
            text = "字体大小预览",
            fontSize = scaledSp(22),
            color = TextDarkBrown,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = scaledSp(22),
        fontWeight = FontWeight.Bold,
        color = PrimaryOrange,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun SettingsRowWithSwitch(
    icon: String,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .background(CardWhite, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 24.sp)
        Spacer(Modifier.width(8.dp))
        Text(
            title,
            fontSize = scaledSp(22),
            color = TextDarkBrown,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = WhiteText,
                checkedTrackColor = PrimaryOrange,
                uncheckedThumbColor = WhiteText,
                uncheckedTrackColor = ChipUnselected
            )
        )
    }
}

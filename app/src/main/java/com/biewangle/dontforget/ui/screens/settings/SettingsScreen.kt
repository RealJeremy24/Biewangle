package com.biewangle.dontforget.ui.screens.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.biewangle.dontforget.R
import com.biewangle.dontforget.ui.theme.scaledSp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.biewangle.dontforget.ui.components.StatsCard
import com.biewangle.dontforget.ui.theme.BackgroundWarm
import com.biewangle.dontforget.ui.theme.CardWhite
import com.biewangle.dontforget.ui.theme.ChipUnselected
import com.biewangle.dontforget.ui.theme.EncourageBg
import com.biewangle.dontforget.ui.theme.PrimaryOrange
import com.biewangle.dontforget.ui.theme.TextDarkBrown
import com.biewangle.dontforget.ui.theme.TextWarmGray
import com.biewangle.dontforget.ui.theme.WhiteText
import com.biewangle.dontforget.util.SoundEffectPlayer
import androidx.compose.ui.platform.LocalContext

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
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()

    // 统计详情弹窗
    val detailDialog by viewModel.detailDialog.collectAsState()
    val totalDetailData by viewModel.totalDetailData.collectAsState()
    val completedDetailData by viewModel.completedDetailData.collectAsState()
    val rateDetailData by viewModel.rateDetailData.collectAsState()

    val context = LocalContext.current

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
                .padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 0.dp)
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
                Spacer(Modifier.weight(1f))
                Image(
                    painter = painterResource(id = R.drawable.mama5),
                    contentDescription = "妈妈贴纸",
                    modifier = Modifier.size(52.dp)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // 时段选择芯片
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatsPeriod.entries.forEach { period ->
                FilterChip(
                    selected = selectedPeriod == period,
                    onClick = {
                        SoundEffectPlayer.playButtonClick(context)
                        viewModel.selectPeriod(period)
                    },
                    label = {
                        Text(
                            text = period.label,
                            fontSize = scaledSp(18),
                            fontWeight = if (selectedPeriod == period) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedPeriod == period) WhiteText else TextDarkBrown
                        )
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = ChipUnselected,
                        selectedContainerColor = PrimaryOrange,
                        labelColor = TextDarkBrown,
                        selectedLabelColor = WhiteText
                    )
                )
            }
        }

        Spacer(Modifier.height(12.dp))

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
                label = "事项",
                modifier = Modifier.weight(1f),
                onClick = {
                    SoundEffectPlayer.playButtonClick(context)
                    viewModel.showDetailDialog(StatsDetailDialog.TOTAL)
                }
            )
            StatsCard(
                icon = "✅",
                value = "${stats.completed}",
                label = "已完成",
                modifier = Modifier.weight(1f),
                onClick = {
                    SoundEffectPlayer.playButtonClick(context)
                    viewModel.showDetailDialog(StatsDetailDialog.COMPLETED)
                }
            )
            StatsCard(
                icon = "📊",
                value = stats.ratePercent,
                label = "完成率",
                modifier = Modifier.weight(1f),
                onClick = {
                    SoundEffectPlayer.playButtonClick(context)
                    viewModel.showDetailDialog(StatsDetailDialog.RATE)
                }
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
            onCheckedChange = {
                SoundEffectPlayer.playSwitchToggle()
                viewModel.toggleVibrateEnabled()
            }
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

        // 底部留白，滚动到底时紧贴导航栏顶部
        Spacer(Modifier.height(120.dp))
    }

    // ── 统计详情弹窗 ──

    // 总事项详情
    if (detailDialog == StatsDetailDialog.TOTAL) {
        TotalDetailDialog(
            data = totalDetailData,
            onDismiss = { viewModel.hideDetailDialog() }
        )
    }

    // 已完成详情
    if (detailDialog == StatsDetailDialog.COMPLETED) {
        CompletedDetailDialog(
            data = completedDetailData,
            onDismiss = { viewModel.hideDetailDialog() }
        )
    }

    // 完成率详情
    if (detailDialog == StatsDetailDialog.RATE) {
        RateDetailDialog(
            data = rateDetailData,
            onDismiss = { viewModel.hideDetailDialog() }
        )
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
                onValueChange = {
                    val newPos = it.roundToInt()
                    if (newPos != sliderPosition) {
                        SoundEffectPlayer.playSliderStep()
                    }
                    onSliderChange(newPos)
                },
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

// ─────────────────────────────────────────────
// 统计详情弹窗
// ─────────────────────────────────────────────

/** 总事项 — 按时段分组 */
@Composable
private fun TotalDetailDialog(
    data: TotalDetailData,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BackgroundWarm,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                "📝 全部事项",
                fontSize = scaledSp(26),
                fontWeight = FontWeight.Bold,
                color = PrimaryOrange,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "共 ${data.total} 条事项",
                    fontSize = scaledSp(18),
                    color = TextWarmGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                if (data.groups.isEmpty()) {
                    Text(
                        "还没有事项\n创建一个备忘吧！",
                        fontSize = scaledSp(20),
                        color = TextWarmGray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    )
                } else {
                    data.groups.forEach { group ->
                        // 分组标题
                        Text(
                            text = "📅 ${group.groupLabel}",
                            fontSize = scaledSp(18),
                            fontWeight = FontWeight.Bold,
                            color = TextDarkBrown,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        group.items.forEach { memo ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(CardWhite, RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    if (memo.isCompleted) "✅" else "⬜",
                                    fontSize = 18.sp
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    memo.title,
                                    fontSize = scaledSp(20),
                                    color = TextDarkBrown,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            DialogCloseButton(onDismiss)
        }
    )
}

/** 已完成 — 按完成日期分组 */
@Composable
private fun CompletedDetailDialog(
    data: CompletedDetailData,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BackgroundWarm,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                "✅ 已完成事项",
                fontSize = scaledSp(26),
                fontWeight = FontWeight.Bold,
                color = PrimaryOrange,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "共完成 ${data.total} 条",
                    fontSize = scaledSp(18),
                    color = TextWarmGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                if (data.groups.isEmpty()) {
                    Text(
                        "还没有完成的事项\n开始行动吧！",
                        fontSize = scaledSp(20),
                        color = TextWarmGray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    )
                } else {
                    data.groups.forEach { group ->
                        // 日期标题
                        Text(
                            text = "📅 ${group.dateLabel}",
                            fontSize = scaledSp(18),
                            fontWeight = FontWeight.Bold,
                            color = TextDarkBrown,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        group.items.forEach { memo ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(CardWhite, RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("✅", fontSize = 18.sp)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    memo.title,
                                    fontSize = scaledSp(20),
                                    color = TextDarkBrown,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            DialogCloseButton(onDismiss)
        }
    )
}

/** 完成率 — 圆环进度 + 妈妈贴纸 */
@Composable
private fun RateDetailDialog(
    data: RateDetailData,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BackgroundWarm,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                "📊 完成率",
                fontSize = scaledSp(26),
                fontWeight = FontWeight.Bold,
                color = PrimaryOrange,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            val fraction = if (data.total > 0) data.completed.toFloat() / data.total else 0f
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 圆环进度 — 百分比在环心
                Box(
                    modifier = Modifier.size(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeW = 20.dp.toPx()
                        val diameter = size.minDimension - strokeW
                        val topLeft = Offset(
                            (size.width - diameter) / 2f,
                            (size.height - diameter) / 2f
                        )
                        val arcSize = Size(diameter, diameter)
                        // 底环
                        drawArc(
                            color = ChipUnselected,
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeW, cap = StrokeCap.Round)
                        )
                        // 进度环
                        drawArc(
                            color = PrimaryOrange,
                            startAngle = -90f,
                            sweepAngle = 360f * fraction,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeW, cap = StrokeCap.Round)
                        )
                    }
                    Text(
                        data.ratePercent,
                        fontSize = scaledSp(44),
                        fontWeight = FontWeight.Bold,
                        color = PrimaryOrange
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "${data.completed} / ${data.total} 条已完成",
                    fontSize = scaledSp(18),
                    color = TextWarmGray
                )
                Spacer(modifier = Modifier.height(8.dp))
                // 妈妈贴纸 — 底边贴紧下方关闭按钮
                Image(
                    painter = painterResource(id = R.drawable.mama7),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                )
                // 关闭按钮移入正文区，贴纸零间距压在其上沿
                DialogCloseButton(onDismiss)
            }
        },
        confirmButton = {}
    )
}

/** 弹窗底部大关闭按钮 */
@Composable
private fun DialogCloseButton(onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(PrimaryOrange, RoundedCornerShape(12.dp))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            "关  闭",
            fontSize = scaledSp(22),
            fontWeight = FontWeight.Bold,
            color = WhiteText,
            letterSpacing = 4.sp
        )
    }
}

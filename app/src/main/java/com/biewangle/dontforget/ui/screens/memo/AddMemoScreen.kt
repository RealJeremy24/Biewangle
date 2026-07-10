package com.biewangle.dontforget.ui.screens.memo

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import android.widget.Toast
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.biewangle.dontforget.BiewangleApp
import com.biewangle.dontforget.data.model.RepeatType
import com.biewangle.dontforget.ui.components.TimePickerDialog
import com.biewangle.dontforget.ui.theme.AlertOrangeRed
import com.biewangle.dontforget.ui.theme.BackgroundWarm
import com.biewangle.dontforget.ui.theme.CardWhite
import com.biewangle.dontforget.ui.theme.ChipSelected
import com.biewangle.dontforget.ui.theme.ChipUnselected
import com.biewangle.dontforget.ui.theme.DividerWarm
import com.biewangle.dontforget.ui.theme.EncourageBg
import com.biewangle.dontforget.ui.theme.PrimaryOrange
import com.biewangle.dontforget.ui.theme.TextDarkBrown
import com.biewangle.dontforget.ui.theme.TextWarmGray
import com.biewangle.dontforget.ui.theme.WhiteText
import com.biewangle.dontforget.ui.theme.scaledSp
import com.biewangle.dontforget.util.DateTimeUtils
import com.biewangle.dontforget.util.SoundEffectPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMemoSheet(viewModel: MemoViewModel) {
    val formState by viewModel.formState.collectAsState()
    val context = LocalContext.current

    // 日期选择器状态
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showRingtoneDialog by remember { mutableStateOf(false) }

    // 铃声文件名称（用于显示）
    var ringtoneDisplayName by remember { mutableStateOf("默认铃声（冲凉最舒适）") }
    var isUsingDefaultRingtone by remember { mutableStateOf(true) }

    // 铃声文件选择器（从 RingtonePickerDialog 中触发）
    val ringtoneFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            var displayName = "自定义铃声"
            context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex >= 0) {
                    displayName = cursor.getString(nameIndex)
                }
            }
            ringtoneDisplayName = displayName
            isUsingDefaultRingtone = false
            // 保存到全局铃声设置
            CoroutineScope(Dispatchers.IO).launch {
                val app = context.applicationContext as BiewangleApp
                val config = app.settingsRepository.getRingtoneConfig()
                app.settingsRepository.saveRingtoneConfig(
                    config.copy(uri = it.toString(), displayName = displayName)
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BackgroundWarm)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        // 标题
        Text(
            text = if (formState.id == 0L) "添加新事项" else "编辑事项",
            fontSize = scaledSp(28),
            fontWeight = FontWeight.Bold,
            color = TextDarkBrown,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // 分隔线
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(DividerWarm)
                .padding(bottom = 16.dp)
        )

        // 标题输入框
        Text(
            text = "标题",
            fontSize = scaledSp(20),
            fontWeight = FontWeight.Medium,
            color = TextDarkBrown,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        TextField(
            value = formState.title,
            onValueChange = { viewModel.updateTitle(it) },
            placeholder = { Text("例如：吃降压药", fontSize = scaledSp(20)) },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = scaledSp(22)),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = CardWhite,
                unfocusedContainerColor = CardWhite,
                focusedIndicatorColor = PrimaryOrange,
                cursorColor = PrimaryOrange
            ),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(Modifier.height(16.dp))

        // 内容输入框
        Text(
            text = "内容（可选）",
            fontSize = scaledSp(20),
            fontWeight = FontWeight.Medium,
            color = TextDarkBrown,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        TextField(
            value = formState.content,
            onValueChange = { viewModel.updateContent(it) },
            placeholder = { Text("补充说明…", fontSize = scaledSp(20)) },
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = scaledSp(22)),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = CardWhite,
                unfocusedContainerColor = CardWhite,
                focusedIndicatorColor = PrimaryOrange,
                cursorColor = PrimaryOrange
            ),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(Modifier.height(16.dp))

        // 日期选择行
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(CardWhite, RoundedCornerShape(12.dp))
                .clickable {
                    SoundEffectPlayer.playButtonClick(context)
                    showDatePicker = true
                }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("📅", fontSize = 24.sp)
            Spacer(Modifier.width(12.dp))
            Text(
                text = DateTimeUtils.formatFullDate(formState.targetDate),
                fontSize = scaledSp(22),
                color = TextDarkBrown,
                modifier = Modifier.weight(1f)
            )
            Text("▼", fontSize = 20.sp, color = TextWarmGray)
        }

        Spacer(Modifier.height(16.dp))

        // 提醒开关 + 时间
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(CardWhite, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("⏰", fontSize = 24.sp)
            Spacer(Modifier.width(12.dp))
            Text(
                text = "提醒时间",
                fontSize = scaledSp(22),
                color = TextDarkBrown,
                modifier = Modifier.weight(1f)
            )
            if (formState.reminderEnabled) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            SoundEffectPlayer.playButtonClick(context)
                            showTimePicker = true
                        }
                        .background(EncourageBg, RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = String.format("%02d:%02d", formState.reminderHour, formState.reminderMinute),
                        fontSize = scaledSp(22),
                        fontWeight = FontWeight.Bold,
                        color = PrimaryOrange
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("›", fontSize = scaledSp(22), color = PrimaryOrange)
                }
            }
            Switch(
                checked = formState.reminderEnabled,
                onCheckedChange = {
                    SoundEffectPlayer.playButtonClick(context)
                    viewModel.toggleReminder(it)
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = WhiteText,
                    checkedTrackColor = PrimaryOrange,
                    uncheckedThumbColor = WhiteText,
                    uncheckedTrackColor = ChipUnselected
                )
            )
        }

        // 使用提醒铃声（仅在开启提醒时显示）
        if (formState.reminderEnabled) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .background(CardWhite, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🔔", fontSize = 24.sp)
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "提醒铃声",
                    fontSize = scaledSp(22),
                    color = TextDarkBrown,
                    modifier = Modifier.weight(1f)
                )
                // 点击弹出铃声选择弹窗
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            SoundEffectPlayer.playButtonClick(context)
                            showRingtoneDialog = true
                        }
                        .background(EncourageBg, RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = ringtoneDisplayName,
                        fontSize = scaledSp(20),
                        fontWeight = FontWeight.Medium,
                        color = PrimaryOrange,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("›", fontSize = scaledSp(22), color = PrimaryOrange)
                }
                Switch(
                    checked = formState.useCustomRingtone,
                    onCheckedChange = {
                        SoundEffectPlayer.playButtonClick(context)
                        viewModel.toggleCustomRingtone(it)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = WhiteText,
                        checkedTrackColor = PrimaryOrange,
                        uncheckedThumbColor = WhiteText,
                        uncheckedTrackColor = ChipUnselected
                    )
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // 重复方式
        Text(
            text = "重复方式",
            fontSize = scaledSp(20),
            fontWeight = FontWeight.Medium,
            color = TextDarkBrown,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RepeatType.entries.forEach { type ->
                val selected = formState.repeatType == type
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            SoundEffectPlayer.playButtonClick(context)
                            viewModel.updateRepeatType(type)
                        }
                        .background(
                            if (selected) ChipSelected else ChipUnselected,
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = type.label,
                        fontSize = scaledSp(22),
                        fontWeight = FontWeight.Medium,
                        color = if (selected) WhiteText else TextDarkBrown,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        softWrap = false
                    )
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        // 保存按钮
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable {
                    SoundEffectPlayer.playButtonClick(context)
                    viewModel.saveMemo()
                }
                .background(PrimaryOrange, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "保  存  事  项",
                fontSize = scaledSp(26),
                fontWeight = FontWeight.Bold,
                color = WhiteText,
                letterSpacing = 4.sp
            )
        }

        // 编辑模式下的删除按钮
        if (formState.id != 0L) {
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        SoundEffectPlayer.playButtonClick(context)
                        viewModel.deleteMemo(formState.id)
                        viewModel.hideForm()
                    }
                    .background(AlertOrangeRed, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "删  除  事  项",
                    fontSize = scaledSp(26),
                    fontWeight = FontWeight.Bold,
                    color = WhiteText,
                    letterSpacing = 4.sp
                )
            }
        }

        Spacer(Modifier.height(32.dp))
    }

    // 日期选择对话框
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = formState.targetDate,
            selectableDates = MinTodaySelectableDates
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    SoundEffectPlayer.playButtonClick(context)
                    val selected = datePickerState.selectedDateMillis
                    if (selected != null) {
                        val todayStart = java.util.Calendar.getInstance().apply {
                            set(java.util.Calendar.HOUR_OF_DAY, 0)
                            set(java.util.Calendar.MINUTE, 0)
                            set(java.util.Calendar.SECOND, 0)
                            set(java.util.Calendar.MILLISECOND, 0)
                        }.timeInMillis
                        if (selected < todayStart) {
                            Toast.makeText(context, "不能选择过去的日期哦～", Toast.LENGTH_SHORT).show()
                            return@TextButton
                        }
                        viewModel.updateDate(selected)
                    }
                    showDatePicker = false
                }) { Text("确定", fontSize = scaledSp(22)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    SoundEffectPlayer.playButtonClick(context)
                    showDatePicker = false
                }) { Text("取消", fontSize = scaledSp(22)) }
            }
        ) {
            DatePicker(
                state = datePickerState,
                title = null,
                showModeToggle = false,
                colors = DatePickerDefaults.colors(
                    containerColor = CardWhite,
                    titleContentColor = TextDarkBrown,
                    headlineContentColor = TextDarkBrown,
                    weekdayContentColor = TextWarmGray,
                    dayContentColor = TextDarkBrown,
                    disabledDayContentColor = Color(0xFF9E9E9E),
                    selectedDayContentColor = WhiteText,
                    selectedDayContainerColor = PrimaryOrange,
                    todayContentColor = PrimaryOrange,
                    todayDateBorderColor = PrimaryOrange,
                ),
                headline = {
                    // 居中日期
                    Text(
                        text = remember(datePickerState.selectedDateMillis) {
                            datePickerState.selectedDateMillis?.let {
                                DateTimeUtils.formatFullDate(it)
                            } ?: "选择日期"
                        },
                        fontSize = scaledSp(28),
                        fontWeight = FontWeight.Bold,
                        color = TextDarkBrown,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            )
            // 日期点击音效：跳过首次触发（dialog 打开时的初始值），之后点击不同日期播放音效
            var dateHasInteracted by remember { mutableStateOf(false) }
            LaunchedEffect(datePickerState.selectedDateMillis) {
                if (dateHasInteracted) {
                    SoundEffectPlayer.playDateClick(context)
                } else {
                    dateHasInteracted = true
                }
            }
        }
    }

    // 时间选择对话框
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = formState.reminderHour,
            initialMinute = formState.reminderMinute,
            is24Hour = true
        )
        // 时间拨动音效：监听 hour/minute 变化
        LaunchedEffect(timePickerState.hour, timePickerState.minute) {
            SoundEffectPlayer.playTimeScroll(context)
        }

        TimePickerDialog(
            onDismiss = { showTimePicker = false },
            onConfirm = {
                viewModel.updateReminderTime(timePickerState.hour, timePickerState.minute)
                showTimePicker = false
            }
        ) {
            TimePicker(
                state = timePickerState,
                colors = TimePickerDefaults.colors(
                    clockDialColor = EncourageBg,
                    selectorColor = PrimaryOrange,
                    clockDialSelectedContentColor = TextDarkBrown,
                    clockDialUnselectedContentColor = TextWarmGray,
                    timeSelectorSelectedContentColor = TextDarkBrown,
                    timeSelectorUnselectedContentColor = TextWarmGray
                )
            )
        }
    }

    // ── 铃声选择弹窗 ──
    if (showRingtoneDialog) {
        RingtonePickerDialog(
            currentDisplayName = ringtoneDisplayName,
            hasCustomRingtone = !isUsingDefaultRingtone,
            isUsingDefault = isUsingDefaultRingtone,
            onSelectDefault = {
                isUsingDefaultRingtone = true
                ringtoneDisplayName = "默认铃声（冲凉最舒适）"
                viewModel.toggleCustomRingtone(true) // 使用 App 自定义铃声（默认 raw）
                // 清除自定义 URI
                CoroutineScope(Dispatchers.IO).launch {
                    val app = context.applicationContext as BiewangleApp
                    val config = app.settingsRepository.getRingtoneConfig()
                    app.settingsRepository.saveRingtoneConfig(
                        config.copy(uri = "", displayName = "默认铃声（冲凉最舒适）")
                    )
                }
            },
            onSelectCustom = {
                isUsingDefaultRingtone = false
                viewModel.toggleCustomRingtone(true)
            },
            onPickFile = {
                ringtoneFilePicker.launch("audio/*")
            },
            onDismiss = { showRingtoneDialog = false }
        )
    }
}

/**
 * 日期可选范围：今天及以后才可选（过去的日期在 DatePicker 中灰显 + 不可点）。
 * 与 confirm 时 "selected < todayStart" 的 toast 兜底逻辑保持一致。
 */
@OptIn(ExperimentalMaterial3Api::class)
private object MinTodaySelectableDates : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
        val todayStart = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        return utcTimeMillis >= todayStart
    }

    override fun isSelectableYear(year: Int): Boolean {
        return year >= java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
    }
}

package com.biewangle.dontforget.ui.screens.memo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.biewangle.dontforget.ui.theme.BackgroundWarm
import com.biewangle.dontforget.ui.theme.CardWhite
import com.biewangle.dontforget.ui.theme.PrimaryOrange
import com.biewangle.dontforget.ui.theme.TextDarkBrown
import com.biewangle.dontforget.ui.theme.TextWarmGray
import com.biewangle.dontforget.ui.theme.scaledSp
import com.biewangle.dontforget.util.SoundEffectPlayer

/**
 * 铃声选择弹窗：
 * - 默认铃声（biewangle.m4a）
 * - 用户自定义铃声（如有）
 * - 底部「从本地选择音频」按钮
 */
@Composable
fun RingtonePickerDialog(
    currentDisplayName: String,
    hasCustomRingtone: Boolean,
    isUsingDefault: Boolean,
    onSelectDefault: () -> Unit,
    onSelectCustom: () -> Unit,
    onPickFile: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(BackgroundWarm, RoundedCornerShape(20.dp))
                .padding(24.dp)
        ) {
            // 标题
            Text(
                text = "选择提醒铃声",
                fontSize = scaledSp(26),
                fontWeight = FontWeight.Bold,
                color = TextDarkBrown
            )

            Spacer(Modifier.height(20.dp))

            // ── 默认铃声选项 ──
            RingtoneOption(
                icon = "🔔",
                title = "默认铃声（冲凉最舒适）",
                isSelected = isUsingDefault,
                onClick = {
                    SoundEffectPlayer.playButtonClick(context)
                    onSelectDefault()
                    onDismiss()
                }
            )

            Spacer(Modifier.height(8.dp))

            // ── 自定义铃声选项（如有） ──
            if (hasCustomRingtone) {
                RingtoneOption(
                    icon = "🎵",
                    title = currentDisplayName,
                    isSelected = !isUsingDefault,
                    onClick = {
                        SoundEffectPlayer.playButtonClick(context)
                        onSelectCustom()
                        onDismiss()
                    }
                )
                Spacer(Modifier.height(8.dp))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(TextWarmGray.copy(alpha = 0.2f))
            )
            Spacer(Modifier.height(8.dp))

            // ── 从本地选择音频 ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .background(CardWhite, RoundedCornerShape(12.dp))
                    .clickable {
                        SoundEffectPlayer.playButtonClick(context)
                        onPickFile()
                        onDismiss()
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📁", fontSize = 20.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "从本地选择音频",
                        fontSize = scaledSp(20),
                        fontWeight = FontWeight.Medium,
                        color = PrimaryOrange
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── 取消按钮 ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        SoundEffectPlayer.playButtonClick(context)
                        onDismiss()
                    },
                contentAlignment = Alignment.Center
            ) {
                TextButton(onClick = onDismiss) {
                    Text("取消", fontSize = scaledSp(20), color = TextWarmGray)
                }
            }
        }
    }
}

@Composable
private fun RingtoneOption(
    icon: String,
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isSelected) PrimaryOrange.copy(alpha = 0.12f) else CardWhite,
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 22.sp)
            Spacer(Modifier.width(12.dp))
            Text(
                title,
                fontSize = scaledSp(20),
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) PrimaryOrange else TextDarkBrown,
                modifier = Modifier.weight(1f)
            )
            if (isSelected) {
                Text("✓", fontSize = 22.sp, color = PrimaryOrange, fontWeight = FontWeight.Bold)
            }
        }
    }
}

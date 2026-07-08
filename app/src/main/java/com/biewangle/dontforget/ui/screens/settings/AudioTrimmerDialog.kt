package com.biewangle.dontforget.ui.screens.settings

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.biewangle.dontforget.data.model.RingtoneConfig
import com.biewangle.dontforget.service.ReminderPlayer
import com.biewangle.dontforget.ui.theme.AlertOrangeRed
import com.biewangle.dontforget.ui.theme.BackgroundWarm
import com.biewangle.dontforget.ui.theme.CardWhite
import com.biewangle.dontforget.ui.theme.PrimaryOrange
import com.biewangle.dontforget.ui.theme.TextDarkBrown
import com.biewangle.dontforget.ui.theme.TextWarmGray
import com.biewangle.dontforget.ui.theme.WhiteText
import com.biewangle.dontforget.ui.theme.scaledSp
import com.biewangle.dontforget.util.SoundEffectPlayer

@Composable
fun AudioTrimmerDialog(
    ringtoneConfig: RingtoneConfig,
    onDismiss: () -> Unit,
    onSave: (startMs: Long, endMs: Long) -> Unit
) {
    val context = LocalContext.current
    val totalDuration = remember(ringtoneConfig) {
        getAudioDurationMs(context, ringtoneConfig.uri)
    }
    val effectiveDuration = if (totalDuration > 0) totalDuration else 30000L

    var trimStartMs by remember { mutableStateOf(ringtoneConfig.trimStartMs.toFloat()) }
    var trimEndMs by remember { mutableStateOf(
        if (ringtoneConfig.trimEndMs > 0) ringtoneConfig.trimEndMs.toFloat()
        else effectiveDuration.toFloat()
    )}

    var isPreviewPlaying by remember { mutableStateOf(false) }
    var previewPlayer by remember { mutableStateOf<ReminderPlayer?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(BackgroundWarm, RoundedCornerShape(20.dp))
                .padding(24.dp)
        ) {
            // 标题
            Text(
                text = "裁剪铃声",
                fontSize = scaledSp(26),
                fontWeight = FontWeight.Bold,
                color = TextDarkBrown,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = "已选择：${ringtoneConfig.displayName}",
                fontSize = scaledSp(18),
                color = TextWarmGray,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // 波形模拟条
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(CardWhite, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "音频波形",
                    fontSize = scaledSp(20),
                    color = TextWarmGray
                )
            }

            Spacer(Modifier.height(16.dp))

            // 起始时间滑块
            Text(
                text = "起始：${formatMs(trimStartMs.toLong())}",
                fontSize = scaledSp(20),
                color = TextDarkBrown
            )
            Slider(
                value = trimStartMs,
                onValueChange = {
                    if (it < trimEndMs - 1000) trimStartMs = it
                },
                valueRange = 0f..effectiveDuration.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = PrimaryOrange,
                    activeTrackColor = PrimaryOrange
                )
            )

            Spacer(Modifier.height(8.dp))

            // 结束时间滑块
            Text(
                text = "结束：${formatMs(trimEndMs.toLong())}",
                fontSize = scaledSp(20),
                color = TextDarkBrown
            )
            Slider(
                value = trimEndMs,
                onValueChange = {
                    if (it > trimStartMs + 1000) trimEndMs = it
                },
                valueRange = 0f..effectiveDuration.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = AlertOrangeRed,
                    activeTrackColor = AlertOrangeRed
                )
            )

            Spacer(Modifier.height(8.dp))

            // 裁剪时长
            Text(
                text = "截取片段时长：${formatMs((trimEndMs - trimStartMs).toLong())}",
                fontSize = scaledSp(20),
                fontWeight = FontWeight.Medium,
                color = PrimaryOrange
            )

            Spacer(Modifier.height(16.dp))

            // 按钮行
            Row(modifier = Modifier.fillMaxWidth()) {
                // 试听按钮
                Button(
                    onClick = {
                        SoundEffectPlayer.playButtonClick(context)
                        if (isPreviewPlaying) {
                            previewPlayer?.stop()
                            previewPlayer = null
                            isPreviewPlaying = false
                        } else {
                            val player = ReminderPlayer(context)
                            if (ringtoneConfig.uri.isNotEmpty()) {
                                player.startPreview(
                                    Uri.parse(ringtoneConfig.uri),
                                    trimStartMs.toLong(),
                                    trimEndMs.toLong()
                                )
                            }
                            previewPlayer = player
                            isPreviewPlaying = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryOrange,
                        contentColor = WhiteText
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (isPreviewPlaying) "⏹ 停止" else "▶ 试听片段",
                        fontSize = scaledSp(22)
                    )
                }

                Spacer(Modifier.width(8.dp))

                // 保存按钮
                Button(
                    onClick = {
                        SoundEffectPlayer.playButtonClick(context)
                        previewPlayer?.stop()
                        onSave(trimStartMs.toLong(), trimEndMs.toLong())
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryOrange,
                        contentColor = WhiteText
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "💾 保存",
                        fontSize = scaledSp(22)
                    )
                }
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val seconds = ms / 1000
    val min = seconds / 60
    val sec = seconds % 60
    return "${min}分${sec.toString().padStart(2, '0')}秒"
}

private fun getAudioDurationMs(context: android.content.Context, uriString: String): Long {
    if (uriString.isEmpty()) return 30000L
    return try {
        val uri = Uri.parse(uriString)
        val mmr = android.media.MediaMetadataRetriever()
        mmr.setDataSource(context, uri)
        val durationStr = mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
        mmr.release()
        durationStr?.toLongOrNull() ?: 30000L
    } catch (e: Exception) {
        30000L
    }
}

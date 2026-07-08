package com.biewangle.dontforget.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import com.biewangle.dontforget.ui.theme.scaledSp
import com.biewangle.dontforget.util.SoundEffectPlayer

/**
 * Material3 TimePickerDialog 封装，提供大号字体的时间选择对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                SoundEffectPlayer.playButtonClick(context)
                onConfirm()
            }) {
                Text("确定", fontSize = scaledSp(22))
            }
        },
        dismissButton = {
            TextButton(onClick = {
                SoundEffectPlayer.playButtonClick(context)
                onDismiss()
            }) {
                Text("取消", fontSize = scaledSp(22))
            }
        },
        text = {
            content()
        }
    )
}

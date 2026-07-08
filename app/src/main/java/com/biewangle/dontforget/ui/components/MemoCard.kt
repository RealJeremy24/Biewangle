package com.biewangle.dontforget.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.biewangle.dontforget.data.model.Memo
import com.biewangle.dontforget.ui.theme.CardWhite
import com.biewangle.dontforget.ui.theme.CompletedGreen
import com.biewangle.dontforget.ui.theme.PrimaryOrange
import com.biewangle.dontforget.ui.theme.TextDarkBrown
import com.biewangle.dontforget.ui.theme.TextWarmGray
import com.biewangle.dontforget.ui.theme.scaledSp
import com.biewangle.dontforget.util.DateTimeUtils
import com.biewangle.dontforget.util.SoundEffectPlayer

@Composable
fun MemoCard(
    memo: Memo,
    onClick: () -> Unit,
    onToggleComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val alpha = if (memo.isCompleted) 0.5f else 1f
    val context = LocalContext.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(CardWhite, RoundedCornerShape(12.dp))
            .clickable {
                SoundEffectPlayer.playButtonClick(context)
                onClick()
            }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 完成复选框
        Checkbox(
            checked = memo.isCompleted,
            onCheckedChange = {
                SoundEffectPlayer.playButtonClick(context)
                onToggleComplete()
            },
            colors = CheckboxDefaults.colors(
                checkedColor = CompletedGreen,
                uncheckedColor = TextWarmGray
            ),
            modifier = Modifier.alpha(1f) // Checkbox always full opacity
        )

        // 事项内容
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.Center
        ) {
            // 标题 + 时间
            Row(verticalAlignment = Alignment.CenterVertically) {
                memo.formattedReminderTime()?.let { time ->
                    Text(
                        text = time,
                        fontSize = scaledSp(18),
                        color = PrimaryOrange,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                Text(
                    text = memo.title,
                    fontSize = scaledSp(22),
                    color = TextDarkBrown,
                    textDecoration = if (memo.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )
            }

            // 重复方式 + 模板标签
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                if (memo.repeatType != com.biewangle.dontforget.data.model.RepeatType.NONE) {
                    Text(
                        text = memo.repeatType.label,
                        fontSize = scaledSp(16),
                        color = TextWarmGray
                    )
                }
                if (memo.content.isNotEmpty()) {
                    if (memo.repeatType != com.biewangle.dontforget.data.model.RepeatType.NONE) {
                        Text(text = "  ", fontSize = scaledSp(16))
                    }
                    Text(
                        text = memo.content,
                        fontSize = scaledSp(16),
                        color = TextWarmGray,
                        maxLines = 1
                    )
                }
            }
        }

        // 编辑按钮
        IconButton(
            onClick = onClick,
            modifier = Modifier.alpha(1f)
        ) {
            Text(
                text = "⋮",
                fontSize = scaledSp(24),
                color = TextWarmGray
            )
        }
    }
}

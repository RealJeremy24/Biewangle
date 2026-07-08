package com.biewangle.dontforget.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.biewangle.dontforget.ui.theme.PrimaryOrange
import com.biewangle.dontforget.ui.theme.WhiteText
import com.biewangle.dontforget.ui.theme.scaledSp
import com.biewangle.dontforget.util.SoundEffectPlayer

/**
 * 大号主按钮组件（最小64dp高，宽填满）
 */
@Composable
fun LargeButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = PrimaryOrange,
    textColor: Color = WhiteText,
    height: Int = 64
) {
    val context = LocalContext.current
    Button(
        onClick = {
            SoundEffectPlayer.playButtonClick(context)
            onClick()
        },
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = textColor
        ),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    ) {
        Text(
            text = text,
            fontSize = scaledSp(26),
            fontWeight = FontWeight.Bold,
            letterSpacing = 4.sp
        )
    }
}

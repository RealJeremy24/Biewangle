package com.biewangle.dontforget.ui.screens.alarm

import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.biewangle.dontforget.BiewangleApp
import com.biewangle.dontforget.R
import com.biewangle.dontforget.service.AlarmForegroundService
import com.biewangle.dontforget.service.AlarmScheduler
import com.biewangle.dontforget.ui.theme.AlarmBgEnd
import com.biewangle.dontforget.ui.theme.AlarmBgMid
import com.biewangle.dontforget.ui.theme.AlarmBgStart
import com.biewangle.dontforget.ui.theme.AlertOrangeRed
import com.biewangle.dontforget.ui.theme.CardWhite
import com.biewangle.dontforget.ui.theme.GlassWhite
import com.biewangle.dontforget.ui.theme.PrimaryOrange
import com.biewangle.dontforget.ui.theme.TextDarkBrown
import com.biewangle.dontforget.ui.theme.TextWarmGray
import com.biewangle.dontforget.ui.theme.WhiteText
import com.biewangle.dontforget.ui.theme.scaledSp
import com.biewangle.dontforget.ui.theme.BiewangleTheme
import com.biewangle.dontforget.util.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 弹窗提醒 Activity
 *
 * UI 结构:
 *  ┌───────────── 渐变背景 + 装饰 blob ─────────────┐
 *  │ ⏰ 提醒时间到                                    │
 *  │                                                 │
 *  │          [圆形妈妈贴纸]                            │
 *  │  ┌──────────────────────────┐                  │
 *  │  │  打电话                    │  ← 卡片被贴纸压住顶部│
 *  │  │   记得打电话                │                  │
 *  │  └──────────────────────────┘                  │
 *  │ 🔔 铃声循环播放中                ⏱ 1分钟前        │
 *  │ ┌──────[✓] 滑动结束提醒  ─────►─┐  (方案 A)      │
 *  │ ┌────── ⏰ 稍后提醒(10分钟后) ───┐  (方案 C)      │
 *  │ 💡 补充维 C,记得报平安~                              │
 *  └──────────────────────────────────────────┘
 *
 * 主按钮:方案 A — iOS 闹钟风格,拖到底才能结束
 * 次按钮:方案 C — 玻璃拟态轻量感
 */
class AlarmActivity : ComponentActivity() {

    private var memoId: Long = -1L
    private var title: String = ""
    private var content: String = ""
    private var useCustomRingtone: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 全屏、点亮屏幕、覆盖锁屏
        window.apply {
            addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
            addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
            addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
        }

        memoId = intent?.getLongExtra(Constants.EXTRA_MEMO_ID, -1L) ?: -1L
        title = intent?.getStringExtra(Constants.EXTRA_MEMO_TITLE) ?: "提醒"
        content = intent?.getStringExtra(Constants.EXTRA_MEMO_CONTENT) ?: ""
        useCustomRingtone = intent?.getBooleanExtra(Constants.EXTRA_USE_CUSTOM_RINGTONE, true) ?: true

        // 启动前台服务播放铃声
        AlarmForegroundService.start(this, memoId, title, content, useCustomRingtone)

        setContent {
            BiewangleTheme {
                AlarmScreen(
                    title = title,
                    content = content,
                    onDismiss = { dismissAlarm() },
                    onSnooze = { snoozeAlarm() }
                )
            }
        }
    }

    private fun dismissAlarm() {
        AlarmForegroundService.stop(this)
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(memoId.toInt())
        CoroutineScope(Dispatchers.IO).launch {
            val app = BiewangleApp.instance
            val memo = app.memoRepository.getById(memoId)
            if (memo != null) app.memoRepository.toggleComplete(memoId)
        }
        finish()
    }

    private fun snoozeAlarm() {
        AlarmForegroundService.stop(this)
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(memoId.toInt())
        val snoozeTime = System.currentTimeMillis() + Constants.SNOOZE_DELAY_MS
        val scheduler = AlarmScheduler(this)
        scheduler.schedule(memoId, title, content, snoozeTime, useCustomRingtone)
        finish()
    }

    @Deprecated("阻止返回键直接结束提醒")
    override fun onBackPressed() {
        // 必须通过滑动确认或稍后提醒才能结束
    }
}

// ============================================================================
// 主屏幕
// ============================================================================

@Composable
fun AlarmScreen(
    title: String,
    content: String,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(AlarmBgStart, AlarmBgMid, AlarmBgEnd)
                )
            )
    ) {
        // 装饰 blob
        DecorativeBlobTopLeft()
        DecorativeBlobBottomRight(modifier = Modifier.align(Alignment.BottomEnd))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(18.dp))

            // ① 顶部标题
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("⏰", fontSize = 22.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.alarm_title_no_icon),
                    fontSize = scaledSp(26),
                    fontWeight = FontWeight.Bold,
                    color = AlertOrangeRed,
                    letterSpacing = 2.sp
                )
            }

            Spacer(Modifier.height(12.dp))

            // ② 贴纸 + 卡片叠层
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                PhoneCallCard(
                    title = title,
                    content = content,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 110.dp)
                )
                MamaSticker(
                    stickerRes = R.drawable.mama2_clean,
                    modifier = Modifier.size(200.dp)
                )
            }

            Spacer(Modifier.height(18.dp))

            // ③ 状态行
            StatusRow()

            Spacer(Modifier.height(22.dp))

            // ④ 主按钮(方案 A)
            SlideToDismissButton(
                text = stringResource(R.string.alarm_slider_hint),
                onDismiss = onDismiss
            )

            Spacer(Modifier.height(14.dp))

            // ⑤ 次按钮(方案 C)
            GlassSnoozeButton(
                text = stringResource(R.string.snooze_10min),
                onClick = onSnooze
            )

            Spacer(Modifier.height(14.dp))

            // ⑥ 鼓励语
            Text(
                text = stringResource(R.string.encourage_footer),
                fontSize = scaledSp(13),
                color = TextWarmGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))
        }
    }
}

// ============================================================================
// 装饰 blob
// ============================================================================

@Composable
private fun DecorativeBlobTopLeft() {
    Box(
        modifier = Modifier
            .offset(x = (-70).dp, y = (-50).dp)
            .size(260.dp)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        PrimaryOrange.copy(alpha = 0.16f),
                        Color.Transparent
                    )
                ),
                shape = CircleShape
            )
    )
}

@Composable
private fun DecorativeBlobBottomRight(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .offset(x = 90.dp, y = 80.dp)
            .size(280.dp)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        AlertOrangeRed.copy(alpha = 0.10f),
                        Color.Transparent
                    )
                ),
                shape = CircleShape
            )
    )
}

// ============================================================================
// 主卡片
// ============================================================================

@Composable
private fun PhoneCallCard(
    title: String,
    content: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = AlertOrangeRed.copy(alpha = 0.18f)
            )
            .clip(RoundedCornerShape(28.dp))
            .background(CardWhite)
            .padding(start = 24.dp, end = 24.dp, top = 88.dp, bottom = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            fontSize = scaledSp(30),
            fontWeight = FontWeight.Bold,
            color = TextDarkBrown,
            textAlign = TextAlign.Center,
            letterSpacing = 3.sp
        )
        if (content.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = content,
                fontSize = scaledSp(17),
                color = TextWarmGray,
                textAlign = TextAlign.Center,
                letterSpacing = 1.sp
            )
        }
    }
}

// ============================================================================
// 妈妈贴纸:圆形 + 白色描边 + 入场弹跳 + 呼吸
// ============================================================================

@Composable
private fun MamaSticker(
    stickerRes: Int,
    modifier: Modifier = Modifier
) {
    // 入场动画:0.5 → 1.0,带过冲
    val entranceScale = remember { Animatable(0.4f) }
    LaunchedEffect(Unit) {
        entranceScale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
        )
    }

    // 持续呼吸
    val infiniteTransition = rememberInfiniteTransition(label = "sticker")
    val breatheScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )

    val finalScale = entranceScale.value * breatheScale

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = finalScale
                scaleY = finalScale
            }
            .shadow(
                elevation = 10.dp,
                shape = CircleShape,
                ambientColor = PrimaryOrange.copy(alpha = 0.30f)
            )
            .clip(CircleShape)
            .background(WhiteText)
            .border(6.dp, WhiteText, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(stickerRes),
            contentDescription = "温馨提醒",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
        )
    }
}

// ============================================================================
// 状态行:铃声抖动 + 时间芯片
// ============================================================================

@Composable
private fun StatusRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BellSway()
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.alarm_playing),
                fontSize = scaledSp(15),
                color = AlertOrangeRed,
                fontWeight = FontWeight.Medium
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(GlassWhite)
                .border(
                    1.dp,
                    WhiteText.copy(alpha = 0.5f),
                    RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 12.dp, vertical = 5.dp)
        ) {
            Text(
                text = stringResource(R.string.alarm_time_chip),
                fontSize = scaledSp(13),
                color = TextWarmGray,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * 铃铛钟摆动画 -12°↔+12°
 */
@Composable
private fun BellSway() {
    val infiniteTransition = rememberInfiniteTransition(label = "bell")
    val angle by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 320, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "swayAngle"
    )
    Text(
        text = "🔔",
        fontSize = 18.sp,
        modifier = Modifier.rotate(angle)
    )
}

// ============================================================================
// 主按钮:滑动关闭(方案 A)
// ============================================================================

@Composable
private fun SlideToDismissButton(
    text: String,
    onDismiss: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val trackHeight = 72.dp
    val thumbSize = 60.dp

    // 容器总宽 ≈ 屏幕宽 - 左右 padding(22*2)
    val trackWidthDp = configuration.screenWidthDp - 44
    val trackWidthPx = with(density) { trackWidthDp.dp.toPx() }
    val thumbPx = with(density) { thumbSize.toPx() }
    val safeInset = with(density) { 12.dp.toPx() }
    val maxOffset = (trackWidthPx - thumbPx - safeInset).coerceAtLeast(0f)

    val offsetX = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(trackHeight)
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(36.dp),
                ambientColor = AlertOrangeRed.copy(alpha = 0.35f)
            )
            .clip(RoundedCornerShape(36.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFFFF8A65), AlertOrangeRed)
                )
            )
    ) {
        // 提示文字
        Text(
            text = text,
            color = WhiteText,
            fontSize = scaledSp(20),
            fontWeight = FontWeight.Bold,
            letterSpacing = 4.sp,
            modifier = Modifier.align(Alignment.Center)
        )

        // 拖动圆球
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = offsetX.value.roundToInt(),
                        y = 0
                    )
                }
                .padding(6.dp)
                .align(Alignment.CenterStart)
                .size(thumbSize - 12.dp)
                .shadow(4.dp, CircleShape)
                .clip(CircleShape)
                .background(WhiteText)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            // 未到 95% 时回弹
                            if (offsetX.value < maxOffset * 0.95f) {
                                coroutineScope.launch {
                                    offsetX.animateTo(0f, tween(300))
                                }
                            }
                        }
                    ) { _, dragAmount ->
                        val newValue = (offsetX.value + dragAmount).coerceIn(0f, maxOffset)
                        coroutineScope.launch {
                            offsetX.snapTo(newValue)
                            if (newValue >= maxOffset * 0.95f) {
                                onDismiss()
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "✓",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = AlertOrangeRed
            )
        }
    }
}

// ============================================================================
// 次按钮:玻璃拟态稍后提醒(方案 C)
// ============================================================================

@Composable
private fun GlassSnoozeButton(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(28.dp)
            )
            .clip(RoundedCornerShape(28.dp))
            .background(GlassWhite)
            .border(
                width = 1.5.dp,
                color = WhiteText.copy(alpha = 0.7f),
                shape = RoundedCornerShape(28.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "⏰",
                fontSize = 18.sp,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = text,
                fontSize = scaledSp(17),
                color = TextDarkBrown,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

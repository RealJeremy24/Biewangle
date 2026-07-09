package com.biewangle.dontforget.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.biewangle.dontforget.R
import com.biewangle.dontforget.ui.theme.scaledSp
import com.biewangle.dontforget.util.SoundEffectPlayer
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 开场动画时间线：
 *   0.0s – 2.1s   一次性减速 Y 轴旋转 0→1080°（3 整圈，落在正面，越转越慢）
 *   1.45s – 2.1s  收尾期真实照片→卡通头像交叉淡入淡出
 *   2.1s – 2.45s  弹跳放大效果
 *   2.1s          装饰 emoji 陆续出现（💚 ✨）
 *   2.3s          「欢迎回来」标签淡入
 *   3.6s          底部白色面板滑入 + 加载指示圆点
 *   ~5.0s         自动进入主界面
 */
@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    val density = LocalDensity.current
    val context = androidx.compose.ui.platform.LocalContext.current
    var isFinished by remember { mutableStateOf(false) }
    var showAccents by remember { mutableStateOf(false) }
    var showAppUi by remember { mutableStateOf(false) }

    // ── 可控动画值 ──
    val spinRotation = remember { Animatable(0f) }       // Y 轴旋转角度（度）
    val photoAlpha = remember { Animatable(1f) }          // 真实照片透明度
    val cartoonAlpha = remember { Animatable(0f) }        // 卡通头像透明度
    val popScale = remember { Animatable(1f) }            // 弹跳缩放

    // 装饰 emoji 动画
    val heartAnim = remember { Animatable(0f) }           // 💚
    val spark1Anim = remember { Animatable(0f) }          // ✨ 右上
    val spark2Anim = remember { Animatable(0f) }          // ✨ 右下
    val labelAnim = remember { Animatable(0f) }           // 「欢迎回来」

    // 底部面板滑动（1 = 隐藏在下方, 0 = 显示）
    val panelSlide = remember { Animatable(1f) }

    // 3D 相机距离（模拟 CSS perspective）
    val camDist = 12f * density.density

    // ── 动画编排 ──
    LaunchedEffect(Unit) {
        // ═══ 阶段 1：一次性减速旋转 0→1080°（3 整圈，~2.1s）+ 收尾交叉淡入 ═══
        coroutineScope {
            launch {
                spinRotation.animateTo(
                    targetValue = 1080f,  // 3 × 360°，落在正面（非镜像）
                    animationSpec = tween(
                        durationMillis = 2100,
                        // 起手快、尾部强力减速：观感≈2.5圈快转+缓缓定住
                        easing = CubicBezierEasing(0.12f, 0f, 0.2f, 1f)
                    )
                )
            }
            // 收尾最后 ~650ms 做照片→卡通交叉淡入（此时转速已很慢、接近正面）
            launch {
                delay(1450)
                launch { photoAlpha.animateTo(0f, tween(650)) }
                cartoonAlpha.animateTo(1f, tween(650))
            }
        }

        // ═══ 阶段 3：弹跳 + 装饰出现 (2.1s – 2.45s) ═══
        showAccents = true

        // 弹跳效果：1.08 → 1.22 → 1.12
        popScale.animateTo(1.22f, tween(175))
        popScale.animateTo(1.12f, tween(175))

        // 装饰元素错峰出现（非阻塞，在弹跳期间并行播放）
        launch { heartAnim.animateTo(1f, tween(500)) }                 // 2.1s  💚
        launch { delay(150); spark1Anim.animateTo(1f, tween(500)) }    // 2.25s ✨
        launch { delay(200); labelAnim.animateTo(1f, tween(500)) }     // 2.3s  文字
        launch { delay(300); spark2Anim.animateTo(1f, tween(500)) }    // 2.4s  ✨

        // ═══ 阶段 4：底部面板滑入 (3.6s) ═══
        delay(1150)  // 等待 2.45 + 1.15 = 3.6s
        showAppUi = true
        panelSlide.animateTo(
            targetValue = 0f,
            animationSpec = tween(
                durationMillis = 550,
                easing = FastOutSlowInEasing
            )
        )

        // ═══ 阶段 5：自动跳转 (~5.1s) ═══
        delay(1500)
        if (!isFinished) {
            isFinished = true
            onSplashFinished()
        }
    }

    // ═══════════════════════════════════════════════
    // UI 布局
    // ═══════════════════════════════════════════════
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                // 暖色渐变 —— CSS: linear-gradient(160deg, #ffe9d6, #d9f2c4)
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFFE9D6),  // var(--bg1)
                        Color(0xFFD9F2C4)   // var(--bg2)
                    ),
                    start = Offset(0f, Float.POSITIVE_INFINITY),
                    end = Offset(Float.POSITIVE_INFINITY, 0f)
                )
            )
    ) {
        // ── 右上角「跳过」按钮 ──
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 48.dp, end = 20.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.Black.copy(alpha = 0.35f))
                .clickable {
                    if (!isFinished) {
                        SoundEffectPlayer.playButtonClick(context)
                        isFinished = true
                        onSplashFinished()
                    }
                }
                .padding(horizontal = 18.dp, vertical = 8.dp)
        ) {
            Text(
                text = "跳过",
                color = Color.White,
                fontSize = scaledSp(13),
                fontWeight = FontWeight.Medium
            )
        }

        // ═══════════════════════════════════════════════
        // 中央 —— 圆形头像旋转区域
        // ═══════════════════════════════════════════════
        Box(
            modifier = Modifier.align(Alignment.Center),
            contentAlignment = Alignment.Center
        ) {
            // 圆形外框（带阴影 + 弹跳缩放）
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .graphicsLayer {
                        scaleX = popScale.value
                        scaleY = popScale.value
                    }
                    .clip(RoundedCornerShape(32.dp)),
                contentAlignment = Alignment.Center
            ) {
                // ── 真实照片层 ──
                Image(
                    painter = painterResource(id = R.drawable.splash_photo),
                    contentDescription = "头像照片",
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            rotationY = spinRotation.value
                            cameraDistance = camDist
                            transformOrigin = TransformOrigin(0.5f, 0.5f)
                            alpha = photoAlpha.value
                        }
                        .clip(RoundedCornerShape(32.dp)),
                    contentScale = ContentScale.Crop
                )

                // ── 卡通头像层 ──
                Image(
                    painter = painterResource(id = R.drawable.splash_cartoon),
                    contentDescription = "卡通头像",
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            rotationY = spinRotation.value
                            cameraDistance = camDist
                            transformOrigin = TransformOrigin(0.5f, 0.5f)
                            alpha = cartoonAlpha.value
                        }
                        .clip(RoundedCornerShape(32.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            // ── 装饰 emoji（定位相对于圆形中心） ──
            // 💚 绿心 —— 圆形左侧
            if (showAccents) {
                Text(
                    text = "💚",
                    fontSize = 34.sp,
                    modifier = Modifier
                        .offset(x = (-146).dp, y = (-106).dp)
                        .graphicsLayer {
                            alpha = heartAnim.value
                            scaleX = 0.4f + 0.6f * heartAnim.value
                            scaleY = 0.4f + 0.6f * heartAnim.value
                        }
                )
            }
            // ✨ 星闪 1 —— 圆形右上角
            if (showAccents) {
                Text(
                    text = "✨",
                    fontSize = 22.sp,
                    modifier = Modifier
                        .offset(x = 140.dp, y = (-120).dp)
                        .graphicsLayer {
                            alpha = spark1Anim.value
                            scaleX = 0.4f + 0.6f * spark1Anim.value
                            scaleY = 0.4f + 0.6f * spark1Anim.value
                        }
                )
            }
            // ✨ 星闪 2 —— 圆形右中
            if (showAccents) {
                Text(
                    text = "✨",
                    fontSize = 18.sp,
                    modifier = Modifier
                        .offset(x = 152.dp, y = (-56).dp)
                        .graphicsLayer {
                            alpha = spark2Anim.value
                            scaleX = 0.4f + 0.6f * spark2Anim.value
                            scaleY = 0.4f + 0.6f * spark2Anim.value
                        }
                )
            }

            // ── 「欢迎回来」标签 —— 圆形正下方 ──
            if (showAccents) {
                Text(
                    text = "欢迎回来",
                    color = Color(0xFF5C4A3A),
                    fontSize = scaledSp(15),
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .offset(y = 160.dp)
                        .alpha(labelAnim.value)
                )
            }
        }

        // ═══════════════════════════════════════════════
        // 底部 —— 滑入的 App UI 面板
        // ═══════════════════════════════════════════════
        if (showAppUi) {
            val panelHeightPx = with(density) { 200.dp.toPx() }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .offset {
                        IntOffset(
                            x = 0,
                            y = (panelSlide.value * panelHeightPx).roundToInt()
                        )
                    }
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(Color.White)
                    .padding(top = 36.dp, bottom = 48.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "正在进入应用…",
                        color = Color(0xFF999999),
                        fontSize = scaledSp(15)
                    )

                    // 三个弹跳圆点（依次延迟 150ms）
                    Row(
                        modifier = Modifier.padding(top = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (i in 0..2) {
                            BouncingDot(initialDelayMs = i * 150L)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 加载指示弹跳圆点。
 *
 * CSS 原版：opacity 0.5 → 1 → 0.5，周期 1s，三个点依次延迟 0 / 150 / 300ms 起跑。
 */
@Composable
private fun BouncingDot(initialDelayMs: Long) {
    val alpha = remember { Animatable(0.5f) }

    LaunchedEffect(Unit) {
        delay(initialDelayMs)
        while (true) {
            // 上半程：弹起到最亮（400ms，对应 CSS 40%）
            alpha.animateTo(1f, tween(400))
            // 下半程：落回暗态（600ms，对应 CSS 40%→100%）
            alpha.animateTo(0.5f, tween(600))
        }
    }

    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(Color(0xFFCFD6DD))
            .graphicsLayer {
                this.alpha = alpha.value
            }
    )
}

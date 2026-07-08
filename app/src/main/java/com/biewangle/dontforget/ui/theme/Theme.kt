package com.biewangle.dontforget.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// 字体缩放 CompositionLocal — 所有 Text composable 使用 scaledSp() 读取
val LocalFontScale = compositionLocalOf { 1.0f }

/** 根据全局字体缩放比例返回缩放后的 sp 值 */
@Composable
fun scaledSp(base: Int): TextUnit = (base * LocalFontScale.current).sp

/** 根据全局字体缩放比例返回缩放后的 sp 值（Float 版本） */
@Composable
fun scaledSp(base: Float): TextUnit = (base * LocalFontScale.current).sp

private val BiewangleColorScheme = lightColorScheme(
    primary = PrimaryOrange,
    onPrimary = WhiteText,
    primaryContainer = EncourageBg,
    onPrimaryContainer = TextDarkBrown,
    secondary = CompletedGreen,
    onSecondary = WhiteText,
    tertiary = AlertOrangeRed,
    onTertiary = WhiteText,
    background = BackgroundWarm,
    onBackground = TextDarkBrown,
    surface = CardWhite,
    onSurface = TextDarkBrown,
    surfaceVariant = ChipUnselected,
    onSurfaceVariant = TextWarmGray,
    error = AlertOrangeRed,
    onError = WhiteText,
    outline = DividerWarm
)

@Composable
fun BiewangleTheme(
    content: @Composable () -> Unit
) {
    val fontScale by com.biewangle.dontforget.BiewangleApp.instance.fontScaleFlow.collectAsState()
    val colorScheme = BiewangleColorScheme
    val typography = biewangleTypography(fontScale)

    // 设置状态栏颜色
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = BackgroundWarm.toArgb()
            window.navigationBarColor = BackgroundWarm.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography
    ) {
        CompositionLocalProvider(LocalFontScale provides fontScale) {
            content()
        }
    }
}

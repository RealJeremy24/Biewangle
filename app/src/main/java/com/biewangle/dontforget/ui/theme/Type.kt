package com.biewangle.dontforget.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 面向老年用户的大号字体定义。
 * 基准（scale = 1.0f）：正文 22sp，标题 28-32sp。
 * @param fontScale 缩放因子，范围 0.8f ~ 1.5f
 */
fun biewangleTypography(fontScale: Float = 1.0f): Typography {
    fun spScaled(base: Int): Float = (base * fontScale).coerceIn(14f, 52f)

    return Typography(
        headlineLarge = TextStyle(
            fontWeight = FontWeight.Bold,
            fontSize = spScaled(32).sp,
            lineHeight = spScaled(40).sp,
            color = TextDarkBrown
        ),
        headlineMedium = TextStyle(
            fontWeight = FontWeight.Bold,
            fontSize = spScaled(26).sp,
            lineHeight = spScaled(34).sp,
            color = TextDarkBrown
        ),
        titleLarge = TextStyle(
            fontWeight = FontWeight.SemiBold,
            fontSize = spScaled(24).sp,
            lineHeight = spScaled(32).sp,
            color = TextDarkBrown
        ),
        titleMedium = TextStyle(
            fontWeight = FontWeight.Medium,
            fontSize = spScaled(22).sp,
            lineHeight = spScaled(30).sp,
            color = TextDarkBrown
        ),
        bodyLarge = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = spScaled(22).sp,
            lineHeight = spScaled(30).sp,
            color = TextDarkBrown
        ),
        bodyMedium = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = spScaled(20).sp,
            lineHeight = spScaled(28).sp,
            color = TextDarkBrown
        ),
        labelLarge = TextStyle(
            fontWeight = FontWeight.SemiBold,
            fontSize = spScaled(22).sp,
            lineHeight = spScaled(28).sp,
            color = TextDarkBrown
        ),
        labelMedium = TextStyle(
            fontWeight = FontWeight.Medium,
            fontSize = spScaled(18).sp,
            lineHeight = spScaled(24).sp,
            color = TextWarmGray
        ),
        bodySmall = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = spScaled(16).sp,
            lineHeight = spScaled(22).sp,
            color = TextWarmGray
        )
    )
}

// 向后兼容
val BiewangleTypography = biewangleTypography(1.0f)
val LargeTypography = biewangleTypography(1.2f)

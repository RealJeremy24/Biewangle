package com.biewangle.dontforget.util

import android.content.Context
import android.graphics.*
import com.biewangle.dontforget.R

/**
 * 运行时生成通知大图的工具类。
 *
 * 合成图规格：
 * - 暖橙→桃粉径向渐变背景 + 右上暖光晕 + 左下珊瑚暗角
 * - 左侧 mama4 完整可见 (520×520)，双手+捧脸压在框框下边沿
 * - 右上文字块 320×auto，标题 40px / 正文 18px，标题白色假粗 + 阴影
 *
 * 输出 900×560 Bitmap (≈1.6:1)，用于 NotificationCompat.BigPictureStyle。
 */
object NotificationBitmapBuilder {

    // Canvas 逻辑尺寸（px），从原 2:1 (900×450) 调整为 ≈1.6:1
    private const val W = 900f
    private const val H = 560f

    // 圆角半径
    private const val CORNER_RADIUS = 28f

    // 贴纸（mama4）等比缩放后的尺寸 — 完整可见
    private const val STICKER_SIZE = 520f
    private const val STICKER_LEFT = 20f
    private const val STICKER_BOTTOM_OFFSET = 20f  // 超出框下沿 20px（西瓜以下微微伸到框外）

    // 文字块
    private const val TEXT_RIGHT = 44f
    private const val TEXT_TOP = 48f
    private const val TEXT_WIDTH = 320f

    // 字号（标题 56 / 正文 24 — 加大以适应中老年用户，2026-07-09 调整）
    private const val TITLE_SIZE = 56f
    private const val CONTENT_SIZE = 24f

    // 暖橙夕阳配色 — 与 colors.xml 中的 primary_orange / alert_orange_red 对齐
    private val COLOR_TOP_LEFT = Color.rgb(0xFF, 0xB0, 0x88)     // #FFB088 桃粉
    private val COLOR_MID = Color.rgb(0xFF, 0x8A, 0x65)          // #FF8A65 橙红
    private val COLOR_BOTTOM_RIGHT = Color.rgb(0xE6, 0x7E, 0x22) // #E67E22 primary_orange

    private val COLOR_HIGHLIGHT = Color.rgb(0xFF, 0xD2, 0x96)     // 右上暖光晕
    private val COLOR_SHADOW = Color.rgb(0xD8, 0x43, 0x15)        // 左下珊瑚暗角 alert_orange_red

    /**
     * 生成通知大图 Bitmap。
     * @param context 用于加载贴纸资源
     * @param title   提醒标题（动态文字）
     * @param content 提醒正文（动态文字）
     */
    fun build(context: Context, title: String, content: String): Bitmap {
        val bitmap = Bitmap.createBitmap(W.toInt(), H.toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        drawBackground(canvas)
        drawDecorations(canvas)
        drawSticker(context, canvas)
        drawTextBlock(canvas, title, content)

        return bitmap
    }

    // ── 暖橙夕阳背景（圆角矩形 + 多层径向叠加） ──────────────

    private fun drawBackground(canvas: Canvas) {
        val rect = RectF(0f, 0f, W, H)

        canvas.save()
        val clipPath = Path().apply {
            addRoundRect(rect, CORNER_RADIUS, CORNER_RADIUS, Path.Direction.CW)
        }
        canvas.clipPath(clipPath)

        // 底层对角线渐变：桃粉 → 橙红 → 主橙
        val baseGradient = LinearGradient(
            0f, 0f, W, H,
            COLOR_TOP_LEFT, COLOR_BOTTOM_RIGHT,
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(rect, Paint().apply { shader = baseGradient })

        // 右上暖光晕
        val highlightShader = RadialGradient(
            W * 0.78f, H * 0.18f,
            W * 0.55f,
            COLOR_HIGHLIGHT, Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(rect, Paint().apply { shader = highlightShader })

        // 左下珊瑚暗角
        val shadowShader = RadialGradient(
            W * 0.18f, H * 0.88f,
            W * 0.60f,
            withAlpha(COLOR_SHADOW, 0.30f), Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(rect, Paint().apply { shader = shadowShader })

        canvas.restore()
    }

    // ── 装饰光晕 blob（增强温度感） ──────────────────────────

    private fun drawDecorations(canvas: Canvas) {
        canvas.save()
        val clipPath = Path().apply {
            addRoundRect(RectF(0f, 0f, W, H), CORNER_RADIUS, CORNER_RADIUS, Path.Direction.CW)
        }
        canvas.clipPath(clipPath)

        // 右上奶油色光晕（顶层强调）
        val blob1Paint = Paint().apply {
            shader = RadialGradient(
                W - 90f, -110f, 320f,
                Color.argb(153, 0xFF, 0xEC, 0xB3), Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(W - 90f, -110f, 320f, blob1Paint)

        // 左下白色柔光
        val blob2Paint = Paint().apply {
            shader = RadialGradient(
                -70f, H - 80f, 280f,
                Color.argb(64, 0xFF, 0xFF, 0xFF), Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(-70f, H - 80f, 280f, blob2Paint)

        canvas.restore()
    }

    // ── 贴纸 mama4（完整可见，无白圆底，下沿压框） ───────────

    private fun drawSticker(context: Context, canvas: Canvas) {
        val original = BitmapFactory.decodeResource(context.resources, R.drawable.mama4)
        val scaled = Bitmap.createScaledBitmap(
            original,
            STICKER_SIZE.toInt(),
            STICKER_SIZE.toInt(),
            true
        )
        if (scaled != original) original.recycle()

        val left = STICKER_LEFT
        val top = H - STICKER_SIZE + STICKER_BOTTOM_OFFSET  // 底部超出 20px

        // 阴影：仅上方一点点的 drop shadow 效果（用绘制偏移近似）
        val shadowPaint = Paint().apply {
            color = Color.argb(90, 0, 0, 0)
            isAntiAlias = true
            maskFilter = BlurMaskFilter(8f, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawBitmap(scaled, left + 0f, top + 0f, shadowPaint)

        // 实际贴纸
        canvas.drawBitmap(scaled, left, top, null)
    }

    // ── 右上文字块（标题 40 + 正文 18，阴影增强可读性） ──────

    private fun drawTextBlock(canvas: Canvas, title: String, content: String) {
        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = TITLE_SIZE
            isAntiAlias = true
            isFakeBoldText = true
            letterSpacing = 0.025f
            setShadowLayer(4f, 0f, 2f, Color.argb(128, 0, 0, 0))
        }

        val contentPaint = Paint().apply {
            color = Color.WHITE
            textSize = CONTENT_SIZE
            isAntiAlias = true
            setShadowLayer(3f, 0f, 1f, Color.argb(128, 0, 0, 0))
        }

        // 文字块右对齐
        titlePaint.textAlign = Paint.Align.RIGHT
        contentPaint.textAlign = Paint.Align.RIGHT

        val x = W - TEXT_RIGHT
        val maxWidth = TEXT_WIDTH

        // 标题（自动换行）
        val titleLines = breakLines(title, titlePaint, maxWidth)
        val titleLineHeight = titlePaint.descent() - titlePaint.ascent()
        val titleGap = 14f

        var y = TEXT_TOP + titleLineHeight  // 第一行 baseline
        for (line in titleLines) {
            canvas.drawText(line, x, y, titlePaint)
            y += titleLineHeight
        }
        y += titleGap - titleLineHeight  // 标题与正文之间的间距

        // 正文（自动换行）
        val contentLines = breakLines(content, contentPaint, maxWidth)
        val contentLineHeight = contentPaint.descent() - contentPaint.ascent()
        y += contentLineHeight  // 正文第一行 baseline
        for (line in contentLines) {
            canvas.drawText(line, x, y, contentPaint)
            y += contentLineHeight
        }
    }

    // ── 工具方法 ─────────────────────────────────────────────

    /**
     * 简单换行：逐字测量，超出最大宽度时换行。
     */
    private fun breakLines(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (text.isEmpty()) return listOf("")

        val lines = mutableListOf<String>()
        var current = StringBuilder()
        for (ch in text) {
            val candidate = current.toString() + ch
            if (paint.measureText(candidate) > maxWidth && current.isNotEmpty()) {
                lines.add(current.toString())
                current = StringBuilder().append(ch)
            } else {
                current.append(ch)
            }
        }
        if (current.isNotEmpty()) lines.add(current.toString())
        return lines
    }

    /**
     * 给颜色加 alpha 通道 (0.0-1.0)。用于 RadialGradient 中心色透明叠加。
     */
    private fun withAlpha(color: Int, alpha: Float): Int {
        val a = (alpha.coerceIn(0f, 1f) * 255).toInt()
        return Color.argb(a, Color.red(color), Color.green(color), Color.blue(color))
    }
}
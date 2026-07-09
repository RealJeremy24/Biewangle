package com.biewangle.dontforget.util

import android.content.Context
import android.graphics.*
import com.biewangle.dontforget.R

/**
 * 运行时生成通知大图的工具类。
 * 合成图规格：绿色渐变背景 + 左侧圆形妈妈贴纸 + 右侧动态提醒文字。
 * 输出 2:1 宽高比 Bitmap，用于 NotificationCompat.BigPictureStyle。
 */
object NotificationBitmapBuilder {

    // Canvas 逻辑尺寸（px），2:1 宽高比
    private const val W = 900f
    private const val H = 450f

    // 布局参数
    private const val PADDING = 40f
    private const val STICKER_DIAMETER = 220f
    private const val STICKER_CX = PADDING + STICKER_DIAMETER / 2f
    private const val STICKER_CY = H / 2f

    private const val TEXT_LEFT = PADDING + STICKER_DIAMETER + PADDING  // 300
    private const val TEXT_MAX_WIDTH = W - TEXT_LEFT - PADDING           // 560

    private const val TITLE_SIZE = 48f
    private const val CONTENT_SIZE = 32f

    // 绿色渐变：与妈妈贴纸背景一致
    private val COLOR_LIGHT_GREEN = Color.rgb(0xB5, 0xE8, 0xB5)
    private val COLOR_DEEP_GREEN = Color.rgb(0x4C, 0xAF, 0x50)

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
        drawSticker(context, canvas)
        drawTextBlock(canvas, title, content)

        return bitmap
    }

    // ── 绿色渐变背景（圆角矩形） ────────────────────────────────

    private fun drawBackground(canvas: Canvas) {
        val radius = 24f
        val rect = RectF(0f, 0f, W, H)

        // 圆角裁剪
        canvas.save()
        val clipPath = Path().apply {
            addRoundRect(rect, radius, radius, Path.Direction.CW)
        }
        canvas.clipPath(clipPath)

        // 绿色渐变
        val gradient = LinearGradient(
            0f, 0f, W, H,
            COLOR_LIGHT_GREEN, COLOR_DEEP_GREEN,
            Shader.TileMode.CLAMP
        )
        val paint = Paint().apply { shader = gradient }
        canvas.drawRect(rect, paint)
        canvas.restore()
    }

    // ── 左侧圆形贴纸（白色描边 + 阴影） ──────────────────────────

    private fun drawSticker(context: Context, canvas: Canvas) {
        val radius = STICKER_DIAMETER / 2f
        val cx = STICKER_CX
        val cy = STICKER_CY

        // 加载并缩放到圆形区域
        val original = BitmapFactory.decodeResource(context.resources, R.drawable.mama3)
        val scaled = Bitmap.createScaledBitmap(
            original,
            STICKER_DIAMETER.toInt(),
            STICKER_DIAMETER.toInt(),
            true
        )
        if (scaled != original) original.recycle()

        // 圆形裁剪绘制
        canvas.save()
        val circlePath = Path().apply {
            addCircle(cx, cy, radius, Path.Direction.CW)
        }
        canvas.clipPath(circlePath)

        val left = cx - radius
        val top = cy - radius
        canvas.drawBitmap(scaled, left, top, null)
        canvas.restore()

        // 阴影（偏移 3px）
        val shadowPaint = Paint().apply {
            style = Paint.Style.STROKE
            color = Color.argb(50, 0, 0, 0)
            strokeWidth = 5f
            isAntiAlias = true
        }
        canvas.drawCircle(cx + 3f, cy + 3f, radius, shadowPaint)

        // 白色描边
        val strokePaint = Paint().apply {
            style = Paint.Style.STROKE
            color = Color.WHITE
            strokeWidth = 6f
            isAntiAlias = true
        }
        canvas.drawCircle(cx, cy, radius, strokePaint)
    }

    // ── 右侧文字区（标题 + 正文） ──────────────────────────────

    private fun drawTextBlock(canvas: Canvas, title: String, content: String) {
        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = TITLE_SIZE
            isAntiAlias = true
            isFakeBoldText = true
            setShadowLayer(3f, 1f, 1f, Color.argb(80, 0, 0, 0))
        }

        val contentPaint = Paint().apply {
            color = Color.WHITE
            textSize = CONTENT_SIZE
            isAntiAlias = true
            setShadowLayer(2f, 1f, 1f, Color.argb(80, 0, 0, 0))
        }

        // ── 排版：标题和正文作为一个整体垂直居中 ──
        val titleLines = breakLines(title, titlePaint, TEXT_MAX_WIDTH)
        val contentLines = breakLines(content, contentPaint, TEXT_MAX_WIDTH)

        val titleLineHeight = titlePaint.descent() - titlePaint.ascent()
        val contentLineHeight = contentPaint.descent() - contentPaint.ascent()
        val titleGap = 16f  // 标题与正文之间的间距

        val titleBlockHeight = titleLines.size * titleLineHeight
        val contentBlockHeight = contentLines.size * contentLineHeight
        val totalHeight = titleBlockHeight + titleGap + contentBlockHeight

        val startY = (H - totalHeight) / 2f + titleLineHeight  // 标题 baseline

        // 绘制标题
        val titleX = TEXT_LEFT
        var y = startY
        for (line in titleLines) {
            canvas.drawText(line, titleX, y, titlePaint)
            y += titleLineHeight
        }

        // 间隔
        y += titleGap - titleLineHeight  // 上一行已加了最后一个 lineHeight，补回间隙差

        // 绘制正文
        y += contentLineHeight  // 正文第一行 baseline
        for (line in contentLines) {
            canvas.drawText(line, titleX, y, contentPaint)
            y += contentLineHeight
        }
    }

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
}

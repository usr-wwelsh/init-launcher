package com.initlauncher.lockscreen

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.widget.TextView
import kotlin.math.floor
import kotlin.math.max

class PixelClockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : TextView(context, attrs) {

    private val paint = Paint()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val cols = PixelFont.rows(text.toString())[0].length
        val h = MeasureSpec.getSize(heightMeasureSpec)
        val maxW = if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED) {
            Int.MAX_VALUE / 2
        } else {
            MeasureSpec.getSize(widthMeasureSpec)
        }
        val cell = PixelFont.cellSize(cols, availW(maxW), availH(h))
        val shadow = max(1f, floor(cell * PixelFont.SHADOW))
        val w = (cols * cell + shadow).toInt() + paddingLeft + paddingRight
        setMeasuredDimension(resolveSize(w, widthMeasureSpec), h)
    }

    private fun availW(w: Int) = (w - paddingLeft - paddingRight).toFloat()

    private fun availH(h: Int) = (h - paddingTop - paddingBottom).toFloat()

    override fun onDraw(canvas: Canvas) {
        val rows = PixelFont.rows(text.toString())
        val cols = rows[0].length
        if (cols == 0) return

        val cell = PixelFont.cellSize(cols, availW(width), availH(height))
        val shadow = max(1f, floor(cell * PixelFont.SHADOW))

        paint.color = shadowColor(currentTextColor)
        draw(canvas, rows, cell, shadow)
        paint.color = currentTextColor
        draw(canvas, rows, cell, 0f)
    }

    private fun draw(canvas: Canvas, rows: List<String>, cell: Float, offset: Float) {
        val ox = paddingLeft + offset
        val oy = paddingTop + offset
        rows.forEachIndexed { y, row ->
            row.forEachIndexed { x, c ->
                if (c == '#') {
                    canvas.drawRect(
                        ox + x * cell, oy + y * cell,
                        ox + (x + 1) * cell, oy + (y + 1) * cell,
                        paint
                    )
                }
            }
        }
    }

    private fun shadowColor(color: Int): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[2] *= 0.35f
        return Color.HSVToColor(hsv)
    }
}

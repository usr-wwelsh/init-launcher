package com.initlauncher.lockscreen

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/**
 * 7-segment block clock with CDE-style 3D bevel on each lit segment.
 *
 * Segment layout (W wide, H tall, thickness T, gap G):
 *
 *   [──A──]
 *  F       B
 *   [──G──]
 *  E       C
 *   [──D──]
 *
 * Verticals run from the END of their adjacent horizontal to the START
 * of the next, with a small gap G so segments never overlap.
 */
class BlockClockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var text: String = "12:00"
        set(value) { field = value; invalidate() }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // CDE palette — raised block style
    private val colorFill   = 0xFFFFFFFF.toInt()  // lit segment fill
    private val colorHi     = 0xFFB8CEDE.toInt()  // top-left bevel highlight
    private val colorShadow = 0xFF1A2A38.toInt()  // bottom-right bevel shadow
    private val colorOff    = 0xFF3A4A58.toInt()  // unlit segment

    //  Bit 0 = A  (top horizontal)
    //  Bit 1 = B  (top-right vertical)
    //  Bit 2 = C  (bottom-right vertical)
    //  Bit 3 = D  (bottom horizontal)
    //  Bit 4 = E  (bottom-left vertical)
    //  Bit 5 = F  (top-left vertical)
    //  Bit 6 = G  (middle horizontal)
    private val segMask = mapOf(
        '0' to 0b0111111,
        '1' to 0b0000110,
        '2' to 0b1011011,
        '3' to 0b1001111,
        '4' to 0b1100110,
        '5' to 0b1101101,
        '6' to 0b1111101,
        '7' to 0b0000111,
        '8' to 0b1111111,
        '9' to 0b1101111,
    )

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val h = MeasureSpec.getSize(heightMeasureSpec).toFloat().coerceAtLeast(64f)
        setMeasuredDimension(totalWidth(h).toInt(), MeasureSpec.getSize(heightMeasureSpec))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val h = height.toFloat()
        val dw = digitW(h)
        val cw = colonW(h)
        val sp = spacing(h)
        var x = 0f
        for (ch in text) {
            when {
                ch.isDigit() -> { drawDigit(canvas, ch, x, h, dw); x += dw + sp }
                ch == ':'    -> { drawColon(canvas, x, h, cw);     x += cw + sp }
            }
        }
    }

    private fun drawDigit(canvas: Canvas, ch: Char, ox: Float, H: Float, W: Float) {
        val mask = segMask[ch] ?: 0
        val T = H * 0.13f   // segment thickness
        val G = T * 0.12f   // gap between segment ends (prevents overlap)
        val bevel = (T * 0.22f).coerceAtLeast(2f)

        // Horizontal segments: inset by T+G at each end (where verticals sit)
        val hx0 = ox + T + G
        val hx1 = ox + W - T - G

        // Vertical segments: run between adjacent horizontal segments, with gap G
        val vTopStart    = T + G          // below top horizontal
        val vTopEnd      = H / 2 - T / 2 - G  // above middle horizontal
        val vBotStart    = H / 2 + T / 2 + G  // below middle horizontal
        val vBotEnd      = H - T - G     // above bottom horizontal

        val segs = arrayOf(
            RectF(hx0,      G,           hx1,       T - G        ),  // A top
            RectF(ox+W-T+G, vTopStart,   ox+W-G,    vTopEnd      ),  // B top-right
            RectF(ox+W-T+G, vBotStart,   ox+W-G,    vBotEnd      ),  // C bottom-right
            RectF(hx0,      H-T+G,       hx1,       H-G          ),  // D bottom
            RectF(ox+G,     vBotStart,   ox+T-G,    vBotEnd      ),  // E bottom-left
            RectF(ox+G,     vTopStart,   ox+T-G,    vTopEnd      ),  // F top-left
            RectF(hx0,      H/2-T/2+G,  hx1,       H/2+T/2-G   ),  // G middle
        )

        for (i in 0..6) drawSegment(canvas, segs[i], on = (mask shr i) and 1 == 1, bevel)
    }

    private fun drawColon(canvas: Canvas, ox: Float, H: Float, W: Float) {
        val s = W * 0.65f
        val cx = ox + (W - s) / 2f
        val bevel = (H * 0.028f).coerceAtLeast(2f)
        drawSegment(canvas, RectF(cx, H * 0.20f, cx + s, H * 0.20f + s), on = true, bevel)
        drawSegment(canvas, RectF(cx, H * 0.62f, cx + s, H * 0.62f + s), on = true, bevel)
    }

    private fun drawSegment(canvas: Canvas, r: RectF, on: Boolean, bevel: Float) {
        if (r.width() <= 0 || r.height() <= 0) return
        if (on) {
            paint.color = colorShadow
            canvas.drawRect(r, paint)
            paint.color = colorHi
            canvas.drawRect(r.left, r.top, r.right - bevel, r.bottom - bevel, paint)
            paint.color = colorFill
            canvas.drawRect(r.left + bevel, r.top + bevel, r.right - bevel, r.bottom - bevel, paint)
        } else {
            paint.color = colorOff
            canvas.drawRect(r, paint)
        }
    }

    private fun digitW(h: Float)  = h * 0.55f
    private fun colonW(h: Float)  = h * 0.20f
    private fun spacing(h: Float) = h * 0.08f

    private fun totalWidth(h: Float): Float {
        val digits = text.count { it.isDigit() }
        val colons = text.count { it == ':' }
        return digits * digitW(h) + colons * colonW(h) +
               (digits + colons - 1).coerceAtLeast(0) * spacing(h)
    }
}

package com.initlauncher

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class NetworkGraphView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val uploadPaint = Paint().apply {
        color = 0xFF00FF00.toInt() // Green for upload
        strokeWidth = 2f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val downloadPaint = Paint().apply {
        color = 0xFFFF0088.toInt() // Pink/Red for download
        strokeWidth = 2f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val gridPaint = Paint().apply {
        color = 0xFF333333.toInt()
        strokeWidth = 1f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val uploadHistory = mutableListOf<Float>()
    private val downloadHistory = mutableListOf<Float>()
    private val maxDataPoints = 60 // Keep last 60 data points
    private var maxSpeed = 100f // KB/s, will auto-adjust

    fun addDataPoint(uploadKBps: Float, downloadKBps: Float) {
        uploadHistory.add(uploadKBps)
        downloadHistory.add(downloadKBps)

        // Keep only last N points
        if (uploadHistory.size > maxDataPoints) {
            uploadHistory.removeAt(0)
        }
        if (downloadHistory.size > maxDataPoints) {
            downloadHistory.removeAt(0)
        }

        // Auto-adjust scale
        val currentMax = maxOf(
            uploadHistory.maxOrNull() ?: 0f,
            downloadHistory.maxOrNull() ?: 0f
        )
        if (currentMax > maxSpeed) {
            maxSpeed = (currentMax * 1.2f).coerceAtLeast(100f)
        }

        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()

        if (w == 0f || h == 0f) return

        // Draw grid lines
        val gridLines = 4
        for (i in 0..gridLines) {
            val y = h * i / gridLines
            canvas.drawLine(0f, y, w, y, gridPaint)
        }

        // Draw upload line
        if (uploadHistory.size > 1) {
            for (i in 0 until uploadHistory.size - 1) {
                val x1 = w * i / maxDataPoints
                val x2 = w * (i + 1) / maxDataPoints
                val y1 = h - (h * uploadHistory[i] / maxSpeed).coerceIn(0f, h)
                val y2 = h - (h * uploadHistory[i + 1] / maxSpeed).coerceIn(0f, h)
                canvas.drawLine(x1, y1, x2, y2, uploadPaint)
            }
        }

        // Draw download line
        if (downloadHistory.size > 1) {
            for (i in 0 until downloadHistory.size - 1) {
                val x1 = w * i / maxDataPoints
                val x2 = w * (i + 1) / maxDataPoints
                val y1 = h - (h * downloadHistory[i] / maxSpeed).coerceIn(0f, h)
                val y2 = h - (h * downloadHistory[i + 1] / maxSpeed).coerceIn(0f, h)
                canvas.drawLine(x1, y1, x2, y2, downloadPaint)
            }
        }
    }
}

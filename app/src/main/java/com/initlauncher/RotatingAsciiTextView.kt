package com.initlauncher

import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.AttributeSet
import android.widget.TextView
import kotlin.random.Random

class RotatingAsciiTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : TextView(context, attrs, defStyleAttr) {

    private val handler = Handler(Looper.getMainLooper())
    private val frameDuration = 150L // milliseconds per color update

    private val colorGreen = Color.parseColor("#00FF00")
    private val colorWhite = Color.parseColor("#FFFFFF")
    private val colorRed = Color.parseColor("#FF0000")

    // Static ASCII art with animated colors
    private val asciiArt = """⠀⠀⠀⠀⢀⣀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⣀⡀⠀⠀⠀⠀
⠀⠀⠀⠀⠀⠙⢷⣤⣤⣴⣶⣶⣦⣤⣤⡾⠋⠀⠀⠀⠀⠀
⠀⠀⠀⠀⠀⣴⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣦⠀⠀⠀⠀⠀
⠀⠀⠀⠀⣼⣿⣿⣉⣹⣿⣿⣿⣿⣏⣉⣿⣿⣧⠀⠀⠀⠀
⠀⠀⠀⢸⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⡇⠀⠀⠀
⣠⣄⠀⢠⣤⣤⣤⣤⣤⣤⣤⣤⣤⣤⣤⣤⣤⣤⡄⠀⣠⣄
⣿⣿⡇⢸⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⡇⢸⣿⣿
⣿⣿⡇⢸⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⡇⢸⣿⣿
⣿⣿⡇⢸⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⡇⢸⣿⣿
⣿⣿⡇⢸⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⡇⢸⣿⣿
⠻⠟⠁⢸⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⡇⠈⠻⠟
⠀⠀⠀⠀⠉⠉⣿⣿⣿⡏⠉⠉⢹⣿⣿⣿⠉⠉⠀⠀⠀⠀
⠀⠀⠀⠀⠀⠀⣿⣿⣿⡇⠀⠀⢸⣿⣿⣿⠀⠀⠀⠀⠀⠀
⠀⠀⠀⠀⠀⠀⣿⣿⣿⡇⠀⠀⢸⣿⣿⣿⠀⠀⠀⠀⠀⠀
⠀⠀⠀⠀⠀⠀⠈⠉⠉⠀⠀⠀⠀⠉⠉⠁⠀⠀⠀⠀⠀⠀"""

    private val animationRunnable = object : Runnable {
        override fun run() {
            val spannable = SpannableString(asciiArt)

            // Add color variation - randomly change some characters to white/red
            for (i in asciiArt.indices) {
                val char = asciiArt[i]
                // Only colorize non-whitespace Braille characters
                if (char != ' ' && char != '\n') {
                    val randomValue = Random.nextFloat()
                    val color = when {
                        randomValue < 0.15 -> colorWhite  // 15% white
                        randomValue < 0.25 -> colorRed    // 10% red
                        else -> colorGreen                 // 75% green
                    }
                    spannable.setSpan(
                        ForegroundColorSpan(color),
                        i,
                        i + 1,
                        SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }

            text = spannable
            handler.postDelayed(this, frameDuration)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startAnimation()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimation()
    }

    fun startAnimation() {
        stopAnimation()
        handler.post(animationRunnable)
    }

    fun stopAnimation() {
        handler.removeCallbacks(animationRunnable)
    }
}

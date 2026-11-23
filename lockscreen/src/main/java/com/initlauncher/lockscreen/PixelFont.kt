package com.initlauncher.lockscreen

import kotlin.math.floor

object PixelFont {

    const val ROWS = 5
    const val SHADOW = 0.25f

    private val GLYPHS: Map<Char, List<String>> = mapOf(
        '0' to listOf("###", "#.#", "#.#", "#.#", "###"),
        '1' to listOf(".#.", "##.", ".#.", ".#.", "###"),
        '2' to listOf("###", "..#", "###", "#..", "###"),
        '3' to listOf("###", "..#", "###", "..#", "###"),
        '4' to listOf("#.#", "#.#", "###", "..#", "..#"),
        '5' to listOf("###", "#..", "###", "..#", "###"),
        '6' to listOf("###", "#..", "###", "#.#", "###"),
        '7' to listOf("###", "..#", "..#", "..#", "..#"),
        '8' to listOf("###", "#.#", "###", "#.#", "###"),
        '9' to listOf("###", "#.#", "###", "..#", "###"),
        ':' to listOf(".", "#", ".", "#", ".")
    )

    private val BLANK = List(ROWS) { "..." }

    fun rows(text: String): List<String> {
        val out = List(ROWS) { StringBuilder() }
        text.forEachIndexed { index, ch ->
            val glyph = GLYPHS[ch] ?: BLANK
            for (i in 0 until ROWS) {
                if (index > 0) out[i].append('.')
                out[i].append(glyph[i])
            }
        }
        return out.map { it.toString() }
    }

    fun cellSize(cols: Int, availW: Float, availH: Float): Float =
        maxOf(1f, floor(minOf(availW / (cols + SHADOW), availH / (ROWS + SHADOW))))
}

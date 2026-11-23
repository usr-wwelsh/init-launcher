package com.initlauncher.lockscreen

import org.junit.Assert.assertEquals
import org.junit.Test

class PixelFontTest {

    @Test
    fun rendersFiveRowsOfEqualWidth() {
        val rows = PixelFont.rows("12:34")
        assertEquals(5, rows.size)
        assertEquals(1, rows.map { it.length }.toSet().size)
    }

    @Test
    fun everyDigitLooksDifferent() {
        val glyphs = ('0'..'9').map { PixelFont.rows(it.toString()) }
        assertEquals(10, glyphs.toSet().size)
    }

    @Test
    fun digitsAreThreeColumnsWide() {
        assertEquals(3, PixelFont.rows("8")[0].length)
    }

    @Test
    fun glyphsAreSeparatedByOneBlankColumn() {
        assertEquals(7, PixelFont.rows("11")[0].length)
    }

    @Test
    fun colonIsOneColumnWide() {
        assertEquals(1, PixelFont.rows(":")[0].length)
    }

    @Test
    fun colonHasTwoDots() {
        assertEquals(2, PixelFont.rows(":").count { it == "#" })
    }

    @Test
    fun eightIsAClosedOutlineWithMiddleBar() {
        assertEquals(listOf("###", "#.#", "###", "#.#", "###"), PixelFont.rows("8"))
    }

    @Test
    fun unknownCharactersRenderBlank() {
        assertEquals(List(5) { "..." }, PixelFont.rows("x"))
    }

    @Test
    fun cellIsLimitedByHeightWhenWidthIsAmple() {
        assertEquals(18f, PixelFont.cellSize(17, 1000f, 96f), 0f)
    }

    @Test
    fun cellIsLimitedByWidthWhenHeightIsAmple() {
        assertEquals(10f, PixelFont.cellSize(17, 175f, 1000f), 0f)
    }

    @Test
    fun cellIsAtLeastOnePixel() {
        assertEquals(1f, PixelFont.cellSize(17, 3f, 3f), 0f)
    }
}

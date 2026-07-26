package com.guberdev.codexusage

import org.junit.Assert.assertEquals
import org.junit.Test

class TileDisplayTest {
    @Test
    fun `tile renders the remaining percentage`() {
        assertEquals("42%", TileDisplay.percent(42))
    }

    @Test
    fun `system icon separates the number from the percent sign`() {
        assertEquals("53", TileDisplay.iconNumber(53))
    }

    @Test
    fun `system icon number fills the available height`() {
        assertEquals(0.72f, TileDisplay.textScale(53))
        assertEquals(0.48f, TileDisplay.textScale(100))
    }

    @Test
    fun `system icon uses a compact superscript percent sign`() {
        assertEquals(0.24f, TileDisplay.percentScale(53))
        assertEquals(0.18f, TileDisplay.percentScale(100))
    }
}

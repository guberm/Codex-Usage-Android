package com.guberdev.codexusage

import org.junit.Assert.assertEquals
import org.junit.Test

class TileDisplayTest {
    @Test
    fun `tile renders the remaining percentage`() {
        assertEquals("42%", TileDisplay.percent(42))
    }

    @Test
    fun `system icon keeps the percent sign with the number`() {
        assertEquals("53%", TileDisplay.iconText(53))
    }

    @Test
    fun `system icon uses clock typography and fills the available height`() {
        assertEquals("sans-serif-medium", TileDisplay.iconFontFamily())
        assertEquals(0.94f, TileDisplay.iconFillFraction())
    }
}

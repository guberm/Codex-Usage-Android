package com.guberdev.codexusage

import org.junit.Assert.assertEquals
import org.junit.Test

class TileDisplayTest {
    @Test
    fun `tile renders the remaining percentage`() {
        assertEquals("42%", TileDisplay.percent(42))
    }

    @Test
    fun `system percentage icon uses the larger text scale`() {
        assertEquals(0.50f, TileDisplay.textScale(42))
    }

    @Test
    fun `three digit percentage remains large enough`() {
        assertEquals(0.39f, TileDisplay.textScale(100))
    }
}

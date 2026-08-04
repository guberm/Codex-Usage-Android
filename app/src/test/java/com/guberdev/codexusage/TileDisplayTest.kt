package com.guberdev.codexusage

import org.junit.Assert.assertEquals
import org.junit.Test

class TileDisplayTest {
    @Test
    fun `tile renders the remaining percentage`() {
        assertEquals("42%", TileDisplay.percent(42))
        assertEquals("42% Codex", TileDisplay.label(42))
    }

    @Test
    fun `tile percentage uses slightly larger type`() {
        assertEquals(1.04f, TileDisplay.ICON_TEXT_SCALE)
        assertEquals(0.99f, TileDisplay.ICON_MAX_WIDTH_RATIO)
    }
}

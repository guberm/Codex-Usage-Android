package com.guberdev.codexusage

import org.junit.Assert.assertEquals
import org.junit.Test

class TileDisplayTest {
    @Test
    fun `tile renders the remaining percentage`() {
        assertEquals("42%", TileDisplay.percent(42))
        assertEquals("42% Codex", TileDisplay.label(42))
    }
}

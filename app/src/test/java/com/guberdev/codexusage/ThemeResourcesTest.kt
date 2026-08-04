package com.guberdev.codexusage

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeResourcesTest {
    @Test
    fun `app provides native night theme resources`() {
        val nightStyle = File("src/main/res/values-night/styles.xml").readText()
        assertTrue(nightStyle.contains("Theme.Material.NoActionBar"))
        assertTrue(nightStyle.contains("name=\"AppTheme\""))
    }
}

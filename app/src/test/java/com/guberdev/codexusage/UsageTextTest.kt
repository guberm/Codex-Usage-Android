package com.guberdev.codexusage

import java.util.Locale
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageTextTest {
    @Test
    fun `dates stay English when the device locale is Russian`() {
        val originalLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("ru"))

            assertTrue(UsageText.resetDate(1_767_359_040).contains("Jan"))
        } finally {
            Locale.setDefault(originalLocale)
        }
    }
}

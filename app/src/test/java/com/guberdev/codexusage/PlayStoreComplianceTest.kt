package com.guberdev.codexusage

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertFalse
import org.junit.Test

class PlayStoreComplianceTest {
    @Test
    fun backgroundMonitoringDoesNotRequireSpecialUseForegroundService() {
        val manifest = DocumentBuilderFactory.newInstance()
            .apply { isNamespaceAware = true }
            .newDocumentBuilder()
            .parse(File("src/main/AndroidManifest.xml"))
        val androidName = "http://schemas.android.com/apk/res/android"
        val permissions = manifest.getElementsByTagName("uses-permission")
        val services = manifest.getElementsByTagName("service")

        assertFalse(
            (0 until permissions.length)
                .map { permissions.item(it).attributes.getNamedItemNS(androidName, "name").nodeValue }
                .any { it == "android.permission.FOREGROUND_SERVICE_SPECIAL_USE" },
        )
        assertFalse(
            (0 until services.length)
                .map { services.item(it).attributes.getNamedItemNS(androidName, "name").nodeValue }
                .any { it == ".UsageMonitorService" },
        )
    }
}

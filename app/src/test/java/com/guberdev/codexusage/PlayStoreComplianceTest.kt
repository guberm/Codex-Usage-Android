package com.guberdev.codexusage

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayStoreComplianceTest {
    @Test
    fun persistentNotificationDoesNotRequireSpecialUseForegroundService() {
        val manifest = DocumentBuilderFactory.newInstance()
            .apply { isNamespaceAware = true }
            .newDocumentBuilder()
            .parse(File("src/main/AndroidManifest.xml"))
        val androidName = "http://schemas.android.com/apk/res/android"
        val permissions = manifest.getElementsByTagName("uses-permission")
        val services = manifest.getElementsByTagName("service")
        val application = manifest.getElementsByTagName("application").item(0)
        val actions = manifest.getElementsByTagName("action")

        assertFalse(
            (0 until permissions.length)
                .map { permissions.item(it).attributes.getNamedItemNS(androidName, "name").nodeValue }
                .any { it == "android.permission.FOREGROUND_SERVICE_SPECIAL_USE" },
        )
        assertTrue(
            (0 until permissions.length)
                .map { permissions.item(it).attributes.getNamedItemNS(androidName, "name").nodeValue }
                .any { it == "android.permission.POST_PROMOTED_NOTIFICATIONS" },
        )
        assertFalse(
            (0 until services.length)
                .map { services.item(it).attributes.getNamedItemNS(androidName, "name").nodeValue }
                .any { it == ".UsageMonitorService" },
        )
        assertTrue(
            application.attributes.getNamedItemNS(androidName, "name").nodeValue == ".CodexUsageApp",
        )
        assertTrue(
            (0 until actions.length)
                .map { actions.item(it).attributes.getNamedItemNS(androidName, "name").nodeValue }
                .any { it == "android.intent.action.USER_PRESENT" },
        )
    }
}

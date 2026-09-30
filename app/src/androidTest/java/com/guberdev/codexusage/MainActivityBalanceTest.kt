package com.guberdev.codexusage

import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityBalanceTest {
    @Test
    fun testPrimaryAndWeeklyBalancesAreClearlyLabeled() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val store = UsageStore(context)
        store.save(
            UsageSnapshot(
                planType = "plus",
                primary = UsageWindow(75, null, 18_000),
                secondary = UsageWindow(40, null, 604_800),
                additionalLimits = emptyList(),
                creditBalance = null,
            ),
        )
        val activity = instrumentation.startActivitySync(
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        try {
            val labels = mutableListOf<String>()
            fun collect(view: View) {
                if (view is TextView) labels += view.text.toString()
                if (view is ViewGroup) for (index in 0 until view.childCount) collect(view.getChildAt(index))
            }
            instrumentation.runOnMainSync { collect(activity.window.decorView) }
            assertTrue("Primary window is not named by its time period: $labels", labels.contains("5-hour balance"))
            assertTrue("Weekly balance is not clearly labeled: $labels", labels.contains("Weekly balance"))
            assertTrue("Weekly remaining percentage is missing: $labels", labels.contains("40% remaining"))
        } finally {
            instrumentation.runOnMainSync { activity.finish() }
            store.clear()
        }
    }
}

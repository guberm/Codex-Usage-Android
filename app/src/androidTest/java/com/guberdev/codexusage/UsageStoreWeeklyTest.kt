package com.guberdev.codexusage

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UsageStoreWeeklyTest {
    @Test
    fun testWeeklyBalanceSurvivesCacheReloadAndIsRemovedWhenAbsent() {
        val context = object : ContextWrapper(InstrumentationRegistry.getInstrumentation().targetContext) {
            override fun getApplicationContext(): Context = this
            override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
                super.getSharedPreferences("test_weekly_$name", mode)
        }
        val store = UsageStore(context)
        store.clear()
        try {
            val snapshot = UsageParser().parse(
                """{"plan_type":"plus","rate_limit":{
                    "primary_window":{"used_percent":25,"limit_window_seconds":18000,"reset_at":1785611900},
                    "secondary_window":{"used_percent":60,"limit_window_seconds":604800,"reset_at":1786216700}
                }}""",
            )
            store.save(snapshot)
            val restored = requireNotNull(UsageStore(context).load())
            assertEquals(snapshot, restored)
            assertEquals(18000L, restored.primary.windowSeconds)
            assertTrue(MonitorDisplay.content(restored).contains("Weekly 40% left"))
            assertTrue(WidgetDisplay.reset(restored).contains("Weekly 40% left"))

            store.save(UsageParser().parse("""{"rate_limit":{"primary_window":{"used_percent":25}}}"""))
            assertFalse(MonitorDisplay.content(store.load()).contains("Weekly"))
            assertEquals("Reset: —", WidgetDisplay.reset(store.load()))
        } finally {
            store.clear()
        }
    }
}

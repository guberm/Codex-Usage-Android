package com.guberdev.codexusage

import android.widget.RemoteViews
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WidgetRemoteViewsInflationTest {
    @Test
    fun testWidgetRemoteViewsInflateOnBackgroundThread() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val remoteViews = RemoteViews(context.packageName, R.layout.codex_usage_widget)
        val inflatedView = AtomicReference<android.view.View?>()
        val failure = AtomicReference<Throwable?>()
        val inflater = Thread {
            try {
                inflatedView.set(remoteViews.apply(context, null))
            } catch (error: Throwable) {
                failure.set(error)
            }
        }

        inflater.start()
        inflater.join(10_000)

        assertFalse("RemoteViews inflation timed out", inflater.isAlive)
        assertNull("RemoteViews failed to inflate: ${failure.get()}", failure.get())
        assertNotNull(inflatedView.get())
        assertNotNull(inflatedView.get()?.findViewById<android.view.View>(R.id.widget_secondary_balance))
    }
}

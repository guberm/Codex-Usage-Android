package com.guberdev.codexusage

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

object WidgetDisplay {
    fun percent(snapshot: UsageSnapshot?): String =
        snapshot?.let { "${it.primary.remainingPercent}%" } ?: "—"

    fun reset(snapshot: UsageSnapshot?): String =
        snapshot?.let { "Reset: ${UsageText.shortResetDate(it.primary.resetAtEpochSeconds)}" }
            ?: "Tap to sign in"

    fun progress(snapshot: UsageSnapshot?): Int =
        snapshot?.primary?.remainingPercent?.coerceIn(0, 100) ?: 0
}

class CodexUsageWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        render(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != ACTION_REFRESH) return
        val pendingResult = goAsync()
        RefreshCoordinator.refresh(context) {
            pendingResult.finish()
        }
    }

    companion object {
        private const val ACTION_REFRESH = "com.guberdev.codexusage.action.REFRESH_WIDGET"

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, CodexUsageWidgetProvider::class.java)
            render(context, manager, manager.getAppWidgetIds(component))
        }

        private fun render(
            context: Context,
            manager: AppWidgetManager,
            widgetIds: IntArray,
        ) {
            if (widgetIds.isEmpty()) return
            val snapshot = UsageStore(context).load()
            widgetIds.forEach { widgetId ->
                val views = RemoteViews(context.packageName, R.layout.codex_usage_widget).apply {
                    setTextViewText(R.id.widget_percent, WidgetDisplay.percent(snapshot))
                    setProgressBar(R.id.widget_progress, 100, WidgetDisplay.progress(snapshot), false)
                    setTextViewText(R.id.widget_reset, WidgetDisplay.reset(snapshot))
                    setOnClickPendingIntent(
                        R.id.widget_root,
                        PendingIntent.getActivity(
                            context,
                            widgetId,
                            Intent(context, MainActivity::class.java),
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                        ),
                    )
                    setOnClickPendingIntent(
                        R.id.widget_refresh,
                        PendingIntent.getBroadcast(
                            context,
                            widgetId,
                            Intent(context, CodexUsageWidgetProvider::class.java)
                                .setAction(ACTION_REFRESH),
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                        ),
                    )
                }
                manager.updateAppWidget(widgetId, views)
            }
        }
    }
}

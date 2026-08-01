package com.guberdev.codexusage

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Build

object MonitorDisplay {
    fun shortCriticalText(snapshot: UsageSnapshot?): String =
        snapshot?.let { TileDisplay.percent(it.primary.remainingPercent) } ?: "Codex"
}

object NotificationHelper {
    const val MONITOR_NOTIFICATION_ID = 4101
    private const val CHANGE_NOTIFICATION_BASE_ID = 4200
    private const val MONITOR_CHANNEL = "codex_monitor"
    private const val CHANGE_CHANNEL = "codex_changes"

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                MONITOR_CHANNEL,
                "Codex Usage monitor",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Persistent Codex Usage background monitoring status"
                setShowBadge(false)
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANGE_CHANNEL,
                "Codex Usage changes",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Changes to the remaining Codex usage limit"
            },
        )
    }

    fun monitorNotification(context: Context, snapshot: UsageSnapshot?, status: String? = null): Notification {
        createChannels(context)
        val title = snapshot?.let { "Codex Usage: ${it.primary.remainingPercent}% remaining" }
            ?: "Codex Usage monitor"
        val reset = snapshot?.primary?.resetAtEpochSeconds
        val text = status ?: snapshot?.let { "Reset: ${UsageText.resetDate(reset)}" }
            ?: "Waiting for the first check"
        val builder = Notification.Builder(context, MONITOR_CHANNEL)
            .setSmallIcon(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                    Icon.createWithResource(context, R.drawable.ic_stat_usage)
                } else {
                    snapshot?.let {
                        UsagePercentIcon.create(context, it.primary.remainingPercent)
                    } ?: Icon.createWithResource(context, R.drawable.ic_stat_usage)
                },
            )
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(mainPendingIntent(context))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_SERVICE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            builder
                .setShortCriticalText(MonitorDisplay.shortCriticalText(snapshot))
                .setColorized(true)
        }
        return builder.build()
    }

    fun notifyChange(context: Context, snapshot: UsageSnapshot, delta: Int) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        createChannels(context)
        val sign = if (delta > 0) "+" else "−"
        val notification = Notification.Builder(context, CHANGE_CHANNEL)
            .setSmallIcon(UsagePercentIcon.create(context, snapshot.primary.remainingPercent))
            .setContentTitle("Codex Usage: $sign${kotlin.math.abs(delta)}%")
            .setContentText(
                "${snapshot.primary.remainingPercent}% remaining • " +
                    "resets ${UsageText.resetDate(snapshot.primary.resetAtEpochSeconds)}",
            )
            .setContentIntent(mainPendingIntent(context))
            .setAutoCancel(true)
            .setCategory(Notification.CATEGORY_STATUS)
            .build()
        context.getSystemService(NotificationManager::class.java)
            .notify(CHANGE_NOTIFICATION_BASE_ID + (System.currentTimeMillis() % 100).toInt(), notification)
    }

    fun updateMonitor(context: Context, snapshot: UsageSnapshot?, status: String? = null) {
        context.getSystemService(NotificationManager::class.java)
            .notify(MONITOR_NOTIFICATION_ID, monitorNotification(context, snapshot, status))
    }

    private fun mainPendingIntent(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}

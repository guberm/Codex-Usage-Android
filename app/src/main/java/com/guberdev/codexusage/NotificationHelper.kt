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
    fun title(snapshot: UsageSnapshot?): String =
        snapshot?.let {
            if (it.secondary != null) {
                "${UsageText.windowName(it.primary.windowSeconds)} ${it.primary.remainingPercent}% · " +
                    "${UsageText.windowName(it.secondary.windowSeconds)} ${it.secondary.remainingPercent}%"
            } else {
                "Codex ${it.primary.remainingPercent}% left · " +
                    "${it.availableResetCount} ${if (it.availableResetCount == 1) "reset" else "resets"}"
            }
        } ?: "Codex Usage monitor"

    fun content(snapshot: UsageSnapshot?): String = snapshot?.let {
        val spark = it.additionalLimits.firstOrNull { limit -> limit.feature == "codex_bengalfox" }
        spark?.let { limit -> "Spark: ${UsageText.limitSummary(limit)}" } ?: "Spark unavailable"
    } ?: "Waiting for the first check"

    fun shortCriticalText(snapshot: UsageSnapshot?): String =
        snapshot?.let {
            it.secondary?.let { secondary -> "${it.primary.remainingPercent}/${secondary.remainingPercent}" }
                ?: "${it.primary.remainingPercent}%"
        } ?: "Codex"
}

object NotificationHelper {
    const val MONITOR_NOTIFICATION_ID = 4101
    private const val CHANGE_NOTIFICATION_ID = 4200
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
                description = "Persistent Codex Usage status"
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

    fun showMonitor(context: Context, snapshot: UsageSnapshot? = UsageStore(context).load()) {
        if (SecureTokenStore(context).load() == null || !canNotify(context)) return
        createChannels(context)
        context.getSystemService(NotificationManager::class.java)
            .notify(MONITOR_NOTIFICATION_ID, monitorNotification(context, snapshot))
    }

    fun cancelMonitor(context: Context) {
        context.getSystemService(NotificationManager::class.java).cancel(MONITOR_NOTIFICATION_ID)
    }

    private fun monitorNotification(context: Context, snapshot: UsageSnapshot?): Notification {
        val text = MonitorDisplay.content(snapshot)
        val builder = Notification.Builder(context, MONITOR_CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_usage)
            .setContentTitle(MonitorDisplay.title(snapshot))
            .setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText(text))
            .setContentIntent(mainPendingIntent(context))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_STATUS)
        if ((snapshot?.availableResetCount ?: 0) > 0) {
            builder.addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(context, R.drawable.ic_refresh),
                    "Use reset",
                    manualResetPendingIntent(context),
                ).build(),
            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            builder.setShortCriticalText(MonitorDisplay.shortCriticalText(snapshot))
            runCatching {
                builder.javaClass
                    .getMethod("setRequestPromotedOngoing", Boolean::class.javaPrimitiveType)
                    .invoke(builder, true)
            }
        }
        return builder.build()
    }

    fun notifyChange(context: Context, snapshot: UsageSnapshot, delta: Int) {
        if (!canNotify(context)) return
        createChannels(context)
        val sign = if (delta > 0) "+" else "−"
        val notification = Notification.Builder(context, CHANGE_CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_usage)
            .setContentTitle("${snapshot.primary.remainingPercent}% remaining ($sign${kotlin.math.abs(delta)}%)")
            .setContentText(
                "${snapshot.primary.remainingPercent}% remaining • " +
                    "resets ${UsageText.resetDate(snapshot.primary.resetAtEpochSeconds)}",
            )
            .setContentIntent(mainPendingIntent(context))
            .setAutoCancel(true)
            .setCategory(Notification.CATEGORY_STATUS)
            .build()
        context.getSystemService(NotificationManager::class.java)
            .notify(CHANGE_NOTIFICATION_ID, notification)
    }

    private fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun mainPendingIntent(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun manualResetPendingIntent(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context,
            1,
            Intent(context, MainActivity::class.java)
                .setAction(MainActivity.ACTION_CONFIRM_RESET)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}

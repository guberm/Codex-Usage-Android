package com.guberdev.codexusage

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

object NotificationHelper {
    private const val CHANGE_NOTIFICATION_BASE_ID = 4200
    private const val CHANGE_CHANNEL = "codex_changes"

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
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
            .notify(CHANGE_NOTIFICATION_BASE_ID + (System.currentTimeMillis() % 100).toInt(), notification)
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

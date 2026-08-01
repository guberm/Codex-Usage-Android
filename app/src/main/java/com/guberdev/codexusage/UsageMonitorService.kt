package com.guberdev.codexusage

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class UsageMonitorService : Service() {
    private var scheduler: ScheduledExecutorService? = null

    override fun onCreate() {
        super.onCreate()
        val notification = NotificationHelper.monitorNotification(this, UsageStore(this).load())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NotificationHelper.MONITOR_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NotificationHelper.MONITOR_NOTIFICATION_ID, notification)
        }
        scheduleChecks()
        UsageBackupJobService.schedule(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        scheduler?.shutdownNow()
        scheduler = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun scheduleChecks() {
        scheduler?.shutdownNow()
        val minutes = MonitorSettingsStore(this).load().checkEveryMinutes.toLong()
        scheduler = Executors.newSingleThreadScheduledExecutor().also {
            it.scheduleWithFixedDelay(
                { RefreshCoordinator.refresh(this) },
                0,
                minutes,
                TimeUnit.MINUTES,
            )
        }
    }

    companion object {
        fun start(context: Context) {
            if (SecureTokenStore(context).load() == null) return
            val intent = Intent(context, UsageMonitorService::class.java)
            runCatching {
                context.startForegroundService(intent)
            }
        }

        fun restart(context: Context) {
            context.stopService(Intent(context, UsageMonitorService::class.java))
            start(context)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, UsageMonitorService::class.java))
        }
    }
}

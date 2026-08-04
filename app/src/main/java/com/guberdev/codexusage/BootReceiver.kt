package com.guberdev.codexusage

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

object RefreshTrigger {
    fun shouldRefreshOnBroadcast(action: String?): Boolean = action == Intent.ACTION_USER_PRESENT
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (RefreshTrigger.shouldRefreshOnBroadcast(intent.action)) {
            if (SecureTokenStore(context).load() == null) return
            val pendingResult = goAsync()
            RefreshCoordinator.refresh(context) { pendingResult.finish() }
            return
        }
        if (
            intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            UsageBackupJobService.schedule(context)
            NotificationHelper.showMonitor(context)
        }
    }
}

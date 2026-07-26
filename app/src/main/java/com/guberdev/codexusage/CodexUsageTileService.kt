package com.guberdev.codexusage

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class CodexUsageTileService : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        render()
    }

    override fun onClick() {
        super.onClick()
        if (SecureTokenStore(this).load() == null) {
            openApp()
            return
        }
        qsTile?.apply {
            state = Tile.STATE_UNAVAILABLE
            label = "Codex • refresh…"
            updateTile()
        }
        UsageMonitorService.start(this)
        RefreshCoordinator.refresh(this) { render() }
    }

    private fun render() {
        val snapshot = UsageStore(this).load()
        qsTile?.apply {
            icon = Icon.createWithResource(this@CodexUsageTileService, R.drawable.ic_stat_usage)
            state = if (snapshot == null) Tile.STATE_INACTIVE else Tile.STATE_ACTIVE
            label = snapshot?.let { "Codex • ${it.primary.remainingPercent}% remaining" } ?: "Codex Usage"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                subtitle = snapshot?.let {
                    "Resets ${UsageText.shortResetDate(it.primary.resetAtEpochSeconds)}"
                } ?: "Tap to sign in"
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                stateDescription = snapshot?.let {
                    "${it.primary.remainingPercent}% remaining, resets ${UsageText.resetDate(it.primary.resetAtEpochSeconds)}"
                } ?: "Sign-in required"
            }
            updateTile()
        }
    }

    @Suppress("DEPRECATION")
    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun openApp() {
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this,
                7,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            startActivityAndCollapse(intent)
        }
    }
}

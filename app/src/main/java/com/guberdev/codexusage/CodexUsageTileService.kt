package com.guberdev.codexusage

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import kotlin.math.roundToInt

object TileDisplay {
    fun percent(remainingPercent: Int): String = "${remainingPercent.coerceIn(0, 100)}%"

    fun iconText(remainingPercent: Int): String = percent(remainingPercent)

    fun textScale(remainingPercent: Int): Float =
        if (iconText(remainingPercent).length > 3) 0.56f else 0.72f
}

object UsagePercentIcon {
    fun create(context: Context, remainingPercent: Int): Icon {
        val size = (48 * context.resources.displayMetrics.density).roundToInt().coerceAtLeast(48)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val text = TileDisplay.iconText(remainingPercent)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            textAlign = Paint.Align.CENTER
            textSize = size * TileDisplay.textScale(remainingPercent)
            typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
        }
        paint.textScaleX = (size * 0.94f / paint.measureText(text)).coerceAtMost(1f)
        val center = size / 2f
        canvas.drawText(text, center, center - (paint.ascent() + paint.descent()) / 2f, paint)
        return Icon.createWithBitmap(bitmap)
    }
}

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
            icon = snapshot?.let {
                UsagePercentIcon.create(this@CodexUsageTileService, it.primary.remainingPercent)
            } ?: Icon.createWithResource(this@CodexUsageTileService, R.drawable.ic_stat_usage)
            state = if (snapshot == null) Tile.STATE_INACTIVE else Tile.STATE_ACTIVE
            label = snapshot?.let {
                "Codex ${TileDisplay.percent(it.primary.remainingPercent)}"
            } ?: "Codex Usage"
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

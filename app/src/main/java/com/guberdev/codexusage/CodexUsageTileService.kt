package com.guberdev.codexusage

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import kotlin.math.roundToInt

object TileDisplay {
    const val ICON_TEXT_SCALE = 1.04f
    const val ICON_MAX_WIDTH_RATIO = 0.99f

    fun percent(remainingPercent: Int): String = "${remainingPercent.coerceIn(0, 100)}%"

    fun label(remainingPercent: Int): String = "${percent(remainingPercent)} Codex"
}

object UsagePercentIcon {
    fun create(context: Context, remainingPercent: Int): Icon {
        val size = (64 * context.resources.displayMetrics.density).roundToInt().coerceAtLeast(64)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val text = TileDisplay.percent(remainingPercent)
        val textPaint = paint(size * TileDisplay.ICON_TEXT_SCALE).apply { textAlign = Paint.Align.CENTER }
        textPaint.textSize *=
            (size * TileDisplay.ICON_MAX_WIDTH_RATIO / textPaint.measureText(text)).coerceAtMost(1f)
        val bounds = Rect().also { textPaint.getTextBounds(text, 0, text.length, it) }
        canvas.drawText(text, size / 2f, size / 2f - (bounds.top + bounds.bottom) / 2f, textPaint)
        return Icon.createWithBitmap(bitmap)
    }

    private fun paint(textSize: Float): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        this.textSize = textSize
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
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
                TileDisplay.label(it.primary.remainingPercent)
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

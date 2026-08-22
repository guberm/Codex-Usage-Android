package com.guberdev.codexusage

import android.content.Context
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

class UsageStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(): UsageSnapshot? {
        val primary = preferences.getInt(KEY_REMAINING, -1)
        if (primary !in 0..100) return null
        val reset = preferences.getLong(KEY_RESET, -1L).takeIf { it > 0 }
        val additional = runCatching {
            val array = JSONArray(preferences.getString(KEY_ADDITIONAL, "[]"))
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    val windows = item.optJSONArray("windows")?.let { storedWindows ->
                        buildList {
                            for (windowIndex in 0 until storedWindows.length()) {
                                val window = storedWindows.getJSONObject(windowIndex)
                                add(
                                    UsageWindow(
                                        remainingPercent = window.getInt("remaining"),
                                        resetAtEpochSeconds = window.optLong("reset").takeIf { it > 0 },
                                        windowSeconds = window.optLong("seconds").takeIf { it > 0 },
                                    ),
                                )
                            }
                        }
                    } ?: listOf(
                        UsageWindow(
                            remainingPercent = item.getInt("remaining"),
                            resetAtEpochSeconds = item.optLong("reset").takeIf { it > 0 },
                        ),
                    )
                    add(
                        AdditionalUsageLimit(
                            feature = item.getString("feature"),
                            name = item.optString("name").takeIf { it.isNotBlank() },
                            windows = windows,
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
        return UsageSnapshot(
            planType = preferences.getString(KEY_PLAN, null),
            primary = UsageWindow(primary, reset),
            additionalLimits = additional,
            creditBalance = preferences.getString(KEY_CREDITS, null),
            availableResetCount = preferences.getInt(KEY_AVAILABLE_RESETS, 0).coerceAtLeast(0),
            fetchedAtEpochMillis = preferences.getLong(KEY_FETCHED_AT, 0L),
        )
    }

    fun save(snapshot: UsageSnapshot, minimumChange: Int = 1): Int? {
        val previousNotificationBaseline = preferences
            .getInt(KEY_NOTIFICATION_BASELINE, -1)
            .takeIf { it in 0..100 }
        val change = UsageChangeDetector.evaluate(
            previousNotificationBaseline,
            snapshot.primary.remainingPercent,
            minimumChange,
        )
        val additional = JSONArray().apply {
            snapshot.additionalLimits.forEach { item ->
                put(
                    JSONObject()
                        .put("feature", item.feature)
                        .put("name", item.name)
                        .put(
                            "windows",
                            JSONArray().apply {
                                item.windows.forEach { window ->
                                    put(
                                        JSONObject()
                                            .put("remaining", window.remainingPercent)
                                            .put("reset", window.resetAtEpochSeconds)
                                            .put("seconds", window.windowSeconds),
                                    )
                                }
                            },
                        ),
                )
            }
        }
        preferences.edit()
            .putInt(KEY_REMAINING, snapshot.primary.remainingPercent)
            .putLong(KEY_RESET, snapshot.primary.resetAtEpochSeconds ?: -1L)
            .putString(KEY_PLAN, snapshot.planType)
            .putString(KEY_CREDITS, snapshot.creditBalance)
            .putInt(KEY_AVAILABLE_RESETS, snapshot.availableResetCount)
            .putString(KEY_ADDITIONAL, additional.toString())
            .putLong(KEY_FETCHED_AT, snapshot.fetchedAtEpochMillis)
            .putInt(KEY_NOTIFICATION_BASELINE, change.nextBaseline)
            .apply()
        return change.delta
    }

    fun clear() {
        preferences.edit().clear().apply()
    }

    companion object {
        private const val PREFS = "codex_usage_cache"
        private const val KEY_REMAINING = "remaining"
        private const val KEY_RESET = "reset"
        private const val KEY_PLAN = "plan"
        private const val KEY_CREDITS = "credits"
        private const val KEY_AVAILABLE_RESETS = "available_resets"
        private const val KEY_ADDITIONAL = "additional"
        private const val KEY_FETCHED_AT = "fetched_at"
        private const val KEY_NOTIFICATION_BASELINE = "notification_baseline"
    }
}

class PendingResetStore(context: Context) {
    private val preferences = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    @Synchronized
    fun getOrCreate(): String {
        preferences.getString(KEY_REQUEST_ID, null)?.let { return it }
        val requestId = UUID.randomUUID().toString()
        check(preferences.edit().putString(KEY_REQUEST_ID, requestId).commit()) {
            "Could not persist the reset request"
        }
        return requestId
    }

    fun clear() {
        preferences.edit().remove(KEY_REQUEST_ID).apply()
    }

    companion object {
        private const val PREFS = "codex_reset_request"
        private const val KEY_REQUEST_ID = "pending_request_id"
    }
}

object RefreshPolicy {
    const val BACKGROUND_INTERVAL_MINUTES = 15L
    const val NOTIFY_CHANGE_PERCENT = 1
}

object UsageText {
    fun resetDate(epochSeconds: Long?): String =
        epochSeconds?.let {
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.ENGLISH)
                .format(Date(it * 1000L))
        } ?: "not specified"

    fun shortResetDate(epochSeconds: Long?): String =
        epochSeconds?.let {
            DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.ENGLISH)
                .format(Date(it * 1000L))
        } ?: "—"

    fun featureName(feature: String): String = when (feature) {
        "codex_bengalfox" -> "GPT-5.3-Codex-Spark"
        else -> feature.replace('_', ' ')
    }

    fun windowName(windowSeconds: Long?): String = when (windowSeconds) {
        in 20L * 60 * 60..28L * 60 * 60 -> "Daily"
        in 6L * 24 * 60 * 60..8L * 24 * 60 * 60 -> "Weekly"
        in 27L * 24 * 60 * 60..32L * 24 * 60 * 60 -> "Monthly"
        in 1L..23L * 60 * 60 -> "${windowSeconds!! / 3600}h"
        else -> "Limit"
    }

    fun limitSummary(limit: AdditionalUsageLimit): String =
        limit.windows
            .sortedBy { it.windowSeconds ?: Long.MAX_VALUE }
            .joinToString(" · ") { window ->
                val label = windowName(window.windowSeconds)
                if (label == "Limit") "${window.remainingPercent}% left"
                else "$label ${window.remainingPercent}% left"
            }
}

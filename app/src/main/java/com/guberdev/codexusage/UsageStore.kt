package com.guberdev.codexusage

import android.content.Context
import java.text.DateFormat
import java.util.Date
import java.util.Locale
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
                    add(
                        AdditionalUsageLimit(
                            feature = item.getString("feature"),
                            window = UsageWindow(
                                remainingPercent = item.getInt("remaining"),
                                resetAtEpochSeconds = item.optLong("reset").takeIf { it > 0 },
                            ),
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
                        .put("remaining", item.window.remainingPercent)
                        .put("reset", item.window.resetAtEpochSeconds),
                )
            }
        }
        preferences.edit()
            .putInt(KEY_REMAINING, snapshot.primary.remainingPercent)
            .putLong(KEY_RESET, snapshot.primary.resetAtEpochSeconds ?: -1L)
            .putString(KEY_PLAN, snapshot.planType)
            .putString(KEY_CREDITS, snapshot.creditBalance)
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
        private const val KEY_ADDITIONAL = "additional"
        private const val KEY_FETCHED_AT = "fetched_at"
        private const val KEY_NOTIFICATION_BASELINE = "notification_baseline"
    }
}

data class MonitorSettings(
    val checkEveryMinutes: Int = 60,
    val notifyEveryPercent: Int = 1,
)

fun checkIntervalLabel(minutes: Int): String = when {
    minutes < 60 -> "$minutes min"
    minutes == 60 -> "1 hour"
    else -> "${minutes / 60} hours"
}

class MonitorSettingsStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(): MonitorSettings = MonitorSettings(
        checkEveryMinutes = (
            if (preferences.contains(KEY_CHECK_MINUTES)) {
                preferences.getInt(KEY_CHECK_MINUTES, 60)
            } else {
                preferences.getInt(KEY_CHECK_HOURS, 1) * 60
            }
        ).takeIf { it in CHECK_MINUTE_OPTIONS } ?: 60,
        notifyEveryPercent = preferences.getInt(KEY_NOTIFY_PERCENT, 1)
            .takeIf { it in NOTIFY_PERCENT_OPTIONS }
            ?: 1,
    )

    fun save(settings: MonitorSettings) {
        require(settings.checkEveryMinutes in CHECK_MINUTE_OPTIONS)
        require(settings.notifyEveryPercent in NOTIFY_PERCENT_OPTIONS)
        preferences.edit()
            .putInt(KEY_CHECK_MINUTES, settings.checkEveryMinutes)
            .remove(KEY_CHECK_HOURS)
            .putInt(KEY_NOTIFY_PERCENT, settings.notifyEveryPercent)
            .apply()
    }

    companion object {
        val CHECK_MINUTE_OPTIONS = listOf(15, 30, 45, 60, 120, 240, 360, 720, 1440)
        val NOTIFY_PERCENT_OPTIONS = listOf(1, 2, 5, 10, 20)
        private const val PREFS = "codex_monitor_settings"
        private const val KEY_CHECK_MINUTES = "check_minutes"
        private const val KEY_CHECK_HOURS = "check_hours"
        private const val KEY_NOTIFY_PERCENT = "notify_percent"
    }
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
}

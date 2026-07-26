package com.guberdev.codexusage

import java.util.Base64
import kotlin.math.abs
import kotlin.math.roundToInt
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

data class UsageWindow(
    val remainingPercent: Int,
    val resetAtEpochSeconds: Long?,
)

data class AdditionalUsageLimit(
    val feature: String,
    val window: UsageWindow,
)

data class UsageSnapshot(
    val planType: String?,
    val primary: UsageWindow,
    val additionalLimits: List<AdditionalUsageLimit>,
    val creditBalance: String?,
    val fetchedAtEpochMillis: Long = System.currentTimeMillis(),
)

class UsageParseException(message: String, cause: Throwable? = null) : Exception(message, cause)

class UsageParser {
    fun parse(json: String): UsageSnapshot {
        try {
            val root = JSONObject(json)
            val rateLimit = root.optJSONObject("rate_limit")
                ?: throw UsageParseException("The Codex response does not contain a primary limit")
            val primaryWindow = rateLimit.optJSONObject("primary_window")
                ?: throw UsageParseException("The Codex response does not contain a limit window")

            return UsageSnapshot(
                planType = root.optionalString("plan_type"),
                primary = parseWindow(primaryWindow),
                additionalLimits = parseAdditional(root.optJSONArray("additional_rate_limits")),
                creditBalance = root.optJSONObject("credits")?.optionalString("balance"),
            )
        } catch (error: UsageParseException) {
            throw error
        } catch (error: JSONException) {
            throw UsageParseException("Could not read the Codex response", error)
        }
    }

    private fun parseWindow(json: JSONObject): UsageWindow {
        if (!json.has("used_percent")) {
            throw UsageParseException("The limit window does not contain used_percent")
        }
        val remaining = (100.0 - json.getDouble("used_percent"))
            .roundToInt()
            .coerceIn(0, 100)
        val resetAt = if (json.has("reset_at") && !json.isNull("reset_at")) {
            json.getLong("reset_at")
        } else {
            null
        }
        return UsageWindow(remainingPercent = remaining, resetAtEpochSeconds = resetAt)
    }

    private fun parseAdditional(items: JSONArray?): List<AdditionalUsageLimit> {
        if (items == null) return emptyList()
        return buildList {
            for (index in 0 until items.length()) {
                val item = items.optJSONObject(index) ?: continue
                val feature = item.optionalString("metered_feature") ?: continue
                val window = item.optJSONObject("rate_limit")
                    ?.optJSONObject("primary_window")
                    ?: continue
                add(AdditionalUsageLimit(feature = feature, window = parseWindow(window)))
            }
        }
    }
}

data class UsageChangeDecision(
    val delta: Int?,
    val nextBaseline: Int,
)

object UsageChangeDetector {
    fun evaluate(
        previousRemaining: Int?,
        currentRemaining: Int,
        minimumChange: Int = 1,
    ): UsageChangeDecision {
        if (previousRemaining == null) return UsageChangeDecision(null, currentRemaining)
        val delta = currentRemaining - previousRemaining
        return if (abs(delta) >= minimumChange.coerceAtLeast(1)) {
            UsageChangeDecision(delta, currentRemaining)
        } else {
            UsageChangeDecision(null, previousRemaining)
        }
    }

    fun detect(
        previousRemaining: Int?,
        currentRemaining: Int,
        minimumChange: Int = 1,
    ): Int? = evaluate(previousRemaining, currentRemaining, minimumChange).delta
}

data class JwtClaims(
    val accountId: String?,
    val email: String?,
    val expiresAtEpochSeconds: Long?,
) {
    companion object {
        fun parse(jwt: String): JwtClaims {
            val parts = jwt.split('.')
            require(parts.size >= 3) { "Invalid OAuth token" }
            val payload = String(Base64.getUrlDecoder().decode(parts[1]))
            val json = JSONObject(payload)
            val auth = json.optJSONObject("https://api.openai.com/auth")
            return JwtClaims(
                accountId = auth?.optionalString("chatgpt_account_id"),
                email = json.optionalString("email")
                    ?: json.optJSONObject("https://api.openai.com/profile")?.optionalString("email"),
                expiresAtEpochSeconds = json.optLong("exp").takeIf { it > 0 },
            )
        }
    }
}

internal fun JSONObject.optionalString(name: String): String? =
    if (!has(name) || isNull(name)) null else optString(name).takeIf { it.isNotBlank() }

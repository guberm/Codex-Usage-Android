package com.guberdev.codexusage

import org.json.JSONObject

data class DeviceCode(
    val deviceAuthId: String,
    val userCode: String,
    val intervalSeconds: Long,
    val verificationUrl: String = "https://auth.openai.com/codex/device",
)

data class PendingAuthorization(
    val authorizationCode: String,
    val codeVerifier: String,
)

data class SessionTokens(
    val idToken: String,
    val accessToken: String,
    val refreshToken: String,
    val accountId: String,
    val accessTokenExpiresAtEpochSeconds: Long?,
)

internal fun SessionTokens.signedInText(): String =
    runCatching { JwtClaims.parse(idToken).email }.getOrNull()
        ?.let { "Signed in with $it" }
        ?: "Signed in with ChatGPT"

object AuthJsonParser {
    fun parseDeviceCode(json: String): DeviceCode {
        val root = JSONObject(json)
        return DeviceCode(
            deviceAuthId = root.getString("device_auth_id"),
            userCode = root.optString("user_code", root.optString("usercode")),
            intervalSeconds = root.optLong("interval", 5L).coerceAtLeast(1L),
        )
    }

    fun parsePendingAuthorization(json: String): PendingAuthorization {
        val root = JSONObject(json)
        return PendingAuthorization(
            authorizationCode = root.getString("authorization_code"),
            codeVerifier = root.getString("code_verifier"),
        )
    }

    fun parseTokens(json: String, fallbackRefreshToken: String? = null): SessionTokens {
        val root = JSONObject(json)
        val idToken = root.getString("id_token")
        val accessToken = root.getString("access_token")
        val refreshToken = root.optionalString("refresh_token") ?: fallbackRefreshToken
            ?: error("OAuth response does not contain a refresh token")
        val accountId = JwtClaims.parse(idToken).accountId
            ?: JwtClaims.parse(accessToken).accountId
            ?: error("OAuth token does not contain a ChatGPT workspace id")
        return SessionTokens(
            idToken = idToken,
            accessToken = accessToken,
            refreshToken = refreshToken,
            accountId = accountId,
            accessTokenExpiresAtEpochSeconds = JwtClaims.parse(accessToken).expiresAtEpochSeconds,
        )
    }
}

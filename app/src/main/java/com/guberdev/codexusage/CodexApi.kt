package com.guberdev.codexusage

import java.net.HttpURLConnection
import java.net.UnknownHostException
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import org.json.JSONObject

class HttpStatusException(val statusCode: Int) : Exception("HTTP $statusCode")

internal fun <T> retryUnknownHost(
    maxAttempts: Int = 3,
    delayMillis: Long = 1_000,
    action: () -> T,
): T {
    require(maxAttempts > 0)
    for (attempt in 1..maxAttempts) {
        try {
            return action()
        } catch (error: UnknownHostException) {
            if (attempt == maxAttempts) throw error
            if (delayMillis > 0) Thread.sleep(delayMillis * attempt)
        }
    }
    error("Unreachable")
}

internal fun <T> pollUntilAuthorized(
    deadlineMillis: Long,
    intervalMillis: Long,
    nowMillis: () -> Long = System::currentTimeMillis,
    sleep: (Long) -> Unit = Thread::sleep,
    onUnknownHost: () -> Unit = {},
    poll: () -> T?,
): T? {
    while (nowMillis() < deadlineMillis) {
        sleep(intervalMillis)
        try {
            poll()?.let { return it }
        } catch (_: UnknownHostException) {
            onUnknownHost()
        }
    }
    return null
}

private object CodexHttp {
    fun request(
        method: String,
        url: String,
        body: String? = null,
        contentType: String? = null,
        headers: Map<String, String> = emptyMap(),
    ): String = retryUnknownHost {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 20_000
            useCaches = false
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "codex-usage-android/0.1.4")
            headers.forEach { (name, value) -> setRequestProperty(name, value) }
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", contentType ?: "application/json")
            }
        }
        try {
            if (body != null) {
                connection.outputStream.bufferedWriter().use { it.write(body) }
            }
            val status = connection.responseCode
            if (status !in 200..299) {
                connection.errorStream?.close()
                throw HttpStatusException(status)
            }
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}

class CodexAuthClient {
    fun requestDeviceCode(): DeviceCode {
        val body = JSONObject().put("client_id", CLIENT_ID).toString()
        return AuthJsonParser.parseDeviceCode(
            CodexHttp.request(
                method = "POST",
                url = "$ISSUER/api/accounts/deviceauth/usercode",
                body = body,
            ),
        )
    }

    fun pollDeviceCode(code: DeviceCode): PendingAuthorization? {
        val body = JSONObject()
            .put("device_auth_id", code.deviceAuthId)
            .put("user_code", code.userCode)
            .toString()
        return try {
            AuthJsonParser.parsePendingAuthorization(
                CodexHttp.request(
                    method = "POST",
                    url = "$ISSUER/api/accounts/deviceauth/token",
                    body = body,
                ),
            )
        } catch (error: HttpStatusException) {
            if (error.statusCode == 403 || error.statusCode == 404) null else throw error
        }
    }

    fun exchangeCode(pending: PendingAuthorization): SessionTokens {
        val body = form(
            "grant_type" to "authorization_code",
            "code" to pending.authorizationCode,
            "redirect_uri" to "$ISSUER/deviceauth/callback",
            "client_id" to CLIENT_ID,
            "code_verifier" to pending.codeVerifier,
        )
        return AuthJsonParser.parseTokens(
            CodexHttp.request(
                method = "POST",
                url = "$ISSUER/oauth/token",
                body = body,
                contentType = "application/x-www-form-urlencoded",
            ),
        )
    }

    fun refresh(tokens: SessionTokens): SessionTokens {
        val body = JSONObject()
            .put("client_id", CLIENT_ID)
            .put("grant_type", "refresh_token")
            .put("refresh_token", tokens.refreshToken)
            .toString()
        return AuthJsonParser.parseTokens(
            CodexHttp.request(
                method = "POST",
                url = "$ISSUER/oauth/token",
                body = body,
            ),
            fallbackRefreshToken = tokens.refreshToken,
        )
    }

    fun ensureFresh(tokens: SessionTokens): SessionTokens {
        val expiresAt = tokens.accessTokenExpiresAtEpochSeconds ?: return tokens
        val now = System.currentTimeMillis() / 1000L
        return if (expiresAt - now <= 300L) refresh(tokens) else tokens
    }

    private fun form(vararg values: Pair<String, String>): String =
        values.joinToString("&") { (name, value) ->
            "${encode(name)}=${encode(value)}"
        }

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.name())

    companion object {
        const val CLIENT_ID = "app_EMoamEEZ73f0CkXaXp7hrann"
        private const val ISSUER = "https://auth.openai.com"
    }
}

class CodexUsageApi(private val parser: UsageParser = UsageParser()) {
    fun fetch(tokens: SessionTokens): UsageSnapshot =
        parser.parse(
            CodexHttp.request(
                method = "GET",
                url = "https://chatgpt.com/backend-api/wham/usage",
                headers = mapOf(
                    "Authorization" to "Bearer ${tokens.accessToken}",
                    "ChatGPT-Account-Id" to tokens.accountId,
                ),
            ),
        )
}

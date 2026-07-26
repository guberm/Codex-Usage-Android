package com.guberdev.codexusage

import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Test

class AuthJsonParserTest {
    @Test
    fun `device code response is parsed`() {
        val code = AuthJsonParser.parseDeviceCode(
            """
            {
              "device_auth_id": "device-123",
              "user_code": "ABCD-EFGH",
              "interval": 5
            }
            """.trimIndent(),
        )

        assertEquals("device-123", code.deviceAuthId)
        assertEquals("ABCD-EFGH", code.userCode)
        assertEquals(5L, code.intervalSeconds)
    }

    @Test
    fun `token exchange derives account and expiry from JWTs`() {
        val idToken = jwt(
            """
            {
              "https://api.openai.com/auth": {
                "chatgpt_account_id": "workspace-123"
              }
            }
            """.trimIndent(),
        )
        val accessToken = jwt("""{"exp":2000000000}""")

        val tokens = AuthJsonParser.parseTokens(
            """{"id_token":"$idToken","access_token":"$accessToken","refresh_token":"refresh-1"}""",
        )

        assertEquals("workspace-123", tokens.accountId)
        assertEquals(2000000000L, tokens.accessTokenExpiresAtEpochSeconds)
        assertEquals("refresh-1", tokens.refreshToken)
    }

    @Test
    fun `refresh response keeps rotated refresh token`() {
        val idToken = jwt(
            """{"https://api.openai.com/auth":{"chatgpt_account_id":"workspace-123"}}""",
        )
        val accessToken = jwt("""{"exp":2100000000}""")

        val tokens = AuthJsonParser.parseTokens(
            """{"id_token":"$idToken","access_token":"$accessToken","refresh_token":"refresh-2"}""",
        )

        assertEquals("refresh-2", tokens.refreshToken)
        assertEquals(2100000000L, tokens.accessTokenExpiresAtEpochSeconds)
    }

    @Test
    fun `signed in label shows the account email`() {
        val tokens = SessionTokens(
            idToken = jwt("""{"email":"michael@example.com"}"""),
            accessToken = jwt("""{"exp":2100000000}"""),
            refreshToken = "refresh-1",
            accountId = "workspace-123",
            accessTokenExpiresAtEpochSeconds = 2100000000L,
        )

        assertEquals("Signed in with michael@example.com", tokens.signedInText())
    }

    @Test
    fun `signed in label uses the OpenAI profile email`() {
        val tokens = SessionTokens(
            idToken = jwt(
                """{"https://api.openai.com/profile":{"email":"profile@example.com"}}""",
            ),
            accessToken = jwt("""{"exp":2100000000}"""),
            refreshToken = "refresh-1",
            accountId = "workspace-123",
            accessTokenExpiresAtEpochSeconds = 2100000000L,
        )

        assertEquals("Signed in with profile@example.com", tokens.signedInText())
    }

    private fun jwt(payload: String): String {
        val encoder = Base64.getUrlEncoder().withoutPadding()
        val header = encoder.encodeToString("""{"alg":"none"}""".toByteArray())
        return "$header.${encoder.encodeToString(payload.toByteArray())}.signature"
    }
}

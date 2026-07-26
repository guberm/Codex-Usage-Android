package com.guberdev.codexusage

import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Test

class JwtClaimsTest {
    @Test
    fun `extracts account id from OpenAI auth claim`() {
        val header = encode("""{"alg":"none"}""")
        val payload = encode(
            """
            {
              "exp": 2000000000,
              "https://api.openai.com/auth": {
                "chatgpt_account_id": "workspace-123"
              }
            }
            """.trimIndent(),
        )

        val claims = JwtClaims.parse("$header.$payload.signature")

        assertEquals("workspace-123", claims.accountId)
        assertEquals(2000000000L, claims.expiresAtEpochSeconds)
    }

    private fun encode(value: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray())
}

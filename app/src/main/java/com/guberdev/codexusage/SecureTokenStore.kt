package com.guberdev.codexusage

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import org.json.JSONObject

class SecureTokenStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun save(tokens: SessionTokens) {
        val json = JSONObject()
            .put("id_token", tokens.idToken)
            .put("access_token", tokens.accessToken)
            .put("refresh_token", tokens.refreshToken)
            .put("account_id", tokens.accountId)
            .put("access_exp", tokens.accessTokenExpiresAtEpochSeconds)
            .toString()
            .toByteArray()

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        preferences.edit()
            .putString(KEY_IV, Base64.getEncoder().encodeToString(cipher.iv))
            .putString(KEY_PAYLOAD, Base64.getEncoder().encodeToString(cipher.doFinal(json)))
            .apply()
    }

    fun load(): SessionTokens? {
        val iv = preferences.getString(KEY_IV, null) ?: return null
        val payload = preferences.getString(KEY_PAYLOAD, null) ?: return null
        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateKey(),
                GCMParameterSpec(128, Base64.getDecoder().decode(iv)),
            )
            val root = JSONObject(String(cipher.doFinal(Base64.getDecoder().decode(payload))))
            SessionTokens(
                idToken = root.getString("id_token"),
                accessToken = root.getString("access_token"),
                refreshToken = root.getString("refresh_token"),
                accountId = root.getString("account_id"),
                accessTokenExpiresAtEpochSeconds = root.optLong("access_exp").takeIf { it > 0 },
            )
        }.getOrNull()
    }

    fun clear() {
        preferences.edit().clear().apply()
        runCatching {
            val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
            keyStore.deleteEntry(KEY_ALIAS)
        }
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER).run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build(),
            )
            generateKey()
        }
    }

    companion object {
        private const val PREFS = "encrypted_codex_session"
        private const val KEY_IV = "iv"
        private const val KEY_PAYLOAD = "payload"
        private const val KEY_ALIAS = "codex_usage_session_key"
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}

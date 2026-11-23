package com.termx.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.termx.core.security.ApiKeyStore

/**
 * Android implementation of [ApiKeyStore] backed by EncryptedSharedPreferences.
 * API keys are encrypted at rest and never written to plain preferences or the repo.
 */
class EncryptedKeyStore(context: Context) : ApiKeyStore {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "termx_secure",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    override suspend fun put(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    override suspend fun get(key: String): String? = prefs.getString(key, null)

    override suspend fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    override suspend fun contains(key: String): Boolean = prefs.contains(key)
}

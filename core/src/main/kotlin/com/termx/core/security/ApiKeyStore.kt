package com.termx.core.security

/**
 * Secure storage abstraction for sensitive values (API keys, tokens).
 *
 * The platform supplies a concrete implementation:
 * - Android → `EncryptedSharedPreferences` (see `android/.../security/EncryptedKeyStore`).
 * - Desktop → OS credential store / obfuscated preferences.
 *
 * Plain-text storage must never be used. Keys are read on demand and never logged.
 */
interface ApiKeyStore {
    suspend fun put(key: String, value: String)
    suspend fun get(key: String): String?
    suspend fun remove(key: String)
    suspend fun contains(key: String): Boolean
}

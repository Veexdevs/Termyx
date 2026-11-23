package com.termx.desktop

import com.termx.core.security.ApiKeyStore
import com.termx.core.util.TermyxJson
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import java.io.File

/**
 * Desktop key store backed by a JSON file under the user's config dir.
 *
 * NOTE: this is a development-grade store. A production build should prefer the OS
 * credential manager (e.g. Windows Credential Manager / libsecret / Keychain). The
 * interface [ApiKeyStore] lets the platform swap in a stronger implementation without
 * touching the rest of the app.
 */
class DesktopKeyStore(private val file: File) : ApiKeyStore {
    private val serializer = MapSerializer(String.serializer(), String.serializer())

    @Synchronized
    private fun readAll(): MutableMap<String, String> {
        if (!file.exists()) return mutableMapOf()
        return runCatching { TermyxJson.decodeFromString(serializer, file.readText()).toMutableMap() }
            .getOrDefault(mutableMapOf())
    }

    @Synchronized
    private fun writeAll(map: Map<String, String>) {
        file.parentFile?.mkdirs()
        file.writeText(TermyxJson.encodeToString(serializer, map))
    }

    override suspend fun put(key: String, value: String) {
        val map = readAll(); map[key] = value; writeAll(map)
    }

    override suspend fun get(key: String): String? = readAll()[key]

    override suspend fun remove(key: String) {
        val map = readAll(); map.remove(key); writeAll(map)
    }

    override suspend fun contains(key: String): Boolean = readAll().containsKey(key)
}

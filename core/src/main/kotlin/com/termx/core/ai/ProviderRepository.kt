package com.termx.core.ai

import com.termx.core.model.AiProvider
import com.termx.core.security.ApiKeyStore
import com.termx.core.util.TermyxJson
import kotlinx.serialization.builtins.ListSerializer

/**
 * Persists AI providers through an [ApiKeyStore]. The whole provider blob (including the
 * API key) is encrypted at rest by the platform store; keys are never written in plaintext.
 * Shared by Android and Desktop.
 */
class ProviderRepository(private val keyStore: ApiKeyStore) {
    private val KEY = "ai_providers_v1"
    private val DEF = "ai_default_v1"

    suspend fun load(): List<AiProvider> {
        val raw = keyStore.get(KEY) ?: return AiProvider.presets()
            .map { it.copy(apiKey = "", isDefault = it.name == "OpenRouter") }
        return runCatching { TermyxJson.decodeFromString(ListSerializer(AiProvider.serializer()), raw) }
            .getOrDefault(AiProvider.presets())
    }

    suspend fun save(list: List<AiProvider>) {
        keyStore.put(KEY, TermyxJson.encodeToString(ListSerializer(AiProvider.serializer()), list))
    }

    suspend fun setDefault(id: String) {
        keyStore.put(DEF, id)
    }

    suspend fun default(): AiProvider? {
        val id = keyStore.get(DEF) ?: return load().firstOrNull { it.isDefault }
        return load().firstOrNull { it.id == id }
    }
}

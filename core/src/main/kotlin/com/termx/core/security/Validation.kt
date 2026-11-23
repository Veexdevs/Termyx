package com.termx.core.security

import com.termx.core.model.AiProvider

/** Validation helpers so the UI rejects obviously bad configuration before a network call. */
object Validation {
    private val URL_RE = Regex("^https?://[\\w.-]+(:\\d+)?(/.*)?$", RegexOption.IGNORE_CASE)

    fun providerName(name: String): Boolean = name.isNotBlank() && name.length <= 64
    fun apiUrl(url: String): Boolean = URL_RE.matches(url.trim())
    fun apiKey(key: String): Boolean = key.isNotBlank() && key.length >= 8
    fun modelId(model: String): Boolean = model.isNotBlank()

    /** Returns a list of human-readable problems; empty means valid. */
    fun validateProvider(provider: AiProvider): List<String> = buildList {
        if (!providerName(provider.name)) add("Provider name is required (max 64 chars).")
        if (!apiUrl(provider.baseUrl)) add("API URL must be a valid http(s) URL.")
        if (!apiKey(provider.apiKey)) add("API key is required and must be at least 8 characters.")
        if (!modelId(provider.model)) add("Model ID is required.")
    }

    /** Redact a secret for safe display/logging. */
    fun redact(value: String): String = if (value.length <= 4) "****" else value.take(2) + "*".repeat(6) + value.takeLast(2)
}

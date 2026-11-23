package com.termx.core.model

import kotlinx.serialization.Serializable

/**
 * API format understood by [com.termx.core.ai.AiClient].
 * OPENAI covers OpenAI, OpenRouter, DeepSeek, Groq and any OpenAI-compatible
 * local server. ANTHROPIC and GEMINI use their respective REST shapes.
 */
enum class ApiFormat { OPENAI, ANTHROPIC, GEMINI }

/**
 * A user-configured AI provider. The [apiKey] is stored encrypted by the platform
 * key store and is never written to the repo or to plain preferences.
 */
@Serializable
data class AiProvider(
    val id: String,
    val name: String,
    val baseUrl: String,
    val apiKey: String,
    val model: String,
    val format: ApiFormat = ApiFormat.OPENAI,
    val temperature: Double = 0.7,
    val maxTokens: Int = 2048,
    val isDefault: Boolean = false
) {
    val trimmedBaseUrl: String
        get() = baseUrl.trim().trimEnd('/')

    companion object {
        fun presets(): List<AiProvider> = listOf(
            AiProvider("openrouter", "OpenRouter", "https://openrouter.ai/api/v1", "", "anthropic/claude-3.5-sonnet", ApiFormat.OPENAI),
            AiProvider("openai", "OpenAI", "https://api.openai.com/v1", "", "gpt-4o", ApiFormat.OPENAI),
            AiProvider("anthropic", "Anthropic", "https://api.anthropic.com/v1", "", "claude-3-5-sonnet-20241022", ApiFormat.ANTHROPIC),
            AiProvider("gemini", "Google Gemini", "https://generativelanguage.googleapis.com/v1beta", "", "gemini-1.5-pro", ApiFormat.GEMINI),
            AiProvider("deepseek", "DeepSeek", "https://api.deepseek.com/v1", "", "deepseek-chat", ApiFormat.OPENAI),
            AiProvider("groq", "Groq", "https://api.groq.com/openai/v1", "", "llama-3.3-70b-versatile", ApiFormat.OPENAI)
        )
    }
}

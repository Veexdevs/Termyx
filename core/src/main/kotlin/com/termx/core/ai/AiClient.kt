package com.termx.core.ai

import com.termx.core.model.AiProvider
import com.termx.core.util.TermyxJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * Real HTTP client for AI providers. Supports OpenAI-compatible, Anthropic and
 * Gemini REST shapes through adapters.
 *
 * Uses [HttpURLConnection] (not java.net.http.HttpClient) so the same code compiles
 * and runs on both Android and the desktop JVM. No keys are logged; network/parse
 * failures are surfaced as exceptions for the caller to show.
 */
object AiClient {

    /** Send a chat request and return the assistant text. */
    suspend fun chat(provider: AiProvider, messages: List<ChatMessage>): String = withContext(Dispatchers.IO) {
        val body = when (provider.format) {
            com.termx.core.model.ApiFormat.OPENAI -> openAiBody(provider, messages)
            com.termx.core.model.ApiFormat.ANTHROPIC -> anthropicBody(provider, messages)
            com.termx.core.model.ApiFormat.GEMINI -> geminiBody(provider, messages)
        }
        val (url, headers, parse) = when (provider.format) {
            com.termx.core.model.ApiFormat.OPENAI -> Triple(
                "${provider.trimmedBaseUrl}/chat/completions",
                mapOf("Authorization" to "Bearer ${provider.apiKey}", "Content-Type" to "application/json"),
                ::parseOpenAi
            )
            com.termx.core.model.ApiFormat.ANTHROPIC -> Triple(
                "${provider.trimmedBaseUrl}/messages",
                mapOf(
                    "x-api-key" to provider.apiKey,
                    "anthropic-version" to "2023-06-01",
                    "Content-Type" to "application/json"
                ),
                ::parseAnthropic
            )
            com.termx.core.model.ApiFormat.GEMINI -> Triple(
                "${provider.trimmedBaseUrl}/models/${provider.model}:generateContent?key=${provider.apiKey}",
                mapOf("Content-Type" to "application/json"),
                ::parseGemini
            )
        }

        val (code, response) = request("POST", url, headers, body)
        if (code !in 200..299) {
            throw RuntimeException("AI provider ${provider.name} returned HTTP $code: ${response.take(500)}")
        }
        parse(response)
    }

    /** Validate connectivity. Returns true on success; throws on failure. */
    suspend fun testConnection(provider: AiProvider): Boolean {
        val text = chat(provider, listOf(ChatMessage("user", "ping")))
        return text.isNotBlank()
    }

    /** List models when the provider exposes an OpenAI-compatible /models endpoint. */
    suspend fun listModels(provider: AiProvider): List<ModelInfo> = withContext(Dispatchers.IO) {
        if (provider.format != com.termx.core.model.ApiFormat.OPENAI) return@withContext emptyList()
        val (code, response) = request(
            "GET", "${provider.trimmedBaseUrl}/models",
            mapOf("Authorization" to "Bearer ${provider.apiKey}"), null
        )
        if (code !in 200..299) return@withContext emptyList()
        runCatching {
            val root = TermyxJson.parseToJsonElement(response).jsonObject
            val arr = root["data"]?.jsonArray ?: JsonArray(emptyList())
            arr.mapNotNull { el ->
                val obj = el.jsonObject
                val id = obj["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                ModelInfo(id, obj["owned_by"]?.jsonPrimitive?.contentOrNull)
            }
        }.getOrDefault(emptyList())
    }

    // ---- low-level request (HttpURLConnection: available on Android + JVM) ----

    private fun request(method: String, url: String, headers: Map<String, String>, body: String?): Pair<Int, String> {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 30_000
            readTimeout = 120_000
            headers.forEach { (k, v) -> setRequestProperty(k, v) }
            if (body != null) {
                doOutput = true
                outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }
            }
        }
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val response = stream?.bufferedReader()?.use { it.readText() } ?: ""
        conn.disconnect()
        return code to response
    }

    // ---- request bodies ----

    private fun openAiBody(p: AiProvider, messages: List<ChatMessage>): String =
        buildJsonObject {
            put("model", p.model)
            put("temperature", p.temperature)
            put("max_tokens", p.maxTokens)
            putJsonArray("messages") {
                messages.forEach { add(buildJsonObject { put("role", it.role); put("content", it.content) }) }
            }
        }.toString()

    private fun anthropicBody(p: AiProvider, messages: List<ChatMessage>): String {
        val system = messages.filter { it.role == "system" }.joinToString("\n") { it.content }
        val rest = messages.filter { it.role != "system" }
        return buildJsonObject {
            put("model", p.model)
            put("max_tokens", p.maxTokens)
            put("temperature", p.temperature)
            if (system.isNotBlank()) put("system", system)
            putJsonArray("messages") {
                rest.forEach { add(buildJsonObject { put("role", it.role); put("content", it.content) }) }
            }
        }.toString()
    }

    private fun geminiBody(p: AiProvider, messages: List<ChatMessage>): String =
        buildJsonObject {
            putJsonArray("contents") {
                messages.forEach { m ->
                    add(buildJsonObject {
                        put("role", if (m.role == "assistant") "model" else "user")
                        putJsonArray("parts") { add(buildJsonObject { put("text", m.content) }) }
                    })
                }
            }
            putJsonObject("generationConfig") {
                put("temperature", p.temperature)
                put("maxOutputTokens", p.maxTokens)
            }
        }.toString()

    // ---- response parsing ----

    private fun parseOpenAi(body: String): String {
        val root = TermyxJson.parseToJsonElement(body).jsonObject
        val choices = root["choices"]?.jsonArray ?: return ""
        val first = (choices.firstOrNull() as? JsonObject) ?: return ""
        val message = first["message"]?.jsonObject ?: return ""
        return message["content"]?.jsonPrimitive?.contentOrNull ?: ""
    }

    private fun parseAnthropic(body: String): String {
        val root = TermyxJson.parseToJsonElement(body).jsonObject
        val content = root["content"]?.jsonArray ?: return ""
        return content.firstNotNullOfOrNull { (it as? JsonObject)?.get("text")?.jsonPrimitive?.contentOrNull } ?: ""
    }

    private fun parseGemini(body: String): String {
        val root = TermyxJson.parseToJsonElement(body).jsonObject
        val candidates = root["candidates"]?.jsonArray ?: return ""
        val first = (candidates.firstOrNull() as? JsonObject) ?: return ""
        val content = first["content"]?.jsonObject ?: return ""
        val parts = content["parts"]?.jsonArray ?: return ""
        return parts.firstNotNullOfOrNull { (it as? JsonObject)?.get("text")?.jsonPrimitive?.contentOrNull } ?: ""
    }
}

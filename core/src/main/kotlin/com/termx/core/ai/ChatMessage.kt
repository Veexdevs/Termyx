package com.termx.core.ai

/** A chat message exchanged with an AI provider. */
data class ChatMessage(
    val role: String, // "system" | "user" | "assistant"
    val content: String
)

/** A model discoverable via a provider's "list models" capability. */
data class ModelInfo(
    val id: String,
    val ownedBy: String? = null
)

package com.termx.core.model

import kotlinx.serialization.Serializable

enum class ServerType {
    NODE, PYTHON, CPP, PHP, JAVA, MINECRAFT, DISCORD_BOT, TELEGRAM_BOT, HTTP, DATABASE, CUSTOM
}

enum class ServerState {
    STOPPED, STARTING, RUNNING, STOPPING, CRASHED, UNKNOWN
}

/**
 * Persistent configuration for a managed server.
 *
 * The actual process lifecycle is owned by [com.termx.core.server.ServerManager];
 * this record is the user-defined intent (command, directory, port, policies).
 */
@Serializable
data class ServerConfig(
    val id: String,
    val name: String,
    val type: ServerType,
    val startCommand: String,
    val workingDirectory: String,
    val port: Int = 0,
    val autoRestart: Boolean = false,
    val autoStart: Boolean = false,
    val runInBackground: Boolean = true,
    val environment: Map<String, String> = emptyMap(),
    val maxRestarts: Int = 5
) {
    val typeLabel: String get() = type.name.lowercase().replaceFirstChar { it.uppercase() }
}

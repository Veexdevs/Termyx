package com.termx.core.util

import kotlinx.serialization.json.Json

/** Shared lenient JSON instance for the core module. */
val TermyxJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    prettyPrint = false
    encodeDefaults = true
}

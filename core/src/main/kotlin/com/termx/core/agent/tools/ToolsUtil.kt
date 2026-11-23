package com.termx.core.agent.tools

import java.io.File

/** Resolve a (possibly relative) path against the sandbox root with traversal protection. */
internal fun resolveWithin(root: String, path: String): File {
    val base = File(root).canonicalFile
    val combined = File(base, path).canonicalFile
    if (!combined.path.startsWith(base.path)) {
        throw SecurityException("Path '$path' escapes the sandbox root.")
    }
    return combined
}

internal fun Map<String, String>.str(key: String, default: String = ""): String = get(key) ?: default

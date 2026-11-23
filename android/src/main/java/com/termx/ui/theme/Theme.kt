package com.termx.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * TERMYX visual identity: minimalist black & white, professional, no noise.
 * Monochrome Material3 schemes — pure black/white surfaces with neutral outlines.
 */
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF000000),
    onPrimary = Color(0xFFFFFFFF),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF000000),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF2F2F2),
    onSurface = Color(0xFF000000),
    outline = Color(0xFFD0D0D0)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFFFFF),
    onPrimary = Color(0xFF000000),
    background = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF0E0E0E),
    surfaceVariant = Color(0xFF1A1A1A),
    onSurface = Color(0xFFFFFFFF),
    outline = Color(0xFF3A3A3A)
)

@Composable
fun TermyxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = TermyxTypography,
        content = content
    )
}

private val TermyxTypography = androidx.compose.material3.Typography()

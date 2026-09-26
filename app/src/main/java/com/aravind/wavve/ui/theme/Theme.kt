package com.aravind.wavve.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Color0 = Color(0xFF1A0A26)

private val WavveColorScheme = darkColorScheme(
    primary = WavvePrimary,
    secondary = WavveSecondary,
    background = WavveBackground,
    surface = WavveSurface,
    surfaceVariant = WavveSurfaceLight,
    onBackground = WavveOnBackground,
    onSurface = WavveOnBackground,
    onPrimary = Color0,
    error = WavveError
)

@Composable
fun WavveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = WavveColorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}
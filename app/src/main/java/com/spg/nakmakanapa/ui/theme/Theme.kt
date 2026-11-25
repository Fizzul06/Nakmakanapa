package com.spg.nakmakanapa.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = DarkGreenHeader,
    onPrimary = CardBackground,
    primaryContainer = DarkGreenContainer,
    onPrimaryContainer = CardBackground,
    secondary = AccentYellow,
    onSecondary = TextPrimary,
    background = LightGrayBackground,
    onBackground = TextPrimary,
    surface = CardBackground,
    onSurface = TextPrimary
)

@Composable
fun NakMakanApaTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
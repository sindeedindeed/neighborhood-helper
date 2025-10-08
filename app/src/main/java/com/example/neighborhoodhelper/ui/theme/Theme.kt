package com.example.neighborhoodhelper.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = DarkBlue,
    onPrimary = White,
    secondary = LightBlue,
    background = LightBlue,
    onBackground = DarkBlue,
    surface = White,
    onSurface = DarkBlue
)

@Composable
fun NeighborhoodHelperTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content
    )
}

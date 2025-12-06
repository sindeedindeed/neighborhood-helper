package com.example.neighborhoodhelper.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryPurple,
    secondary = WarmSecondary,
    tertiary = Pink80,
    background = BackgroundLight,
    surface = BackgroundLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantDark
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryPurple,
    secondary = WarmSecondary,
    tertiary = Pink40,
    background = BackgroundLight,
    surface = BackgroundLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantDark
)

@Composable
fun NeighborhoodHelperTheme(
    darkTheme: Boolean = false,  // Always use light theme
    dynamicColor: Boolean = false,  // Disable dynamic colors
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme  // Force light color scheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

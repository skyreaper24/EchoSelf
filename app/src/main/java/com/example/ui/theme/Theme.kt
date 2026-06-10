package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CosmicDarkColorScheme = darkColorScheme(
    primary = PolarStarBlue,
    secondary = NebulaCyan,
    tertiary = CelestialPurple,
    background = CosmicBackground,
    surface = CosmicSurface,
    surfaceVariant = CosmicCardInner,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = CosmicTextOnBackground,
    onSurface = CosmicTextOnBackground,
    onSurfaceVariant = CosmicTextMuted,
    outline = CosmicBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force premium cosmic dark mode by default for constellation viewing
    dynamicColor: Boolean = false, // Disable dynamic light mode to protect our highly customized art scheme
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = CosmicDarkColorScheme,
        typography = Typography,
        content = content
    )
}

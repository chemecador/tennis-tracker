package com.chemecador.tennistracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BrandColors = darkColorScheme(
    primary = BrandLime,
    onPrimary = BrandInk,
    secondary = BrandLime,
    onSecondary = BrandInk,
    background = BrandInk,
    onBackground = Color.White,
    surface = BrandInk,
    onSurface = Color.White,
    surfaceVariant = BrandInkSoft,
    onSurfaceVariant = BrandLime,
    outline = BrandLime,
)

@Composable
fun TennisTrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = BrandColors, content = content)
}

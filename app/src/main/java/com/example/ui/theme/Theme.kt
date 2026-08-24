package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val HighDensityColorScheme =
  lightColorScheme(
    primary = HighDensityPrimary,
    onPrimary = Color.White,
    primaryContainer = HighDensityPrimaryContainer,
    onPrimaryContainer = HighDensityOnPrimaryContainer,
    secondary = HighDensitySecondary,
    onSecondary = Color.White,
    secondaryContainer = HighDensitySecondaryContainer,
    onSecondaryContainer = HighDensityTextPrimary,
    tertiary = HighDensityCrimson,
    onTertiary = Color.White,
    background = HighDensityBg,
    onBackground = HighDensityTextPrimary,
    surface = HighDensitySurface,
    onSurface = HighDensityTextPrimary,
    surfaceVariant = HighDensityBg,
    onSurfaceVariant = HighDensityTextSecondary,
    outline = HighDensityCardBorder,
    error = HighDensityCrimson,
    onError = Color.White,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  MaterialTheme(colorScheme = HighDensityColorScheme, typography = Typography, content = content)
}



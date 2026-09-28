package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MayaDarkColorScheme = darkColorScheme(
    primary = MayaCyan,
    onPrimary = Color.Black,
    primaryContainer = MayaSurfaceElevated,
    onPrimaryContainer = MayaCyan,
    secondary = MayaPurple,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF261D4E),
    onSecondaryContainer = MayaPurple,
    tertiary = MayaPink,
    onTertiary = Color.White,
    background = MayaBgDark,
    onBackground = TextPrimary,
    surface = MayaSurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = MayaSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = MayaSurfaceBorder,
    error = MayaError,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    // Maya Assistant uses custom futuristic midnight cyberpunk scheme
    MaterialTheme(
        colorScheme = MayaDarkColorScheme,
        typography = Typography,
        content = content
    )
}

package com.example.switchstream.ui.theme

import androidx.compose.runtime.Composable
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

private val DarkColorScheme = darkColorScheme(
    surface = SurfaceBlack,
    onSurface = TextPrimary,
    background = PureBlack,
    onBackground = TextPrimary,
    primary = AccentBlue,
    onPrimary = PureWhite,
    secondary = SurfaceVariant,
    onSecondary = TextPrimary,
    tertiary = AccentBlue,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondary,
    error = ErrorRed,
    onError = PureWhite
)

@Composable
fun SwitchStreamTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}

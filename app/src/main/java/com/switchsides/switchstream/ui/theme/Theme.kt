package com.switchsides.switchstream.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val DarkColorScheme = androidx.tv.material3.darkColorScheme(
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

private val MobileDarkColorScheme = androidx.compose.material3.darkColorScheme(
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
    val dimensions = detectDimensions()
    CompositionLocalProvider(LocalDimensions provides dimensions) {
        if (dimensions.isTV) {
            androidx.tv.material3.MaterialTheme(
                colorScheme = DarkColorScheme,
                typography = Typography,
                content = content
            )
        } else {
            // Standard material3 theme for phones/tablets
            // TV material3 components still work — they just need a MaterialTheme in scope
            androidx.compose.material3.MaterialTheme(
                colorScheme = MobileDarkColorScheme,
                typography = MobileTypography
            ) {
                // Also provide TV MaterialTheme so tv.material3 components don't crash
                androidx.tv.material3.MaterialTheme(
                    colorScheme = DarkColorScheme,
                    typography = Typography,
                    content = content
                )
            }
        }
    }
}

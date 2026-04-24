package com.switchsides.switchstream.ui.theme

import androidx.compose.ui.graphics.Color

// Streaming Cinema Palette
val SurfaceBlack = Color(0xFF0A0A0A)
val SurfaceElevated = Color(0xFF161616)
val SurfaceVariant = Color(0xFF1C1C1E)
val SurfaceFocus = Color(0xFF2A2A2E)
val TextPrimary = Color(0xFFF5F5F7)
val TextSecondary = Color(0xFFA1A1A6)
val TextTertiary = Color(0xFF636366)
// Editorial vermillion — the one brand accent. Slightly desaturated so it reads
// confidently on black without vibrating the way a pure red would.
val AccentRed = Color(0xFFE63946)
val AccentSubtle = Color(0x26E63946)  // 15% opacity brand red

// Back-compat alias. Call sites still read "AccentBlue" but the color is red now.
val AccentBlue = AccentRed

// Deeper burgundy kept for secondary accents (e.g., destructive button tint).
val AccentBurgundy = Color(0xFF8B1A1A)
val ErrorRed = Color(0xFFFF453A)
val SuccessGreen = Color(0xFF30D158)
val Divider = Color(0xFF2C2C2E)
val OverlayBlack = Color(0xB3000000)  // 70% black
val PureWhite = Color(0xFFFFFFFF)
val PureBlack = Color(0xFF000000)

// Glassmorphism system
val GlassSurface = Color(0xFF1C1C1E).copy(alpha = 0.65f)
val GlassSurfaceLight = Color(0xFF2A2A2E).copy(alpha = 0.55f)
val GlassBorder = Color(0xFFFFFFFF).copy(alpha = 0.12f)
val GlassBorderFocus = Color(0xFFFFFFFF).copy(alpha = 0.35f)
val GlassGlow = AccentRed.copy(alpha = 0.15f)

// Legacy aliases — kept so any stray references compile but map to new palette
val BurgundyPrimary = SurfaceElevated
val BurgundyDark = SurfaceBlack
val BurgundyMedium = SurfaceVariant
val BurgundyLight = SurfaceFocus
val Newsprint = TextPrimary
val NewsprintDim = TextSecondary
val InkGray = TextTertiary
val RuleGray = Divider
val GoldAccent = AccentRed

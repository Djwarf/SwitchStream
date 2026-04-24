package com.switchsides.switchstream.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Typography

// Editorial serif for content titles, sans for UI chrome.
// Letter-spacing is intentionally tight on display / headline sizes (closer to print
// typography) and gently open on body / labels for 10-foot legibility.

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = EditorialSerifFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 72.sp,
        lineHeight = 80.sp,
        letterSpacing = (-0.8).sp
    ),
    displayMedium = TextStyle(
        fontFamily = EditorialSerifFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 56.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.5).sp
    ),
    displaySmall = TextStyle(
        fontFamily = EditorialSerifFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 44.sp,
        lineHeight = 52.sp,
        letterSpacing = (-0.3).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = EditorialSerifFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.2).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = EditorialSerifFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = EditorialSerifFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 30.sp
    ),
    titleLarge = TextStyle(
        fontFamily = StreamingSansFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily = StreamingSansFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    titleSmall = TextStyle(
        fontFamily = StreamingSansFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = StreamingSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 28.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = StreamingSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodySmall = TextStyle(
        fontFamily = StreamingSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontFamily = StreamingSansFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.4.sp
    ),
    labelMedium = TextStyle(
        fontFamily = StreamingSansFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.3.sp
    ),
    labelSmall = TextStyle(
        fontFamily = StreamingSansFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.8.sp
    )
)

// Bespoke style for editorial row labels (SectionHeader). All-caps tracking, narrow
// enough to sit above a row without competing with artwork.
val EditorialRowLabel = TextStyle(
    fontFamily = StreamingSansFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 14.sp,
    lineHeight = 18.sp,
    letterSpacing = 2.4.sp
)

// Body copy tuned for long-form reading: wider leading (26sp on 16sp) and a hair of
// letter-spacing opens the line. Pair with `Modifier.widthIn(max = 640.dp)` at call
// sites to hold the line to an editorial measure (~65–72 characters).
val EditorialBody = TextStyle(
    fontFamily = StreamingSansFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 16.sp,
    lineHeight = 26.sp,
    letterSpacing = 0.1.sp
)

// Monospace style for S01E05, runtime, and timestamps — gives metadata a technical
// editorial rhythm without needing a custom font file.
val EditorialMono = TextStyle(
    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
    fontWeight = FontWeight.Medium,
    fontSize = 13.sp,
    lineHeight = 18.sp,
    letterSpacing = 0.5.sp
)

val MobileTypography = androidx.compose.material3.Typography(
    displayLarge = Typography.displayLarge,
    displayMedium = Typography.displayMedium,
    displaySmall = Typography.displaySmall,
    headlineLarge = Typography.headlineLarge,
    headlineMedium = Typography.headlineMedium,
    headlineSmall = Typography.headlineSmall,
    titleLarge = Typography.titleLarge,
    titleMedium = Typography.titleMedium,
    titleSmall = Typography.titleSmall,
    bodyLarge = Typography.bodyLarge,
    bodyMedium = Typography.bodyMedium,
    bodySmall = Typography.bodySmall,
    labelLarge = Typography.labelLarge,
    labelMedium = Typography.labelMedium,
    labelSmall = Typography.labelSmall
)

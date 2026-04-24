package com.switchsides.switchstream.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.switchsides.switchstream.R

// UI / body — system sans-serif (Roboto on Android TV). Neutral, highly legible.
val StreamingSansFamily = FontFamily.SansSerif

// Display / headlines — Crimson Text, a transitional serif in the Garamond family.
// Gives content titles an editorial/curatorial feel (think Criterion, Mubi) without
// fighting the system sans used for UI chrome.
val EditorialSerifFamily = FontFamily(
    Font(R.font.crimson_text_regular, FontWeight.Normal),
    Font(R.font.crimson_text_semibold, FontWeight.SemiBold),
    Font(R.font.crimson_text_bold, FontWeight.Bold),
    Font(R.font.crimson_text_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.crimson_text_bold_italic, FontWeight.Bold, FontStyle.Italic)
)

// Back-compat alias so any remaining `CrimsonTextFamily` call sites compile.
@Deprecated(
    message = "Use EditorialSerifFamily instead.",
    replaceWith = ReplaceWith("EditorialSerifFamily")
)
val CrimsonTextFamily = EditorialSerifFamily

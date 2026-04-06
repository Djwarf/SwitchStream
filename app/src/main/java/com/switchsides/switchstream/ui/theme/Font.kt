package com.switchsides.switchstream.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.switchsides.switchstream.R

// Primary font family — system sans-serif (Roboto on Android TV)
val StreamingSansFamily = FontFamily.SansSerif

// Legacy — kept so existing references don't break
val CrimsonTextFamily = FontFamily(
    Font(R.font.crimson_text_regular, FontWeight.Normal),
    Font(R.font.crimson_text_semibold, FontWeight.SemiBold),
    Font(R.font.crimson_text_bold, FontWeight.Bold),
    Font(R.font.crimson_text_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.crimson_text_bold_italic, FontWeight.Bold, FontStyle.Italic)
)

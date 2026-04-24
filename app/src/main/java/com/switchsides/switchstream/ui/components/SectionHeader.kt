package com.switchsides.switchstream.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.switchsides.switchstream.ui.theme.AccentBlue
import com.switchsides.switchstream.ui.theme.EditorialRowLabel
import com.switchsides.switchstream.ui.theme.LocalDimensions
import com.switchsides.switchstream.ui.theme.TextPrimary

/**
 * Editorial row header: a short accent bar followed by a wide-tracked, semibold label
 * in smallcaps styling. Uppercases the title to read as a section rubric (Criterion /
 * Mubi style) rather than a standalone heading.
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    val dims = LocalDimensions.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(
            horizontal = dims.screenPadding,
            vertical = if (dims.isTV) 20.dp else 12.dp
        )
    ) {
        // Accent bar — narrow, tall, colored. Reads as a margin note in an editorial layout.
        Spacer(
            modifier = Modifier
                .width(3.dp)
                .height(if (dims.isTV) 20.dp else 16.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(AccentBlue)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title.uppercase(),
            style = EditorialRowLabel,
            color = TextPrimary
        )
    }
}

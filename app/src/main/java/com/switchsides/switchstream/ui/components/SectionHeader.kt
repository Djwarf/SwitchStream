package com.switchsides.switchstream.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.switchsides.switchstream.ui.theme.LocalDimensions
import com.switchsides.switchstream.ui.theme.TextPrimary

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    val dims = LocalDimensions.current
    Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        color = TextPrimary,
        modifier = modifier.padding(horizontal = dims.screenPadding, vertical = if (dims.isTV) 20.dp else 12.dp)
    )
}

package com.switchsides.switchstream.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.switchsides.switchstream.ui.theme.AccentRed
import com.switchsides.switchstream.ui.theme.EditorialRowLabel
import com.switchsides.switchstream.ui.theme.TextSecondary
import com.switchsides.switchstream.ui.theme.TextTertiary

@Composable
fun EmptyState(
    message: String,
    modifier: Modifier = Modifier,
    eyebrow: String = "NOTHING YET"
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(48.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(24.dp)
                    .height(2.dp)
                    .background(AccentRed)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = eyebrow,
                style = EditorialRowLabel,
                color = AccentRed
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.headlineSmall,
                color = TextTertiary,
                textAlign = TextAlign.Center
            )
        }
    }
}

package com.example.switchstream.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.example.switchstream.ui.theme.GlassBorder
import com.example.switchstream.ui.theme.GlassBorderFocus
import com.example.switchstream.ui.theme.GlassSurface
import com.example.switchstream.ui.theme.PureBlack
import com.example.switchstream.ui.theme.PureWhite
import com.example.switchstream.ui.theme.TextPrimary

@Composable
fun FocusableButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = true,
    enabled: Boolean = true
) {
    var isFocused by remember { mutableStateOf(false) }

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.onFocusChanged { isFocused = it.isFocused },
        shape = ButtonDefaults.shape(shape = RoundedCornerShape(12.dp)),
        colors = if (isPrimary) {
            ButtonDefaults.colors(
                containerColor = PureWhite,
                contentColor = PureBlack,
                focusedContainerColor = PureWhite,
                focusedContentColor = PureBlack
            )
        } else {
            // Glass secondary button
            ButtonDefaults.colors(
                containerColor = GlassSurface,
                contentColor = TextPrimary,
                focusedContainerColor = PureWhite.copy(alpha = 0.2f),
                focusedContentColor = PureWhite
            )
        },
        border = if (!isPrimary) {
            ButtonDefaults.border(
                border = Border(
                    border = BorderStroke(1.dp, GlassBorder),
                    shape = RoundedCornerShape(12.dp)
                ),
                focusedBorder = Border(
                    border = BorderStroke(1.5.dp, GlassBorderFocus),
                    shape = RoundedCornerShape(12.dp)
                )
            )
        } else {
            ButtonDefaults.border()
        }
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
        )
    }
}

package com.switchsides.switchstream.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.switchsides.switchstream.ui.theme.GlassBorder
import com.switchsides.switchstream.ui.theme.GlassBorderFocus
import com.switchsides.switchstream.ui.theme.GlassSurface
import com.switchsides.switchstream.ui.theme.PureBlack
import com.switchsides.switchstream.ui.theme.PureWhite
import com.switchsides.switchstream.ui.theme.TextPrimary

@Composable
fun FocusableButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = true,
    enabled: Boolean = true,
    // When true (default), the inner label fills the button's width and centers —
    // needed for buttons explicitly given Modifier.fillMaxWidth() externally. When
    // false, the label is intrinsic and the button sizes to its content; use this
    // for buttons that live next to siblings in a wrap-content Row (e.g. the hero
    // "Info" button), where filling would balloon the button and squeeze neighbours.
    stretchContent: Boolean = true
) {
    val haptic = com.switchsides.switchstream.ui.util.rememberHaptic()
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1f,
        animationSpec = tween(200),
        label = "button_scale"
    )

    Button(
        onClick = { haptic(); onClick() },
        enabled = enabled,
        // .clickable handles touchscreen taps (TV Material3 Button's own onClick fires on
        // focus-click via d-pad, which doesn't engage on a touch device outside a TV theme).
        modifier = modifier
            .clickable { haptic(); onClick() }
            .onFocusChanged { isFocused = it.isFocused }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
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
        Box(
            modifier = Modifier
                .then(if (stretchContent) Modifier.fillMaxWidth() else Modifier)
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }
    }
}

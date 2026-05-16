package com.switchsides.switchstream.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Speed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.switchsides.switchstream.ui.theme.AccentRed
import com.switchsides.switchstream.ui.theme.EditorialMono
import com.switchsides.switchstream.ui.theme.EditorialRowLabel
import com.switchsides.switchstream.ui.theme.GlassBorder
import com.switchsides.switchstream.ui.theme.GlassSurface
import com.switchsides.switchstream.ui.theme.GlassSurfaceLight
import com.switchsides.switchstream.ui.theme.OverlayBlack
import com.switchsides.switchstream.ui.theme.PureWhite
import com.switchsides.switchstream.ui.theme.TextPrimary
import com.switchsides.switchstream.ui.theme.TextSecondary
import com.switchsides.switchstream.ui.util.autoFocusOnAppear

/**
 * Bottom-row "More" sheet for the player. Holds the controls that don't earn a
 * permanent slot in the chrome — speed, audio track, sleep timer, quality, aspect.
 * Each row reads as: icon · label · current value (mono). Tapping a row dismisses
 * the sheet and either opens an existing detail dialog or cycles state inline.
 */
@Composable
fun PlayerMoreSheet(
    audioTrackLabel: String,
    speedLabel: String,
    qualityLabel: String,
    sleepTimerLabel: String,
    aspectLabel: String,
    onAudioClick: () -> Unit,
    onSpeedClick: () -> Unit,
    onQualityClick: () -> Unit,
    onSleepTimerClick: () -> Unit,
    onAspectClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OverlayBlack)
            // pointerInput.detectTapGestures consumes touches without making the Box
            // focusable — important so d-pad navigation isn't pulled away from the
            // sheet's rows by the backdrop's clickable focus target.
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onDismiss() })
            }
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.Back) {
                    onDismiss()
                    true
                } else false
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(540.dp)
                // Cap height so the card never grows beyond the viewport on short screens.
                .heightIn(max = 560.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(GlassSurface)
                .border(1.dp, GlassBorder, RoundedCornerShape(18.dp))
                .padding(28.dp)
                // Same: swallow taps in the card without becoming a focus target.
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { /* swallow */ })
                }
                .verticalScroll(rememberScrollState())
                // focusGroup keeps d-pad focus search inside the sheet's rows so DOWN
                // moves to the next row instead of escaping to the player chrome below.
                .focusGroup()
        ) {
            // Editorial eyebrow + title
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height(2.dp)
                        .background(AccentRed)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "PLAYER OPTIONS",
                    style = EditorialRowLabel,
                    color = AccentRed
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "More",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(20.dp))

            MoreOptionRow(
                icon = Icons.Filled.Audiotrack,
                label = "Audio Track",
                value = audioTrackLabel,
                autoFocus = true,
                onClick = { onDismiss(); onAudioClick() }
            )
            MoreOptionRow(
                icon = Icons.Filled.Speed,
                label = "Playback Speed",
                value = speedLabel,
                onClick = { onDismiss(); onSpeedClick() }
            )
            MoreOptionRow(
                icon = Icons.Filled.HighQuality,
                label = "Streaming Quality",
                value = qualityLabel,
                onClick = { onDismiss(); onQualityClick() }
            )
            MoreOptionRow(
                icon = Icons.Filled.Bedtime,
                label = "Sleep Timer",
                value = sleepTimerLabel,
                onClick = { onDismiss(); onSleepTimerClick() }
            )
            MoreOptionRow(
                icon = Icons.Filled.AspectRatio,
                label = "Aspect Ratio",
                value = aspectLabel,
                onClick = onAspectClick   // cycles in place — keep sheet open
            )
        }
    }
}

@Composable
private fun MoreOptionRow(
    icon: ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit,
    autoFocus: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }
    // Critical modifier order: focusRequester (via autoFocusOnAppear) must sit BEFORE
    // the focusable node (Modifier.clickable). Otherwise the FocusRequester targets
    // nothing and the LaunchedEffect's requestFocus() silently no-ops — which is why
    // the sheet was opening with no row focused.
    val visualBase = Modifier
        .fillMaxWidth()
        .padding(vertical = 2.dp)
        .clip(RoundedCornerShape(10.dp))
        .background(if (isFocused) GlassSurfaceLight else androidx.compose.ui.graphics.Color.Transparent)
    val withFocusRequester = if (autoFocus) visualBase.then(Modifier.autoFocusOnAppear()) else visualBase
    Row(
        modifier = withFocusRequester
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.material3.Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isFocused) PureWhite else TextSecondary,
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 14.dp)
                .size(22.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isFocused) PureWhite else TextPrimary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = EditorialMono,
            color = if (isFocused) AccentRed else TextSecondary,
            modifier = Modifier.padding(end = 14.dp)
        )
    }
}
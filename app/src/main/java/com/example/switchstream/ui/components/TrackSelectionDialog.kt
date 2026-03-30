package com.example.switchstream.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Text
import com.example.switchstream.data.model.MediaTrackInfo
import com.example.switchstream.ui.theme.AccentBlue
import com.example.switchstream.ui.theme.GlassBorder
import com.example.switchstream.ui.theme.GlassSurface
import com.example.switchstream.ui.theme.GlassSurfaceLight
import com.example.switchstream.ui.theme.Divider
import com.example.switchstream.ui.theme.OverlayBlack
import com.example.switchstream.ui.theme.SurfaceElevated
import com.example.switchstream.ui.theme.SurfaceVariant
import com.example.switchstream.ui.theme.TextPrimary
import com.example.switchstream.ui.theme.TextSecondary

@Composable
fun TrackSelectionDialog(
    title: String,
    tracks: List<MediaTrackInfo>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
    showNoneOption: Boolean = false
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OverlayBlack)
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.Back) {
                    onDismiss()
                    true
                } else {
                    false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(500.dp)
                .background(
                    color = GlassSurface,
                    shape = RoundedCornerShape(16.dp)
                )
                .border(
                    width = 1.dp,
                    color = GlassBorder,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(24.dp)
        ) {
            // Title
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Separator
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Divider)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Track list
            LazyColumn {
                // None option
                if (showNoneOption) {
                    item {
                        val isSelected = selectedIndex == -1
                        TrackRow(
                            label = "None",
                            detail = null,
                            isSelected = isSelected,
                            onClick = { onSelect(-1) }
                        )
                    }
                }

                itemsIndexed(tracks) { index, track ->
                    val isSelected = index == selectedIndex
                    TrackRow(
                        label = track.title.ifEmpty { track.language ?: "Track ${index + 1}" },
                        detail = track.codec,
                        isSelected = isSelected,
                        onClick = { onSelect(index) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TrackRow(
    label: String,
    detail: String?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .onFocusChanged { isFocused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(8.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isFocused) GlassSurfaceLight else GlassSurface,
            focusedContainerColor = GlassSurfaceLight
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Selected checkmark
            if (isSelected) {
                androidx.tv.material3.Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Selected",
                    tint = AccentBlue,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected) AccentBlue else TextPrimary,
                modifier = Modifier.weight(1f)
            )

            if (detail != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = detail,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }
    }
}

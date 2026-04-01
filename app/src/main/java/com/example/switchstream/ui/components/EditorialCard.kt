package com.example.switchstream.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.compose.ui.graphics.vector.ImageVector
import coil.compose.AsyncImage
import com.example.switchstream.ui.theme.AccentBlue
import com.example.switchstream.ui.theme.GlassBorder
import com.example.switchstream.ui.theme.GlassBorderFocus
import com.example.switchstream.ui.theme.GlassSurface
import com.example.switchstream.ui.theme.GlassSurfaceLight
import com.example.switchstream.ui.theme.PureWhite
import com.example.switchstream.ui.theme.TextPrimary
import com.example.switchstream.ui.theme.TextSecondary

@Composable
fun EditorialCard(
    title: String,
    imageUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    progress: Float? = null,
    typeIcon: ImageVector? = null,
    cardWidth: Dp = 240.dp,
    imageHeight: Dp = 135.dp,
    badge: String? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    val haptic = com.example.switchstream.ui.util.rememberHaptic()

    Card(
        onClick = { haptic(); onClick() },
        modifier = modifier
            .width(cardWidth)
            .clickable { haptic(); onClick() }
            .onFocusChanged { isFocused = it.isFocused },
        shape = CardDefaults.shape(shape = RoundedCornerShape(16.dp)),
        colors = CardDefaults.colors(
            containerColor = GlassSurface,
            focusedContainerColor = GlassSurfaceLight
        ),
        border = CardDefaults.border(
            border = Border(
                border = BorderStroke(1.dp, GlassBorder),
                shape = RoundedCornerShape(16.dp)
            ),
            focusedBorder = Border(
                border = BorderStroke(2.dp, GlassBorderFocus),
                shape = RoundedCornerShape(16.dp)
            )
        ),
        scale = CardDefaults.scale(focusedScale = 1.08f)
    ) {
        Column {
            Box {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(imageHeight)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                    contentScale = ContentScale.Crop
                )
                // Type badge (top-left)
                if (typeIcon != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .background(GlassSurface, RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        androidx.tv.material3.Icon(
                            imageVector = typeIcon,
                            contentDescription = "Content type",
                            tint = TextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                // Badge (top-right) — "NEW" or unplayed count
                if (badge != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(AccentBlue, RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        androidx.tv.material3.Text(
                            text = badge,
                            style = androidx.tv.material3.MaterialTheme.typography.labelSmall,
                            color = PureWhite
                        )
                    }
                }
                // Progress bar overlay
                if (progress != null && progress > 0f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(PureWhite.copy(alpha = 0.15f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress.coerceIn(0f, 1f))
                                .height(3.dp)
                                .background(AccentBlue)
                        )
                    }
                }
            }
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isFocused) PureWhite else TextPrimary,
                    maxLines = if (isFocused) 2 else 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
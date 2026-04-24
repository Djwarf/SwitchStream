package com.switchsides.switchstream.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.switchsides.switchstream.ui.theme.AccentRed
import com.switchsides.switchstream.ui.theme.EditorialRowLabel
import com.switchsides.switchstream.ui.theme.LocalDimensions
import com.switchsides.switchstream.ui.theme.PureBlack
import com.switchsides.switchstream.ui.theme.PureWhite

/**
 * Film-leader style intro shown on top of the player while the first frame buffers.
 *
 * Composition timing:
 *   0ms   — overlay fades in (controlled by the caller's AnimatedVisibility)
 *   200ms — accent bar starts drawing from left
 *   300ms — title fades in and slides up
 *   700ms — overlay fade-out begins (caller's responsibility)
 *
 * This is a pure visual component — it doesn't control its own lifecycle. The caller
 * wraps it in AnimatedVisibility and flips the flag after ~900ms (or sooner if a
 * resume dialog or user interaction supersedes it).
 */
@Composable
fun NowPlayingIntro(
    title: String,
    backdropUrl: String?,
    modifier: Modifier = Modifier
) {
    val dims = LocalDimensions.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        if (!backdropUrl.isNullOrEmpty()) {
            AsyncImage(
                model = backdropUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 0.55f },
                contentScale = ContentScale.Crop
            )
        }

        // Dark gradient + grain — keeps title legible over any backdrop.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to PureBlack.copy(alpha = 0.35f),
                            0.5f to PureBlack.copy(alpha = 0.55f),
                            0.85f to PureBlack.copy(alpha = 0.88f),
                            1f to PureBlack
                        )
                    )
                )
                .filmGrain(alpha = 0.03f)
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(
                    start = dims.screenPadding,
                    end = dims.screenPadding,
                    bottom = if (dims.isTV) 96.dp else 48.dp
                )
        ) {
            // Animated eyebrow — accent bar draws in from left, then the label reads.
            var barArmed by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(200)
                barArmed = true
            }
            val barWidth by animateFloatAsState(
                targetValue = if (barArmed) 32f else 0f,
                animationSpec = tween(durationMillis = 500),
                label = "nowplaying_bar"
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(barWidth.dp)
                        .height(2.dp)
                        .background(AccentRed)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "NOW PLAYING",
                    style = EditorialRowLabel,
                    color = PureWhite.copy(alpha = 0.9f)
                )
            }

            Spacer(modifier = Modifier.height(if (dims.isTV) 16.dp else 10.dp))

            // Title — staggered reveal brings it in 300ms after the overlay appears.
            StaggeredReveal(delayMs = 300, durationMs = 520, startOffsetY = 12) {
                Text(
                    text = title,
                    style = if (dims.isTV) MaterialTheme.typography.displayMedium
                           else MaterialTheme.typography.headlineLarge,
                    color = PureWhite,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

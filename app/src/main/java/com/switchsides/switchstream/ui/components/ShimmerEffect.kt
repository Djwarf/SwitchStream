package com.switchsides.switchstream.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.switchsides.switchstream.ui.theme.AccentBlue
import com.switchsides.switchstream.ui.theme.GlassSurface
import com.switchsides.switchstream.ui.theme.GlassSurfaceLight

@Composable
private fun pulsingAlpha(): Float {
    val transition = rememberInfiniteTransition(label = "pulse")
    val alpha by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )
    return alpha
}

@Composable
private fun pulsingColor(): Color {
    val t = pulsingAlpha()
    val base = lerp(GlassSurface, GlassSurfaceLight, t)
    return lerp(base, AccentBlue.copy(alpha = 0.15f), t * 0.3f)
}

@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp
) {
    val color = pulsingColor()
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(color)
    )
}

@Composable
fun ShimmerHomeScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 24.dp)
    ) {
        // Hero skeleton — full bleed with radial glow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp)
        ) {
            SkeletonBox(
                modifier = Modifier.fillMaxSize(),
                cornerRadius = 0.dp
            )
            // Radial accent glow at center
            val t = pulsingAlpha()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                AccentBlue.copy(alpha = 0.06f * t),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        repeat(3) { i ->
            SkeletonSection(delayIndex = i)
            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun SkeletonSection(delayIndex: Int = 0) {
    Column {
        // Section title bar
        SkeletonBox(
            modifier = Modifier
                .padding(start = 56.dp)
                .width(120.dp + (delayIndex * 20).dp)
                .height(18.dp),
            cornerRadius = 6.dp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Card row
        LazyRow(
            contentPadding = PaddingValues(horizontal = 56.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            userScrollEnabled = false
        ) {
            items(6) {
                SkeletonCard()
            }
        }
    }
}

@Composable
private fun SkeletonCard() {
    Column {
        // Poster (2:3 ratio)
        SkeletonBox(
            modifier = Modifier
                .width(160.dp)
                .aspectRatio(2f / 3f)
        )
        Spacer(modifier = Modifier.height(10.dp))
        // Title
        SkeletonBox(
            modifier = Modifier
                .width(120.dp)
                .height(12.dp),
            cornerRadius = 4.dp
        )
        Spacer(modifier = Modifier.height(6.dp))
        // Subtitle
        SkeletonBox(
            modifier = Modifier
                .width(70.dp)
                .height(10.dp),
            cornerRadius = 4.dp
        )
    }
}

@Composable
fun ShimmerDetailScreen() {
    val dims = com.switchsides.switchstream.ui.theme.LocalDimensions.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = dims.topBarClearance + 16.dp)
    ) {
        // Compact header: poster + info skeleton
        Row(
            modifier = Modifier.padding(horizontal = dims.screenPadding),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Poster placeholder
            SkeletonBox(
                modifier = Modifier
                    .width(120.dp)
                    .height(180.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                // Title
                SkeletonBox(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(24.dp),
                    cornerRadius = 6.dp
                )
                Spacer(modifier = Modifier.height(10.dp))
                // Meta
                SkeletonBox(
                    modifier = Modifier
                        .width(160.dp)
                        .height(14.dp),
                    cornerRadius = 4.dp
                )
                Spacer(modifier = Modifier.height(20.dp))
                // Play button placeholder
                SkeletonBox(
                modifier = Modifier
                    .width(140.dp)
                    .height(48.dp)
            )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Overview lines
        Column(modifier = Modifier.padding(horizontal = dims.screenPadding)) {
            SkeletonBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp),
                cornerRadius = 4.dp
            )
            Spacer(modifier = Modifier.height(8.dp))
            SkeletonBox(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(12.dp),
                cornerRadius = 4.dp
            )
            Spacer(modifier = Modifier.height(8.dp))
            SkeletonBox(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(12.dp),
                cornerRadius = 4.dp
            )
        }
    }
}

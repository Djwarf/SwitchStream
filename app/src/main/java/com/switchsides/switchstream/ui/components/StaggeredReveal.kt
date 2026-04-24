package com.switchsides.switchstream.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Fade + small upward slide on first appearance. Fires once (keyed to `Unit`) so it
 * doesn't replay on scroll or recomposition. Use with a small per-row `delayMs`
 * (e.g. `80L * index`) to stagger a vertical list — hero first, then rows cascading
 * in like a magazine spread unfolding.
 */
@Composable
fun StaggeredReveal(
    delayMs: Long = 0,
    durationMs: Int = 480,
    startOffsetY: Int = 16,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (delayMs > 0) delay(delayMs)
        visible = true
    }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMs, easing = FastOutSlowInEasing),
        label = "reveal_alpha"
    )
    val yOffsetDp by animateFloatAsState(
        targetValue = if (visible) 0f else startOffsetY.toFloat(),
        animationSpec = tween(durationMs, easing = FastOutSlowInEasing),
        label = "reveal_y"
    )
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .graphicsLayer { this.alpha = alpha }
            .offset(y = yOffsetDp.dp)
    ) {
        content()
    }
}

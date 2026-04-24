package com.switchsides.switchstream.ui.util

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.compositionLocalOf

/**
 * Ambient access to Compose's shared-transition scopes so deeply nested composables
 * (cards inside lazy rows inside screens inside nav destinations) can opt into a
 * shared-element transition without every intermediate node having to forward the
 * scopes as parameters.
 *
 * NavGraph wraps the NavHost in a SharedTransitionLayout and, inside each
 * `composable(...)` block, provides both scopes. A callsite (e.g. EditorialCard,
 * DetailScreen's backdrop) reads `LocalSharedTransitionScope.current` +
 * `LocalAnimatedContentScope.current`; when both are non-null it applies a shared
 * modifier, otherwise it renders normally. No-op outside a SharedTransitionLayout.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

val LocalAnimatedContentScope = compositionLocalOf<AnimatedContentScope?> { null }

/** Stable key so the same item's card and Detail backdrop are paired. */
fun sharedItemKey(id: Any): String = "item-image-$id"

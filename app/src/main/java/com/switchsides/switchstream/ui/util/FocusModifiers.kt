package com.switchsides.switchstream.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester

/**
 * Claim focus on first composition. Wraps a remembered [FocusRequester] and the
 * attach/request pair in one modifier so call sites don't have to repeat the boilerplate.
 *
 * `runCatching` is intentional: Compose's `requestFocus()` throws if the node isn't
 * yet attached/measured, which can happen on fast-changing overlays.
 */
@Composable
fun Modifier.autoFocusOnAppear(enabled: Boolean = true): Modifier {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        if (enabled) runCatching { focusRequester.requestFocus() }
    }
    return this.focusRequester(focusRequester)
}

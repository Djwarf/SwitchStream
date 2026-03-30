package com.example.switchstream.ui.components

import androidx.compose.runtime.Composable
import org.jellyfin.sdk.model.api.BaseItemDto

@Composable
fun MainScaffold(
    currentRoute: String,
    libraries: List<BaseItemDto>,
    onNavigate: (String) -> Unit,
    content: @Composable () -> Unit
) {
    AppNavigationDrawer(
        currentRoute = currentRoute,
        libraries = libraries,
        onNavigate = onNavigate,
        content = content
    )
}

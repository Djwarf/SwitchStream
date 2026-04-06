package com.switchsides.switchstream.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.switchsides.switchstream.ui.theme.ErrorRed
import com.switchsides.switchstream.ui.theme.PureWhite
import com.switchsides.switchstream.ui.theme.SuccessGreen
import kotlinx.coroutines.delay

@Composable
fun ConnectivityBanner(
    isOnline: Boolean,
    modifier: Modifier = Modifier
) {
    var showReconnected by remember { mutableStateOf(false) }
    var wasOffline by remember { mutableStateOf(false) }

    LaunchedEffect(isOnline) {
        if (isOnline && wasOffline) {
            showReconnected = true
            delay(3000)
            showReconnected = false
        }
        wasOffline = !isOnline
    }

    // Offline banner
    AnimatedVisibility(
        visible = !isOnline,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(ErrorRed)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "You're offline",
                style = MaterialTheme.typography.labelMedium,
                color = PureWhite
            )
        }
    }

    // Reconnected banner
    AnimatedVisibility(
        visible = showReconnected,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SuccessGreen)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Back online",
                style = MaterialTheme.typography.labelMedium,
                color = PureWhite
            )
        }
    }
}

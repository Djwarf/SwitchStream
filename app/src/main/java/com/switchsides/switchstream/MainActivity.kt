package com.switchsides.switchstream

import android.app.PictureInPictureParams
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.switchsides.switchstream.data.SessionData
import com.switchsides.switchstream.ui.components.LoadingIndicator
import com.switchsides.switchstream.ui.navigation.NavGraph
import com.switchsides.switchstream.ui.navigation.Screen
import com.switchsides.switchstream.ui.theme.SwitchStreamTheme
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID

class MainActivity : ComponentActivity() {

    var isInPlayer: Boolean = false
    var pipEnabled: Boolean = false
    var pendingDeepLink: String? = null

    // Compose-observable PiP state. Flipped by onPictureInPictureModeChanged so overlays can react.
    val isInPipState: MutableState<Boolean> = mutableStateOf(false)

    // Current video aspect ratio (width/height) provided by PlayerScreen. Null → fall back to 16:9.
    var currentVideoAspect: Rational? = null

    fun enterPipIfEnabled(): Boolean {
        if (!isInPlayer || !pipEnabled) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val aspect = currentVideoAspect?.takeIf { it.numerator > 0 && it.denominator > 0 }
                ?: Rational(16, 9)
            // Android PiP clamps aspect ratio to [1:2.39, 2.39:1]. Coerce to stay inside.
            val safeAspect = clampAspect(aspect)
            enterPictureInPictureMode(
                PictureInPictureParams.Builder()
                    .setAspectRatio(safeAspect)
                    .build()
            )
            return true
        }
        return false
    }

    private fun clampAspect(r: Rational): Rational {
        val min = Rational(100, 239) // ~1:2.39
        val max = Rational(239, 100) // ~2.39:1
        return when {
            r.toFloat() < min.toFloat() -> min
            r.toFloat() > max.toFloat() -> max
            else -> r
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        enterPipIfEnabled()
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipState.value = isInPictureInPictureMode
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pendingDeepLink = parseDeepLink(intent)
    }

    private fun parseDeepLink(intent: Intent?): String? {
        val uri = intent?.data ?: return null
        if (uri.scheme != "switchstream") return null
        val pathSegments = uri.pathSegments
        if (pathSegments.isEmpty()) return null

        return when (uri.host) {
            "item" -> pathSegments.firstOrNull()?.let { "detail/$it" }
            "play" -> pathSegments.firstOrNull()?.let { "player/$it/DeepLink" }
            else -> {
                // Also handle switchstream://item/id format (host = item)
                when (pathSegments.firstOrNull()) {
                    "item" -> pathSegments.getOrNull(1)?.let { "detail/$it" }
                    "play" -> pathSegments.getOrNull(1)?.let { "player/$it/DeepLink" }
                    else -> null
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)

        val app = applicationContext as SwitchStreamApp
        var isReady = false

        // Parse deep link: switchstream://item/{id}, switchstream://play/{id}
        pendingDeepLink = parseDeepLink(intent)

        splash.setKeepOnScreenCondition { !isReady }

        setContent {
            SwitchStreamTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Global background image — always visible behind all screens
                    Image(
                        painter = painterResource(id = R.drawable.bg_gradient),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // App content on top
                    var startDestination by remember { mutableStateOf<String?>(null) }

                    LaunchedEffect(Unit) {
                        val session: SessionData? = app.container.sessionManager.session.firstOrNull()
                        if (session != null) {
                            app.container.setAuthenticated(
                                serverUrl = session.serverUrl,
                                accessToken = session.authToken,
                                userId = UUID.fromString(session.userId)
                            )
                            startDestination = Screen.Home.route
                        } else {
                            startDestination = Screen.Connect.route
                        }
                        isReady = true
                    }

                    if (startDestination != null) {
                        NavGraph(
                            startDestination = startDestination!!,
                            deepLinkRoute = pendingDeepLink
                        )
                    } else {
                        LoadingIndicator()
                    }
                }
            }
        }
    }
}
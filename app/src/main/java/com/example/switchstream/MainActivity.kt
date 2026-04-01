package com.example.switchstream

import android.app.PictureInPictureParams
import android.content.Intent
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.switchstream.data.SessionData
import com.example.switchstream.ui.components.LoadingIndicator
import com.example.switchstream.ui.navigation.NavGraph
import com.example.switchstream.ui.navigation.Screen
import com.example.switchstream.ui.theme.SwitchStreamTheme
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID

class MainActivity : ComponentActivity() {

    var isInPlayer: Boolean = false
    var pipEnabled: Boolean = false
    var pendingDeepLink: String? = null

    fun enterPipIfEnabled(): Boolean {
        if (!isInPlayer || !pipEnabled) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            enterPictureInPictureMode(
                PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .build()
            )
            return true
        }
        return false
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        enterPipIfEnabled()
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
package com.example.switchstream

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = applicationContext as SwitchStreamApp

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
                    }

                    if (startDestination != null) {
                        NavGraph(startDestination = startDestination!!)
                    } else {
                        LoadingIndicator()
                    }
                }
            }
        }
    }
}
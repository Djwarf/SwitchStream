package com.switchsides.switchstream.ui.screens.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.switchsides.switchstream.R
import com.switchsides.switchstream.ui.components.FocusableButton
import com.switchsides.switchstream.ui.components.LoadingIndicator
import com.switchsides.switchstream.ui.components.SwitchStreamTextField
import com.switchsides.switchstream.data.CachedUser
import com.switchsides.switchstream.ui.theme.AccentBlue
import com.switchsides.switchstream.ui.theme.ErrorRed
import com.switchsides.switchstream.ui.theme.GlassBorder
import com.switchsides.switchstream.ui.theme.GlassSurface
import com.switchsides.switchstream.ui.theme.GlassSurfaceLight
import com.switchsides.switchstream.ui.theme.TextPrimary
import com.switchsides.switchstream.ui.theme.TextSecondary
import com.switchsides.switchstream.ui.theme.TextTertiary

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoggedIn: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // Navigate when Quick Connect completes
    LaunchedEffect(uiState.quickConnectPolling) {
        // If we were polling and stopped, and there's no error, we're authenticated
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (uiState.isLoading) {
            LoadingIndicator()
        } else {
            // Glass form card
            Column(
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .padding(24.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(GlassSurface)
                    .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
                    .padding(32.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = uiState.serverName,
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Sign In",
                    style = MaterialTheme.typography.headlineLarge,
                    color = TextPrimary
                )

                // Cached users (quick switch)
                if (uiState.cachedUsers.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(20.dp))

                    uiState.cachedUsers.forEach { user ->
                        FocusableButton(
                            text = user.username,
                            onClick = { viewModel.switchToCachedUser(user, onLoggedIn) },
                            isPrimary = false,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "or sign in as a different user",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextTertiary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Tab selector
                if (uiState.quickConnectAvailable) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FocusableButton(
                            text = "Password",
                            onClick = { viewModel.setTab(LoginTab.PASSWORD) },
                            isPrimary = uiState.activeTab == LoginTab.PASSWORD,
                            modifier = Modifier.weight(1f)
                        )
                        FocusableButton(
                            text = "Quick Connect",
                            onClick = { viewModel.setTab(LoginTab.QUICK_CONNECT) },
                            isPrimary = uiState.activeTab == LoginTab.QUICK_CONNECT,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }

                when (uiState.activeTab) {
                    LoginTab.PASSWORD -> PasswordLoginContent(
                        uiState = uiState,
                        onUsernameChange = viewModel::updateUsername,
                        onPasswordChange = viewModel::updatePassword,
                        onLogin = { viewModel.login(onLoggedIn) }
                    )
                    LoginTab.QUICK_CONNECT -> QuickConnectContent(
                        uiState = uiState,
                        serverUrl = "",
                        onLoggedIn = onLoggedIn
                    )
                }

                if (uiState.error != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = uiState.error!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = ErrorRed
                    )
                }
            }
        }
    }
}

@Composable
private fun PasswordLoginContent(
    uiState: LoginUiState,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLogin: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        SwitchStreamTextField(
            value = uiState.username,
            onValueChange = onUsernameChange,
            label = "Username",
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )

        Spacer(modifier = Modifier.height(16.dp))

        SwitchStreamTextField(
            value = uiState.password,
            onValueChange = onPasswordChange,
            label = "Password",
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { onLogin() })
        )

        Spacer(modifier = Modifier.height(24.dp))

        FocusableButton(
            text = "Sign In",
            onClick = onLogin,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun QuickConnectContent(
    uiState: LoginUiState,
    serverUrl: String,
    onLoggedIn: () -> Unit
) {
    // Auto-navigate when authenticated via Quick Connect
    LaunchedEffect(uiState.quickConnectPolling) {
        if (!uiState.quickConnectPolling && uiState.quickConnectCode != null && uiState.error == null) {
            onLoggedIn()
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(16.dp)
    ) {
        if (uiState.quickConnectCode != null) {
            Text(
                text = "Your Quick Connect Code",
                style = MaterialTheme.typography.headlineSmall,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = uiState.quickConnectCode,
                style = MaterialTheme.typography.displaySmall,
                color = AccentBlue,
                letterSpacing = 8.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Enter this code in your Jellyfin dashboard\nunder Quick Connect to sign in",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            if (uiState.quickConnectPolling) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Waiting for authorization...",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextTertiary
                )
            }
        } else {
            LoadingIndicator()
        }
    }
}

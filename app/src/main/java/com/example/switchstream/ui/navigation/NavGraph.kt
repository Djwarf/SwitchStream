package com.example.switchstream.ui.navigation

import android.net.Uri
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.switchstream.MainActivity
import com.example.switchstream.SwitchStreamApp
import com.example.switchstream.ui.components.MainScaffold
import com.example.switchstream.ui.screens.connect.ConnectScreen
import com.example.switchstream.ui.screens.connect.ConnectViewModel
import com.example.switchstream.ui.screens.detail.DetailScreen
import com.example.switchstream.ui.screens.detail.DetailViewModel
import com.example.switchstream.ui.screens.favorites.FavoritesScreen
import com.example.switchstream.ui.screens.favorites.FavoritesViewModel
import com.example.switchstream.ui.screens.history.HistoryScreen
import com.example.switchstream.ui.screens.history.HistoryViewModel
import com.example.switchstream.ui.screens.home.HomeScreen
import com.example.switchstream.ui.screens.home.HomeViewModel
import com.example.switchstream.ui.screens.library.LibraryScreen
import com.example.switchstream.ui.screens.library.LibraryViewModel
import com.example.switchstream.ui.screens.login.LoginScreen
import com.example.switchstream.ui.screens.login.LoginViewModel
import com.example.switchstream.ui.screens.player.PlayerScreen
import com.example.switchstream.ui.screens.player.PlayerViewModel
import com.example.switchstream.ui.screens.search.SearchScreen
import com.example.switchstream.ui.screens.search.SearchViewModel
import com.example.switchstream.ui.screens.settings.SettingsScreen
import com.example.switchstream.ui.screens.settings.SettingsViewModel
import org.jellyfin.sdk.model.api.BaseItemDto
import java.util.UUID

@Composable
fun NavGraph(startDestination: String, deepLinkRoute: String? = null) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val app = context.applicationContext as SwitchStreamApp
    val container = app.container

    // Handle deep link after nav is ready
    if (deepLinkRoute != null && startDestination == Screen.Home.route) {
        androidx.compose.runtime.LaunchedEffect(deepLinkRoute) {
            navController.navigate(deepLinkRoute)
        }
    }

    val defaultEnter = fadeIn(androidx.compose.animation.core.tween(150)) +
            slideInHorizontally(androidx.compose.animation.core.tween(150)) { it / 4 }
    val defaultExit = fadeOut(androidx.compose.animation.core.tween(100)) +
            slideOutHorizontally(androidx.compose.animation.core.tween(100)) { -it / 4 }
    val defaultPopEnter = fadeIn(androidx.compose.animation.core.tween(150)) +
            slideInHorizontally(androidx.compose.animation.core.tween(150)) { -it / 4 }
    val defaultPopExit = fadeOut(androidx.compose.animation.core.tween(100)) +
            slideOutHorizontally(androidx.compose.animation.core.tween(100)) { it / 4 }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Auth screens (no drawer)
        composable(Screen.Connect.route) {
            val vm = viewModel {
                ConnectViewModel(container, container.sessionManager)
            }
            ConnectScreen(
                viewModel = vm,
                onConnected = {
                    val serverName = Uri.encode(vm.uiState.value.serverName ?: "Server")
                    navController.navigate(Screen.Login.createRoute(serverName)) {
                        popUpTo(Screen.Connect.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.Login.route,
            arguments = listOf(navArgument("serverName") { type = NavType.StringType })
        ) { backStackEntry ->
            val serverName = backStackEntry.arguments?.getString("serverName") ?: ""
            val vm = viewModel {
                LoginViewModel(container, container.sessionManager, serverName)
            }
            LoginScreen(
                viewModel = vm,
                onLoggedIn = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Main screens (with drawer)
        composable(
            Screen.Home.route,
            enterTransition = { defaultEnter },
            exitTransition = { defaultExit },
            popEnterTransition = { defaultPopEnter },
            popExitTransition = { defaultPopExit }
        ) {
            val vm = viewModel {
                HomeViewModel(
                    container.createLibraryRepository(),
                    container.createImageRepository(),
                    container.downloadRepository,
                    container.settingsManager,
                    isTV = context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_LEANBACK)
                )
            }
            val uiState by vm.uiState.collectAsState()

            DrawerWrappedScreen(
                currentRoute = Screen.Home.route,
                libraries = uiState.libraries,
                navController = navController,
                container = container
            ) {
                HomeScreen(
                    viewModel = vm,
                    onItemClick = { itemId ->
                        navController.navigate(Screen.Detail.createRoute(itemId))
                    },
                    onLibraryClick = { libraryId, libraryName ->
                        navController.navigate(
                            Screen.Library.createRoute(libraryId, Uri.encode(libraryName))
                        )
                    }
                )
            }
        }

        composable(
            route = Screen.Library.route,
            arguments = listOf(
                navArgument("libraryId") { type = NavType.StringType },
                navArgument("libraryName") { type = NavType.StringType }
            ),
            enterTransition = { defaultEnter },
            exitTransition = { defaultExit },
            popEnterTransition = { defaultPopEnter },
            popExitTransition = { defaultPopExit }
        ) { backStackEntry ->
            val libraryId = backStackEntry.arguments?.getString("libraryId") ?: ""
            val libraryName = backStackEntry.arguments?.getString("libraryName") ?: ""
            val vm = viewModel {
                LibraryViewModel(
                    container.createLibraryRepository(),
                    container.createImageRepository(),
                    UUID.fromString(libraryId),
                    libraryName
                )
            }
            DrawerWrappedScreen(
                currentRoute = Screen.Library.route,
                libraries = emptyList(),
                navController = navController,
                container = container
            ) {
                LibraryScreen(
                    viewModel = vm,
                    onItemClick = { itemId ->
                        navController.navigate(Screen.Detail.createRoute(itemId))
                    },
                    onGenreBrowse = { libId ->
                        navController.navigate(Screen.Genre.createRoute(libId, "genres"))
                    }
                )
            }
        }

        composable(
            route = Screen.Detail.route,
            arguments = listOf(navArgument("itemId") { type = NavType.StringType }),
            enterTransition = { defaultEnter },
            exitTransition = { defaultExit },
            popEnterTransition = { defaultPopEnter },
            popExitTransition = { defaultPopExit }
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getString("itemId") ?: ""
            val vm = viewModel {
                DetailViewModel(
                    libraryRepo = container.createLibraryRepository(),
                    imageRepo = container.createImageRepository(),
                    itemId = UUID.fromString(itemId),
                    downloadRepo = container.downloadRepository,
                    serverUrl = container.serverUrl,
                    accessToken = container.accessToken
                )
            }
            DrawerWrappedScreen(
                currentRoute = Screen.Detail.route,
                libraries = emptyList(),
                navController = navController,
                container = container
            ) {
                DetailScreen(
                    viewModel = vm,
                    onPlayClick = { id ->
                        val title = Uri.encode(vm.uiState.value.item?.name ?: "")
                        val seriesId = if (vm.uiState.value.isSeries) {
                            vm.uiState.value.item?.id?.toString() ?: ""
                        } else ""
                        navController.navigate(Screen.Player.createRoute(id, title, seriesId))
                    },
                    onPersonClick = { personId, personName ->
                        if (personId.isNotEmpty()) {
                            navController.navigate(
                                Screen.Person.createRoute(personId, Uri.encode(personName))
                            )
                        }
                    },
                    onGenreClick = { genre ->
                        navController.navigate(Screen.Search.createRoute(Uri.encode(genre)))
                    }
                )
            }
        }

        // Genre browsing
        composable(
            route = Screen.Genre.route,
            arguments = listOf(
                navArgument("libraryId") { type = NavType.StringType },
                navArgument("genreName") { type = NavType.StringType }
            ),
            enterTransition = { defaultEnter },
            exitTransition = { defaultExit },
            popEnterTransition = { defaultPopEnter },
            popExitTransition = { defaultPopExit }
        ) { backStackEntry ->
            val libraryId = backStackEntry.arguments?.getString("libraryId") ?: ""
            val vm = viewModel {
                com.example.switchstream.ui.screens.genre.GenreViewModel(
                    libraryRepo = container.createLibraryRepository(),
                    imageRepo = container.createImageRepository(),
                    libraryId = UUID.fromString(libraryId)
                )
            }
            com.example.switchstream.ui.screens.genre.GenreScreen(
                viewModel = vm,
                onItemClick = { itemId ->
                    navController.navigate(Screen.Detail.createRoute(itemId))
                }
            )
        }

        // Person filmography
        composable(
            route = Screen.Person.route,
            arguments = listOf(
                navArgument("personId") { type = NavType.StringType },
                navArgument("personName") { type = NavType.StringType }
            ),
            enterTransition = { defaultEnter },
            exitTransition = { defaultExit },
            popEnterTransition = { defaultPopEnter },
            popExitTransition = { defaultPopExit }
        ) { backStackEntry ->
            val personId = backStackEntry.arguments?.getString("personId") ?: ""
            val personName = backStackEntry.arguments?.getString("personName") ?: ""
            val vm = viewModel {
                com.example.switchstream.ui.screens.person.PersonViewModel(
                    libraryRepo = container.createLibraryRepository(),
                    imageRepo = container.createImageRepository(),
                    personId = UUID.fromString(personId),
                    personName = personName
                )
            }
            com.example.switchstream.ui.screens.person.PersonScreen(
                viewModel = vm,
                onItemClick = { itemId ->
                    navController.navigate(Screen.Detail.createRoute(itemId))
                }
            )
        }

        composable(
            route = Screen.Search.route,
            arguments = listOf(
                navArgument("query") {
                    type = NavType.StringType
                    defaultValue = ""
                    nullable = true
                }
            ),
            enterTransition = { defaultEnter },
            exitTransition = { defaultExit },
            popEnterTransition = { defaultPopEnter },
            popExitTransition = { defaultPopExit }
        ) { backStackEntry ->
            val initialQuery = backStackEntry.arguments?.getString("query") ?: ""
            val vm = viewModel {
                SearchViewModel(container.createLibraryRepository(), container.createImageRepository(), container.downloadRepository, container.networkMonitor)
            }
            androidx.compose.runtime.LaunchedEffect(initialQuery) {
                if (initialQuery.isNotEmpty()) {
                    vm.updateQuery(initialQuery)
                }
            }
            DrawerWrappedScreen(
                currentRoute = "search",
                libraries = emptyList(),
                navController = navController,
                container = container
            ) {
                SearchScreen(
                    viewModel = vm,
                    onItemClick = { itemId ->
                        navController.navigate(Screen.Detail.createRoute(itemId))
                    }
                )
            }
        }

        // Downloads
        composable(
            route = Screen.Downloads.route,
            enterTransition = { defaultEnter },
            exitTransition = { defaultExit },
            popEnterTransition = { defaultPopEnter },
            popExitTransition = { defaultPopExit }
        ) {
            val vm = viewModel {
                com.example.switchstream.ui.screens.downloads.DownloadsViewModel(
                    container.downloadRepository
                )
            }
            DrawerWrappedScreen(
                currentRoute = Screen.Downloads.route,
                libraries = emptyList(),
                navController = navController,
                container = container
            ) {
                com.example.switchstream.ui.screens.downloads.DownloadsScreen(
                    viewModel = vm,
                    onItemClick = { itemId ->
                        navController.navigate(Screen.Detail.createRoute(itemId))
                    }
                )
            }
        }

        composable(
            route = Screen.Settings.route,
            enterTransition = { defaultEnter },
            exitTransition = { defaultExit },
            popEnterTransition = { defaultPopEnter },
            popExitTransition = { defaultPopExit }
        ) {
            val vm = viewModel {
                SettingsViewModel(container.sessionManager, container.settingsManager, container)
            }
            DrawerWrappedScreen(
                currentRoute = Screen.Settings.route,
                libraries = emptyList(),
                navController = navController,
                container = container
            ) {
                SettingsScreen(
                    viewModel = vm,
                    onSwitchServer = {
                        navController.navigate(Screen.Connect.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onOfflineModeChanged = {
                        // Navigate to Home with fresh state
                        navController.navigate(Screen.Home.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }

        // Users screen (profile switcher)
        composable(
            route = Screen.Users.route,
            enterTransition = { fadeIn(androidx.compose.animation.core.tween(300)) },
            exitTransition = { fadeOut(androidx.compose.animation.core.tween(200)) },
            popEnterTransition = { fadeIn(androidx.compose.animation.core.tween(300)) },
            popExitTransition = { fadeOut(androidx.compose.animation.core.tween(200)) }
        ) {
            val scope = rememberCoroutineScope()
            val sessionData by container.sessionManager.session.collectAsState(initial = null)
            var cachedUsers by remember { mutableStateOf<List<com.example.switchstream.data.CachedUser>>(emptyList()) }

            // Load cached users
            androidx.compose.runtime.LaunchedEffect(Unit) {
                // Cache current user first (read session directly, not from compose state)
                val s = container.sessionManager.session.first()
                if (s != null) {
                    container.sessionManager.cacheUser(s.serverUrl, s.authToken, s.userId, s.username)
                }
                cachedUsers = container.sessionManager.getCachedUsers(container.serverUrl)
            }

            com.example.switchstream.ui.screens.users.UsersScreen(
                cachedUsers = cachedUsers,
                currentUserId = sessionData?.userId,
                serverName = sessionData?.serverName ?: "Server",
                onUserSelected = { user ->
                    scope.launch {
                        container.setAuthenticated(
                            user.serverUrl,
                            user.authToken,
                            java.util.UUID.fromString(user.userId)
                        )
                        container.sessionManager.saveSession(
                            serverUrl = user.serverUrl,
                            authToken = user.authToken,
                            userId = user.userId,
                            serverName = sessionData?.serverName ?: "",
                            username = user.username
                        )
                        navController.navigate(Screen.Home.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
                onAddUser = {
                    scope.launch {
                        val serverName = Uri.encode(sessionData?.serverName ?: "Server")
                        // Cache current session before going to login
                        val s = sessionData
                        if (s != null) {
                            container.sessionManager.cacheUser(s.serverUrl, s.authToken, s.userId, s.username)
                        }
                        container.sessionManager.clearSession()
                        navController.navigate(Screen.Login.createRoute(serverName)) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(
            route = Screen.History.route,
            enterTransition = { defaultEnter },
            exitTransition = { defaultExit },
            popEnterTransition = { defaultPopEnter },
            popExitTransition = { defaultPopExit }
        ) {
            val vm = viewModel {
                HistoryViewModel(container.createLibraryRepository(), container.createImageRepository())
            }
            DrawerWrappedScreen(
                currentRoute = Screen.History.route,
                libraries = emptyList(),
                navController = navController,
                container = container
            ) {
                HistoryScreen(
                    viewModel = vm,
                    onItemClick = { itemId ->
                        navController.navigate(Screen.Detail.createRoute(itemId))
                    }
                )
            }
        }

        composable(
            route = Screen.Favorites.route,
            enterTransition = { defaultEnter },
            exitTransition = { defaultExit },
            popEnterTransition = { defaultPopEnter },
            popExitTransition = { defaultPopExit }
        ) {
            val vm = viewModel {
                FavoritesViewModel(container.createLibraryRepository(), container.createImageRepository())
            }
            DrawerWrappedScreen(
                currentRoute = Screen.Favorites.route,
                libraries = emptyList(),
                navController = navController,
                container = container
            ) {
                FavoritesScreen(
                    viewModel = vm,
                    onItemClick = { itemId ->
                        navController.navigate(Screen.Detail.createRoute(itemId))
                    }
                )
            }
        }

        // Player (no drawer, cinematic transition)
        composable(
            route = Screen.Player.route,
            arguments = listOf(
                navArgument("itemId") { type = NavType.StringType },
                navArgument("title") { type = NavType.StringType },
                navArgument("seriesId") {
                    type = NavType.StringType
                    defaultValue = ""
                    nullable = true
                }
            ),
            enterTransition = { fadeIn(androidx.compose.animation.core.tween(400)) },
            exitTransition = { fadeOut(androidx.compose.animation.core.tween(300)) },
            popEnterTransition = { fadeIn(androidx.compose.animation.core.tween(300)) },
            popExitTransition = { fadeOut(androidx.compose.animation.core.tween(300)) }
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getString("itemId") ?: ""
            val title = backStackEntry.arguments?.getString("title") ?: ""
            val seriesIdStr = backStackEntry.arguments?.getString("seriesId")?.takeIf { it.isNotEmpty() }
            val vm = viewModel {
                PlayerViewModel(
                    context = context,
                    playbackRepo = container.createPlaybackRepository(),
                    libraryRepo = container.createLibraryRepository(),
                    settingsManager = container.settingsManager,
                    itemId = UUID.fromString(itemId),
                    seriesId = seriesIdStr?.let { UUID.fromString(it) },
                    title = title
                )
            }
            // Enable PiP when in player
            val activity = context as? MainActivity
            val pipSetting by container.settingsManager.settings.collectAsState(
                initial = com.example.switchstream.data.PlaybackSettings()
            )
            DisposableEffect(Unit) {
                activity?.isInPlayer = true
                onDispose { activity?.isInPlayer = false }
            }
            // Keep activity PiP flag in sync with setting
            androidx.compose.runtime.LaunchedEffect(pipSetting.pictureInPictureEnabled) {
                activity?.pipEnabled = pipSetting.pictureInPictureEnabled
            }

            vm.onPlayNextEpisode = { nextItemId ->
                navController.navigate(
                    Screen.Player.createRoute(
                        nextItemId.toString(),
                        Uri.encode(vm.uiState.value.nextEpisode?.name ?: ""),
                        seriesIdStr ?: ""
                    )
                ) {
                    popUpTo(Screen.Player.route) { inclusive = true }
                }
            }
            PlayerScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
private fun DrawerWrappedScreen(
    currentRoute: String,
    libraries: List<BaseItemDto>,
    navController: androidx.navigation.NavController,
    container: com.example.switchstream.di.AppContainer,
    content: @Composable () -> Unit
) {
    val sessionData by container.sessionManager.session.collectAsState(initial = null)
    val isOnline by container.networkMonitor.isOnline.collectAsState()

    MainScaffold(
        currentRoute = currentRoute,
        libraries = libraries,
        username = sessionData?.username ?: "",
        isOnline = isOnline,
        onNavigate = { route ->
            if (route != currentRoute) {
                navController.navigate(route) {
                    popUpTo(Screen.Home.route) { inclusive = false }
                    launchSingleTop = true
                }
            }
        },
        content = content
    )
}

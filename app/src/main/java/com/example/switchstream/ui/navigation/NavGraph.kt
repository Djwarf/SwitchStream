package com.example.switchstream.ui.navigation

import android.net.Uri
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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
fun NavGraph(startDestination: String) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val app = context.applicationContext as SwitchStreamApp
    val container = app.container

    val defaultEnter = fadeIn(androidx.compose.animation.core.tween(300)) +
            slideInHorizontally(androidx.compose.animation.core.tween(300)) { it / 4 }
    val defaultExit = fadeOut(androidx.compose.animation.core.tween(200)) +
            slideOutHorizontally(androidx.compose.animation.core.tween(200)) { -it / 4 }
    val defaultPopEnter = fadeIn(androidx.compose.animation.core.tween(300)) +
            slideInHorizontally(androidx.compose.animation.core.tween(300)) { -it / 4 }
    val defaultPopExit = fadeOut(androidx.compose.animation.core.tween(200)) +
            slideOutHorizontally(androidx.compose.animation.core.tween(200)) { it / 4 }

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
                HomeViewModel(container.createLibraryRepository(), container.createImageRepository())
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
                    container.createLibraryRepository(),
                    container.createImageRepository(),
                    UUID.fromString(itemId)
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
                    onPersonClick = { personName ->
                        navController.navigate(Screen.Search.createRoute(Uri.encode(personName)))
                    }
                )
            }
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
                SearchViewModel(container.createLibraryRepository(), container.createImageRepository())
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
                    onSignOut = {
                        navController.navigate(Screen.Connect.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onSwitchServer = {
                        navController.navigate(Screen.Connect.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
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
    MainScaffold(
        currentRoute = currentRoute,
        libraries = libraries,
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

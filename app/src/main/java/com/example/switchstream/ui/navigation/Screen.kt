package com.example.switchstream.ui.navigation

sealed class Screen(val route: String) {
    data object Connect : Screen("connect")
    data object Login : Screen("login/{serverName}") {
        fun createRoute(serverName: String) = "login/$serverName"
    }
    data object Home : Screen("home")
    data object Library : Screen("library/{libraryId}/{libraryName}") {
        fun createRoute(libraryId: String, libraryName: String) = "library/$libraryId/$libraryName"
    }
    data object Detail : Screen("detail/{itemId}") {
        fun createRoute(itemId: String) = "detail/$itemId"
    }
    data object Player : Screen("player/{itemId}/{title}?seriesId={seriesId}") {
        fun createRoute(itemId: String, title: String, seriesId: String = "") =
            if (seriesId.isNotEmpty()) "player/$itemId/$title?seriesId=$seriesId"
            else "player/$itemId/$title"
    }
    data object Search : Screen("search?query={query}") {
        fun createRoute(query: String = "") = if (query.isNotEmpty()) "search?query=$query" else "search"
    }
    data object Settings : Screen("settings")
    data object History : Screen("history")
    data object Favorites : Screen("favorites")
    data object Users : Screen("users")
    data object Person : Screen("person/{personId}/{personName}") {
        fun createRoute(personId: String, personName: String) = "person/$personId/$personName"
    }
}

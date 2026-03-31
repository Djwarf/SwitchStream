package com.example.switchstream.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SwitchAccount
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.tv.material3.DrawerValue
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.NavigationDrawer
import androidx.tv.material3.Text
import com.example.switchstream.ui.navigation.Screen
import com.example.switchstream.ui.theme.AccentBlue
import com.example.switchstream.ui.theme.GlassBorder
import com.example.switchstream.ui.theme.GlassSurface
import com.example.switchstream.ui.theme.GlassSurfaceLight
import com.example.switchstream.ui.theme.TextPrimary
import com.example.switchstream.ui.theme.TextSecondary
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.CollectionType

data class DrawerMenuItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
fun AppNavigationDrawer(
    currentRoute: String,
    libraries: List<BaseItemDto>,
    username: String = "",
    onNavigate: (String) -> Unit,
    content: @Composable () -> Unit
) {
    val contentFocusRequester = remember { FocusRequester() }

    // Push focus to content whenever the route changes, closing the drawer
    LaunchedEffect(currentRoute) {
        try {
            contentFocusRequester.requestFocus()
        } catch (_: Exception) { }
    }

    NavigationDrawer(
        drawerContent = { drawerValue ->
            DrawerContent(
                drawerValue = drawerValue,
                currentRoute = currentRoute,
                libraries = libraries,
                username = username,
                onNavigate = onNavigate
            )
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(contentFocusRequester)
                .focusable()
        ) {
            content()
        }
    }
}

@Composable
private fun DrawerContent(
    drawerValue: DrawerValue,
    currentRoute: String,
    libraries: List<BaseItemDto>,
    username: String,
    onNavigate: (String) -> Unit
) {
    val isOpen = drawerValue == DrawerValue.Open

    // Frosted glass sidebar — straight edge, no rounded corners
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(if (isOpen) 260.dp else 56.dp)
            .background(GlassSurface)
            .drawBehind {
                // Right edge border line
                drawLine(
                    color = GlassBorder,
                    start = Offset(size.width, 0f),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(vertical = 24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        if (isOpen) {
            Text(
                text = "SwitchStream",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        val topItems = listOf(
            DrawerMenuItem(Screen.Home.route, "Home", Icons.Outlined.Home),
            DrawerMenuItem("search", "Search", Icons.Outlined.Search)
        )

        topItems.forEach { item ->
            DrawerItem(
                item = item,
                isSelected = currentRoute == item.route,
                isOpen = isOpen,
                onClick = { onNavigate(item.route) }
            )
        }

        if (libraries.isNotEmpty() && isOpen) {
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(1.dp)
                    .background(GlassBorder)
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        libraries.forEach { library ->
            val route = Screen.Library.createRoute(
                library.id.toString(),
                library.name ?: "Library"
            )
            val icon = when (library.collectionType) {
                CollectionType.MOVIES -> Icons.Outlined.Movie
                CollectionType.TVSHOWS -> Icons.Outlined.Tv
                else -> Icons.Outlined.VideoLibrary
            }
            DrawerItem(
                item = DrawerMenuItem(
                    route = route,
                    label = library.name ?: "Library",
                    icon = icon
                ),
                isSelected = currentRoute.startsWith("library/${library.id}"),
                isOpen = isOpen,
                onClick = { onNavigate(route) }
            )
        }

        if (isOpen && libraries.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(1.dp)
                    .background(GlassBorder)
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        val bottomItems = listOf(
            DrawerMenuItem(Screen.History.route, "Watch History", Icons.Outlined.History),
            DrawerMenuItem(Screen.Favorites.route, "Favorites", Icons.Outlined.Favorite),
            DrawerMenuItem(Screen.Settings.route, "Settings", Icons.Outlined.Settings)
        )

        bottomItems.forEach { item ->
            DrawerItem(
                item = item,
                isSelected = currentRoute == item.route,
                isOpen = isOpen,
                onClick = { onNavigate(item.route) }
            )
        }

        // User profile icon — navigates to Users screen
        Spacer(modifier = Modifier.weight(1f))

        if (isOpen) {
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(1.dp)
                    .background(GlassBorder)
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        DrawerItem(
            item = DrawerMenuItem(Screen.Users.route, username.ifEmpty { "Profile" }, Icons.Outlined.Person),
            isSelected = currentRoute == Screen.Users.route,
            isOpen = isOpen,
            onClick = { onNavigate(Screen.Users.route) }
        )
    }
}

@Composable
private fun DrawerItem(
    item: DrawerMenuItem,
    isSelected: Boolean,
    isOpen: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    val backgroundColor = when {
        isFocused -> GlassSurfaceLight
        isSelected -> GlassSurfaceLight.copy(alpha = 0.3f)
        else -> Color.Transparent
    }

    val contentColor = when {
        isSelected -> AccentBlue
        isFocused -> TextPrimary
        else -> TextSecondary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .then(
                if (isFocused) {
                    Modifier.border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                } else Modifier
            )
            .then(
                if (isSelected) {
                    Modifier.drawBehind {
                        val indicatorWidth = 3.dp.toPx()
                        val indicatorHeight = 20.dp.toPx()
                        val yOffset = (size.height - indicatorHeight) / 2f
                        drawRoundRect(
                            color = AccentBlue,
                            topLeft = Offset(0f, yOffset),
                            size = Size(indicatorWidth, indicatorHeight),
                            cornerRadius = CornerRadius(2.dp.toPx())
                        )
                    }
                } else Modifier
            )
            .selectable(selected = isSelected, onClick = onClick)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.label,
            tint = contentColor,
            modifier = Modifier.size(22.dp)
        )
        if (isOpen) {
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = item.label,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor
            )
        }
    }
}
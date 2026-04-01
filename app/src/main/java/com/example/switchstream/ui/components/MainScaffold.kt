package com.example.switchstream.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tv
import org.jellyfin.sdk.model.api.CollectionType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.example.switchstream.ui.navigation.Screen
import com.example.switchstream.ui.theme.AccentBlue
import com.example.switchstream.ui.theme.AccentBurgundy
import com.example.switchstream.ui.theme.GlassBorder
import com.example.switchstream.ui.theme.GlassBorderFocus
import com.example.switchstream.ui.theme.PureBlack
import com.example.switchstream.ui.theme.PureWhite
import com.example.switchstream.ui.theme.LocalDimensions
import com.example.switchstream.ui.theme.TextPrimary
import com.example.switchstream.ui.theme.TextSecondary
import com.example.switchstream.ui.theme.TextTertiary
import org.jellyfin.sdk.model.api.BaseItemDto

private data class NavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
fun MainScaffold(
    currentRoute: String,
    libraries: List<BaseItemDto>,
    username: String = "",
    isOnline: Boolean = true,
    onNavigate: (String) -> Unit,
    content: @Composable () -> Unit
) {
    val dims = LocalDimensions.current

    if (dims.isTV) {
        // TV: Column layout — nav with refined dark background, D-pad traverses naturally
        Column(modifier = Modifier.fillMaxSize()) {
            ConnectivityBanner(isOnline = isOnline)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                com.example.switchstream.ui.theme.SurfaceBlack,
                                com.example.switchstream.ui.theme.SurfaceElevated
                            )
                        )
                    )
                    .drawBehind {
                        // Subtle bottom border line
                        drawLine(
                            color = GlassBorder,
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = 0.5f.dp.toPx()
                        )
                    }
                    .padding(top = 14.dp, bottom = 14.dp)
            ) {
                TopNavBar(
                    currentRoute = currentRoute,
                    libraries = libraries,
                    username = username,
                    onNavigate = onNavigate
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                content()
            }
        }
    } else {
        // Phone/Tablet: same floating top nav, adapted for touch
        Box(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize()) {
                content()
            }
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    PureBlack.copy(alpha = 0.9f),
                                    PureBlack.copy(alpha = 0.6f),
                                    Color.Transparent
                                )
                            )
                        )
                        .statusBarsPadding()
                        .padding(top = 6.dp, bottom = 16.dp)
                ) {
                    MobileTopNavBar(
                        currentRoute = currentRoute,
                        libraries = libraries,
                        onNavigate = onNavigate
                    )
                }
            }
        }
    }
}

@Composable
private fun TopNavBar(
    currentRoute: String,
    libraries: List<BaseItemDto>,
    username: String,
    onNavigate: (String) -> Unit,
) {
    val navItems = mutableListOf(
        NavItem(Screen.Home.route, "Home", Icons.Outlined.Home)
    )
    libraries.forEach { lib ->
        val route = Screen.Library.createRoute(lib.id.toString(), lib.name ?: "Library")
        val icon = when (lib.collectionType) {
            CollectionType.MOVIES -> Icons.Outlined.Movie
            CollectionType.TVSHOWS -> Icons.Outlined.Tv
            else -> return@forEach
        }
        navItems.add(NavItem(route, lib.name ?: "Library", icon))
    }
    navItems.addAll(listOf(
        NavItem("search", "Search", Icons.Outlined.Search),
        NavItem(Screen.Settings.route, "Settings", Icons.Outlined.Settings)
    ))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Brand
        Text(
            text = "SWITCH",
            style = MaterialTheme.typography.titleMedium,
            color = PureWhite,
            letterSpacing = 3.sp
        )
        Text(
            text = "STREAM",
            style = MaterialTheme.typography.titleMedium,
            color = AccentBurgundy,
            letterSpacing = 3.sp
        )

        Spacer(modifier = Modifier.width(40.dp))

        // Nav items
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f)
        ) {
            navItems.forEach { item ->
                val isSelected = currentRoute == item.route ||
                    (item.route == "search" && currentRoute.startsWith("search")) ||
                    (item.route.startsWith("library/") && currentRoute.startsWith("library/") &&
                        item.route.substringAfter("library/").substringBefore("/") ==
                        currentRoute.substringAfter("library/").substringBefore("/"))
                NavPill(
                    label = item.label,
                    icon = item.icon,
                    isSelected = isSelected,
                    onClick = { onNavigate(item.route) }
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Profile avatar
        ProfileButton(
            username = username,
            isSelected = currentRoute == Screen.Users.route,
            onClick = { onNavigate(Screen.Users.route) }
        )
    }
}

@Composable
private fun NavPill(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        if (isFocused) 1.05f else 1f,
        animationSpec = tween(150)
    )
    val bgColor by animateColorAsState(
        when {
            isSelected -> AccentBurgundy.copy(alpha = 0.2f)
            isFocused -> PureWhite.copy(alpha = 0.1f)
            else -> Color.Transparent
        },
        animationSpec = tween(200)
    )
    val borderColor by animateColorAsState(
        when {
            isFocused -> GlassBorderFocus
            isSelected -> AccentBurgundy.copy(alpha = 0.4f)
            else -> Color.Transparent
        },
        animationSpec = tween(200)
    )

    Surface(
        onClick = onClick,
        modifier = Modifier
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(24.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = bgColor,
            focusedContainerColor = PureWhite.copy(alpha = 0.12f)
        ),
        border = ClickableSurfaceDefaults.border(
            border = androidx.tv.material3.Border(
                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
                shape = RoundedCornerShape(24.dp)
            ),
            focusedBorder = androidx.tv.material3.Border(
                border = androidx.compose.foundation.BorderStroke(1.5.dp, GlassBorderFocus),
                shape = RoundedCornerShape(24.dp)
            )
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = when {
                    isSelected -> AccentBurgundy
                    isFocused -> PureWhite
                    else -> TextSecondary
                },
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = when {
                    isSelected -> PureWhite
                    isFocused -> PureWhite
                    else -> TextSecondary
                }
            )
        }
    }
}

@Composable
private fun ProfileButton(
    username: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val ringColor by animateColorAsState(
        when {
            isFocused -> AccentBurgundy
            isSelected -> AccentBurgundy.copy(alpha = 0.7f)
            else -> GlassBorder
        },
        animationSpec = tween(200)
    )
    val scale by animateFloatAsState(
        if (isFocused) 1.1f else 1f,
        animationSpec = tween(150)
    )

    Surface(
        onClick = onClick,
        modifier = Modifier
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = ClickableSurfaceDefaults.shape(shape = CircleShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .border(2.dp, ringColor, CircleShape)
                .background(
                    if (isSelected) AccentBurgundy.copy(alpha = 0.2f)
                    else PureWhite.copy(alpha = 0.06f)
                ),
            contentAlignment = Alignment.Center
        ) {
            // Show first letter of username, or person icon
            if (username.isNotEmpty()) {
                Text(
                    text = username.first().uppercase(),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isSelected || isFocused) AccentBurgundy else TextSecondary
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = "Profile",
                    tint = if (isSelected || isFocused) AccentBurgundy else TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun MobileTopNavBar(
    currentRoute: String,
    libraries: List<BaseItemDto>,
    onNavigate: (String) -> Unit
) {
    // Build items: Home, library tabs, Search, Settings, Profile
    val navItems = mutableListOf(
        NavItem(Screen.Home.route, "Home", Icons.Outlined.Home)
    )
    libraries.forEach { lib ->
        val route = Screen.Library.createRoute(lib.id.toString(), lib.name ?: "Library")
        val icon = when (lib.collectionType) {
            CollectionType.MOVIES -> Icons.Outlined.Movie
            CollectionType.TVSHOWS -> Icons.Outlined.Tv
            else -> return@forEach
        }
        navItems.add(NavItem(route, lib.name ?: "Library", icon))
    }
    navItems.add(NavItem("search", "Search", Icons.Outlined.Search))
    navItems.add(NavItem(Screen.Settings.route, "Settings", Icons.Outlined.Settings))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Brand — compact
        androidx.compose.material3.Text(
            text = "S",
            style = MaterialTheme.typography.titleMedium,
            color = PureWhite
        )
        androidx.compose.material3.Text(
            text = "S",
            style = MaterialTheme.typography.titleMedium,
            color = AccentBurgundy
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Scrollable pills
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f)
        ) {
            navItems.forEach { item ->
                val isSelected = currentRoute == item.route ||
                    (item.route == "search" && currentRoute.startsWith("search")) ||
                    (item.route.startsWith("library/") && currentRoute.startsWith("library/") &&
                        item.route.substringAfter("library/").substringBefore("/") ==
                        currentRoute.substringAfter("library/").substringBefore("/"))

                MobileNavPill(
                    label = item.label,
                    isSelected = isSelected,
                    onClick = { onNavigate(item.route) }
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Profile circle
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .border(
                    1.5.dp,
                    if (currentRoute == Screen.Users.route) AccentBurgundy else GlassBorder,
                    CircleShape
                )
                .background(PureWhite.copy(alpha = 0.06f))
                .clickable { onNavigate(Screen.Users.route) },
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = "Profile",
                tint = if (currentRoute == Screen.Users.route) AccentBurgundy else TextSecondary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun MobileNavPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        when {
            isSelected -> AccentBurgundy.copy(alpha = 0.2f)
            else -> Color.Transparent
        },
        animationSpec = tween(200)
    )
    val borderColor by animateColorAsState(
        when {
            isSelected -> AccentBurgundy.copy(alpha = 0.4f)
            else -> Color.Transparent
        },
        animationSpec = tween(200)
    )
    val textColor by animateColorAsState(
        if (isSelected) PureWhite else TextSecondary,
        animationSpec = tween(200)
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        androidx.compose.material3.Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            maxLines = 1
        )
    }
}
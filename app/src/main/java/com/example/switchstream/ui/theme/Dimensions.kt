package com.example.switchstream.ui.theme

import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class DeviceType { PHONE, TABLET, TV }

data class Dimensions(
    val deviceType: DeviceType = DeviceType.TV,
    val isTV: Boolean = true,
    val screenPadding: Dp = 56.dp,
    val heroHeight: Dp = 500.dp,
    val cardWidth: Dp = 240.dp,
    val cardImageRatio: Float = 2f / 3f,
    val episodeRowHeight: Dp = 130.dp,
    val episodeThumbWidth: Dp = 240.dp,
    val gridMinCellSize: Dp = 200.dp,
    val sectionSpacing: Dp = 32.dp,
    val backdropHeight: Dp = 450.dp,
    val controlIconSize: Dp = 22.dp,
    val playerPadding: Dp = 32.dp,
    val userTileSize: Dp = 140.dp,
    val navIconSize: Dp = 18.dp,
    val topBarClearance: Dp = 0.dp
)

val PhoneDimensions = Dimensions(
    deviceType = DeviceType.PHONE,
    isTV = false,
    screenPadding = 16.dp,
    heroHeight = 240.dp,
    cardWidth = 130.dp,
    cardImageRatio = 2f / 3f,
    episodeRowHeight = 100.dp,
    episodeThumbWidth = 160.dp,
    topBarClearance = 72.dp,
    gridMinCellSize = 120.dp,
    sectionSpacing = 16.dp,
    backdropHeight = 280.dp,
    controlIconSize = 28.dp,
    playerPadding = 16.dp,
    userTileSize = 110.dp,
    navIconSize = 24.dp
)

val TabletDimensions = Dimensions(
    deviceType = DeviceType.TABLET,
    isTV = false,
    screenPadding = 32.dp,
    heroHeight = 350.dp,
    cardWidth = 170.dp,
    cardImageRatio = 2f / 3f,
    episodeRowHeight = 115.dp,
    episodeThumbWidth = 200.dp,
    gridMinCellSize = 160.dp,
    sectionSpacing = 24.dp,
    backdropHeight = 360.dp,
    controlIconSize = 24.dp,
    playerPadding = 24.dp,
    userTileSize = 130.dp,
    navIconSize = 22.dp,
    topBarClearance = 72.dp
)

val TVDimensions = Dimensions()

val LocalDimensions = compositionLocalOf { TVDimensions }

@Composable
fun detectDeviceType(): DeviceType {
    val context = LocalContext.current
    val isLeanback = context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
    if (isLeanback) return DeviceType.TV

    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    return if (screenWidthDp < 600) DeviceType.PHONE else DeviceType.TABLET
}

@Composable
fun detectDimensions(): Dimensions {
    return when (detectDeviceType()) {
        DeviceType.PHONE -> PhoneDimensions
        DeviceType.TABLET -> TabletDimensions
        DeviceType.TV -> TVDimensions
    }
}

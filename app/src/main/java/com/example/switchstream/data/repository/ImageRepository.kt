package com.example.switchstream.data.repository

import java.util.UUID

class ImageRepository(private val serverUrl: String) {

    fun getPrimaryImageUrl(itemId: UUID, maxWidth: Int = 400): String {
        return "$serverUrl/Items/$itemId/Images/Primary?maxWidth=$maxWidth&quality=90"
    }

    fun getBackdropUrl(itemId: UUID, maxWidth: Int = 1920): String {
        return "$serverUrl/Items/$itemId/Images/Backdrop?maxWidth=$maxWidth&quality=80"
    }

    fun getLogoUrl(itemId: UUID, maxWidth: Int = 600): String {
        return "$serverUrl/Items/$itemId/Images/Logo?maxWidth=$maxWidth&quality=90"
    }

    fun getThumbUrl(itemId: UUID, maxWidth: Int = 600): String {
        return "$serverUrl/Items/$itemId/Images/Thumb?maxWidth=$maxWidth&quality=90"
    }
}

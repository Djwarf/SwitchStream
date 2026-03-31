package com.example.switchstream.data.repository

import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.genresApi
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.api.client.extensions.libraryApi
import org.jellyfin.sdk.api.client.extensions.tvShowsApi
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.api.client.extensions.suggestionsApi
import org.jellyfin.sdk.api.client.extensions.playStateApi
import org.jellyfin.sdk.api.client.extensions.userViewsApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.CollectionType
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemFilter
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.SortOrder
import java.util.UUID

class LibraryRepository(private val apiClient: ApiClient, private val userId: UUID) {

    suspend fun getLibraries(): Result<List<BaseItemDto>> = runCatching {
        val response = apiClient.userViewsApi.getUserViews(userId = userId)
        response.content.items.orEmpty().filter { lib ->
            lib.collectionType != CollectionType.MUSIC &&
            lib.collectionType != CollectionType.MUSICVIDEOS
        }
    }

    suspend fun getLatestMedia(parentId: UUID? = null, limit: Int = 16): Result<List<BaseItemDto>> = runCatching {
        val response = apiClient.userLibraryApi.getLatestMedia(
            userId = userId,
            parentId = parentId,
            limit = limit
        )
        response.content
    }

    suspend fun getResumeItems(limit: Int = 12): Result<List<BaseItemDto>> = runCatching {
        val response = apiClient.itemsApi.getItems(
            userId = userId,
            filters = listOf(ItemFilter.IS_RESUMABLE),
            sortBy = listOf(ItemSortBy.DATE_PLAYED),
            sortOrder = listOf(SortOrder.DESCENDING),
            limit = limit,
            recursive = true,
            includeItemTypes = listOf(BaseItemKind.MOVIE, BaseItemKind.EPISODE)
        )
        response.content.items.orEmpty()
    }

    suspend fun getItems(
        parentId: UUID,
        startIndex: Int = 0,
        limit: Int = 50,
        sortBy: ItemSortBy = ItemSortBy.SORT_NAME,
        sortOrder: SortOrder = SortOrder.ASCENDING,
        genres: List<String>? = null,
        filters: List<ItemFilter>? = null
    ): Result<Pair<List<BaseItemDto>, Int>> = runCatching {
        val response = apiClient.itemsApi.getItems(
            userId = userId,
            parentId = parentId,
            startIndex = startIndex,
            limit = limit,
            recursive = false,
            sortBy = listOf(sortBy),
            sortOrder = listOf(sortOrder),
            genres = genres,
            filters = filters
        )
        val content = response.content
        Pair(content.items.orEmpty(), content.totalRecordCount ?: 0)
    }

    suspend fun getItemsByPerson(personId: UUID, limit: Int = 50): Result<List<BaseItemDto>> = runCatching {
        val response = apiClient.itemsApi.getItems(
            userId = userId,
            personIds = listOf(personId),
            recursive = true,
            limit = limit,
            includeItemTypes = listOf(BaseItemKind.MOVIE, BaseItemKind.SERIES),
            sortBy = listOf(ItemSortBy.PRODUCTION_YEAR),
            sortOrder = listOf(SortOrder.DESCENDING)
        )
        response.content.items.orEmpty()
    }

    suspend fun getItemDetail(itemId: UUID): Result<BaseItemDto> = runCatching {
        val response = apiClient.userLibraryApi.getItem(
            userId = userId,
            itemId = itemId
        )
        response.content
    }

    // TV Show support
    suspend fun getSeasons(seriesId: UUID): Result<List<BaseItemDto>> = runCatching {
        val response = apiClient.tvShowsApi.getSeasons(
            seriesId = seriesId,
            userId = userId
        )
        response.content.items.orEmpty()
    }

    suspend fun getEpisodes(seriesId: UUID, seasonId: UUID): Result<List<BaseItemDto>> = runCatching {
        val response = apiClient.tvShowsApi.getEpisodes(
            seriesId = seriesId,
            seasonId = seasonId,
            userId = userId
        )
        response.content.items.orEmpty()
    }

    suspend fun getNextUp(seriesId: UUID? = null, limit: Int = 1): Result<List<BaseItemDto>> = runCatching {
        val response = apiClient.tvShowsApi.getNextUp(
            userId = userId,
            seriesId = seriesId,
            limit = limit
        )
        response.content.items.orEmpty()
    }

    // Recommendations from Jellyfin's suggestion engine
    suspend fun getSuggestions(limit: Int = 10): Result<List<BaseItemDto>> = runCatching {
        val response = apiClient.suggestionsApi.getSuggestions(
            userId = userId,
            mediaType = null,
            type = listOf(BaseItemKind.MOVIE, BaseItemKind.SERIES),
            startIndex = null,
            limit = limit,
            enableTotalRecordCount = false
        )
        response.content.items.orEmpty()
    }

    // Search
    suspend fun search(query: String, limit: Int = 30): Result<List<BaseItemDto>> = runCatching {
        val response = apiClient.itemsApi.getItems(
            userId = userId,
            searchTerm = query,
            recursive = true,
            limit = limit,
            includeItemTypes = listOf(BaseItemKind.MOVIE, BaseItemKind.SERIES)
        )
        response.content.items.orEmpty()
    }

    // Similar items
    suspend fun getSimilarItems(itemId: UUID, limit: Int = 12): Result<List<BaseItemDto>> = runCatching {
        val response = apiClient.libraryApi.getSimilarItems(
            itemId = itemId,
            userId = userId,
            limit = limit
        )
        response.content.items.orEmpty()
    }

    // Genres
    suspend fun getGenres(parentId: UUID): Result<List<BaseItemDto>> = runCatching {
        val response = apiClient.genresApi.getGenres(
            parentId = parentId,
            userId = userId
        )
        response.content.items.orEmpty()
    }

    // Watch history
    suspend fun getWatchedItems(startIndex: Int = 0, limit: Int = 50): Result<List<BaseItemDto>> = runCatching {
        val response = apiClient.itemsApi.getItems(
            userId = userId,
            filters = listOf(ItemFilter.IS_PLAYED),
            sortBy = listOf(ItemSortBy.DATE_PLAYED),
            sortOrder = listOf(SortOrder.DESCENDING),
            startIndex = startIndex,
            limit = limit,
            recursive = true,
            includeItemTypes = listOf(BaseItemKind.MOVIE, BaseItemKind.EPISODE)
        )
        response.content.items.orEmpty()
    }

    // Favorites
    suspend fun getFavorites(startIndex: Int = 0, limit: Int = 50): Result<List<BaseItemDto>> = runCatching {
        val response = apiClient.itemsApi.getItems(
            userId = userId,
            isFavorite = true,
            sortBy = listOf(ItemSortBy.SORT_NAME),
            sortOrder = listOf(SortOrder.ASCENDING),
            startIndex = startIndex,
            limit = limit,
            recursive = true,
            includeItemTypes = listOf(BaseItemKind.MOVIE, BaseItemKind.SERIES)
        )
        response.content.items.orEmpty()
    }

    // Toggle favorite
    suspend fun setFavorite(itemId: UUID, favorite: Boolean): Result<Unit> = runCatching {
        if (favorite) {
            apiClient.userLibraryApi.markFavoriteItem(userId = userId, itemId = itemId)
        } else {
            apiClient.userLibraryApi.unmarkFavoriteItem(userId = userId, itemId = itemId)
        }
        Unit
    }

    // Mark played / unplayed
    suspend fun markPlayed(itemId: UUID): Result<Unit> = runCatching {
        apiClient.playStateApi.markPlayedItem(userId = userId, itemId = itemId)
        Unit
    }

    suspend fun markUnplayed(itemId: UUID): Result<Unit> = runCatching {
        apiClient.playStateApi.markUnplayedItem(userId = userId, itemId = itemId)
        Unit
    }

    // Recently added across all libraries (movies and series only, no episodes)
    suspend fun getRecentlyAdded(limit: Int = 20): Result<List<BaseItemDto>> = runCatching {
        val response = apiClient.itemsApi.getItems(
            userId = userId,
            sortBy = listOf(ItemSortBy.DATE_CREATED),
            sortOrder = listOf(SortOrder.DESCENDING),
            limit = limit,
            recursive = true,
            includeItemTypes = listOf(BaseItemKind.MOVIE, BaseItemKind.SERIES)
        )
        response.content.items.orEmpty()
    }
}

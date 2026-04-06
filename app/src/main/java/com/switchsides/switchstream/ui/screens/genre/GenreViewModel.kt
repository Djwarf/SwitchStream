package com.switchsides.switchstream.ui.screens.genre

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.switchsides.switchstream.data.repository.ImageRepository
import com.switchsides.switchstream.data.repository.LibraryRepository
import com.switchsides.switchstream.util.isNetworkError
import org.jellyfin.sdk.model.api.BaseItemDto
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class GenreSection(
    val genreName: String,
    val items: List<BaseItemDto>
)

data class GenreUiState(
    val genres: List<String> = emptyList(),
    val sections: List<GenreSection> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

class GenreViewModel(
    private val libraryRepo: LibraryRepository,
    val imageRepo: ImageRepository,
    private val libraryId: UUID
) : ViewModel() {

    private val _uiState = MutableStateFlow(GenreUiState())
    val uiState: StateFlow<GenreUiState> = _uiState.asStateFlow()

    init {
        loadGenres()
    }

    private fun loadGenres() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            libraryRepo.getGenres(libraryId).fold(
                onSuccess = { genres ->
                    val genreNames = genres.map { it.name ?: "" }.filter { it.isNotEmpty() }
                    _uiState.value = _uiState.value.copy(genres = genreNames)

                    // Load items for top genres in parallel (limit to 8 swimlanes)
                    val sections = genreNames.take(8).map { genre ->
                        async {
                            val result = libraryRepo.getItems(
                                parentId = libraryId,
                                genres = listOf(genre),
                                limit = 12
                            )
                            val items = result.getOrNull()?.first ?: emptyList()
                            if (items.isNotEmpty()) GenreSection(genre, items) else null
                        }
                    }.mapNotNull { it.await() }

                    _uiState.value = _uiState.value.copy(
                        sections = sections,
                        isLoading = false
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = if (isNetworkError(e)) "You're offline" else "Failed to load: ${e.message}"
                    )
                }
            )
        }
    }
}

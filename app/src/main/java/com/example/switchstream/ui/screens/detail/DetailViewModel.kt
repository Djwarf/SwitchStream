package com.example.switchstream.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.switchstream.data.repository.ImageRepository
import com.example.switchstream.data.repository.LibraryRepository
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class DetailUiState(
    val item: BaseItemDto? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val isSeries: Boolean = false,
    val seasons: List<BaseItemDto> = emptyList(),
    val episodes: List<BaseItemDto> = emptyList(),
    val selectedSeasonIndex: Int = 0,
    val similarItems: List<BaseItemDto> = emptyList(),
    val nextUpEpisode: BaseItemDto? = null,
    val isFavorite: Boolean = false,
    val isPlayed: Boolean = false
)

class DetailViewModel(
    private val libraryRepo: LibraryRepository,
    val imageRepo: ImageRepository,
    private val itemId: UUID
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    init {
        loadDetail()
    }

    private fun loadDetail() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            libraryRepo.getItemDetail(itemId).fold(
                onSuccess = { item ->
                    val isSeries = item.type == BaseItemKind.SERIES
                    _uiState.value = _uiState.value.copy(
                        item = item,
                        isLoading = false,
                        isSeries = isSeries,
                        isFavorite = item.userData?.isFavorite == true,
                        isPlayed = item.userData?.played == true
                    )

                    if (isSeries) {
                        loadSeriesData(item.id)
                    } else {
                        loadSimilarItems(item.id)
                    }
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to load: ${e.message}"
                    )
                }
            )
        }
    }

    private fun loadSeriesData(seriesId: UUID) {
        viewModelScope.launch {
            // Fetch seasons
            libraryRepo.getSeasons(seriesId).onSuccess { seasons ->
                _uiState.value = _uiState.value.copy(seasons = seasons)

                // Load episodes for the first season
                if (seasons.isNotEmpty()) {
                    loadEpisodesForSeason(seriesId, seasons[0].id)
                }
            }

            // Fetch next up for this series
            libraryRepo.getNextUp(seriesId).onSuccess { nextUpList ->
                _uiState.value = _uiState.value.copy(
                    nextUpEpisode = nextUpList.firstOrNull()
                )
            }
        }
    }

    private fun loadEpisodesForSeason(seriesId: UUID, seasonId: UUID) {
        viewModelScope.launch {
            libraryRepo.getEpisodes(seriesId, seasonId).onSuccess { episodes ->
                _uiState.value = _uiState.value.copy(episodes = episodes)
            }
        }
    }

    private fun loadSimilarItems(itemId: UUID) {
        viewModelScope.launch {
            libraryRepo.getSimilarItems(itemId).onSuccess { similar ->
                _uiState.value = _uiState.value.copy(similarItems = similar)
            }
        }
    }

    fun selectSeason(index: Int) {
        val seasons = _uiState.value.seasons
        if (index < 0 || index >= seasons.size) return

        _uiState.value = _uiState.value.copy(
            selectedSeasonIndex = index,
            episodes = emptyList()
        )

        val item = _uiState.value.item ?: return
        loadEpisodesForSeason(item.id, seasons[index].id)
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            val current = _uiState.value.isFavorite
            libraryRepo.setFavorite(itemId, !current).onSuccess {
                _uiState.value = _uiState.value.copy(isFavorite = !current)
            }
        }
    }

    fun togglePlayed() {
        viewModelScope.launch {
            val current = _uiState.value.isPlayed
            val result = if (current) {
                libraryRepo.markUnplayed(itemId)
            } else {
                libraryRepo.markPlayed(itemId)
            }
            result.onSuccess {
                _uiState.value = _uiState.value.copy(isPlayed = !current)
            }
        }
    }

    fun refresh() {
        loadDetail()
    }
}

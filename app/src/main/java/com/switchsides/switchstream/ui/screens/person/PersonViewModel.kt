package com.switchsides.switchstream.ui.screens.person

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.switchsides.switchstream.data.repository.ImageRepository
import com.switchsides.switchstream.data.repository.LibraryRepository
import com.switchsides.switchstream.util.isNetworkError
import org.jellyfin.sdk.model.api.BaseItemDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class PersonUiState(
    val personName: String = "",
    val items: List<BaseItemDto> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

class PersonViewModel(
    private val libraryRepo: LibraryRepository,
    val imageRepo: ImageRepository,
    private val personId: UUID,
    personName: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(PersonUiState(personName = personName))
    val uiState: StateFlow<PersonUiState> = _uiState.asStateFlow()

    init {
        loadFilmography()
    }

    private fun loadFilmography() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            libraryRepo.getItemsByPerson(personId).fold(
                onSuccess = { items ->
                    _uiState.value = _uiState.value.copy(
                        items = items,
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

    fun refresh() {
        loadFilmography()
    }
}

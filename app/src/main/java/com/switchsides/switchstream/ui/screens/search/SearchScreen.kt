package com.switchsides.switchstream.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.switchsides.switchstream.ui.components.EditorialCard
import com.switchsides.switchstream.ui.components.EmptyState
import com.switchsides.switchstream.ui.components.LoadingIndicator
import com.switchsides.switchstream.ui.components.SectionHeader
import com.switchsides.switchstream.ui.components.SwitchStreamTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Tv
import com.switchsides.switchstream.ui.theme.LocalDimensions
import com.switchsides.switchstream.ui.theme.PureBlack
import com.switchsides.switchstream.ui.theme.TextSecondary
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onItemClick: (itemId: String) -> Unit
) {
    val dims = LocalDimensions.current
    val uiState by viewModel.uiState.collectAsState()
    val offlineResults by viewModel.offlineResults.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack.copy(alpha = 0.75f))
            .padding(top = dims.topBarClearance)
    ) {
        SectionHeader("Search")

        Spacer(modifier = Modifier.height(8.dp))

        SwitchStreamTextField(
            value = uiState.query,
            onValueChange = { viewModel.updateQuery(it) },
            label = "Search movies, shows...",
            modifier = Modifier.padding(horizontal = 48.dp)
        )

        val totalResults = uiState.results.size + offlineResults.size
        if (uiState.hasSearched && totalResults > 0) {
            Text(
                text = if (offlineResults.isNotEmpty()) "$totalResults downloaded" else "$totalResults results",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = dims.screenPadding, vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        when {
            uiState.isSearching -> {
                LoadingIndicator()
            }
            !uiState.hasSearched -> {
                EmptyState("Search for movies, shows, and more")
            }
            uiState.hasSearched && uiState.results.isEmpty() && offlineResults.isEmpty() -> {
                EmptyState("No results for \"${uiState.query}\"")
            }
            offlineResults.isNotEmpty() -> {
                // Offline search results
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = dims.gridMinCellSize),
                    contentPadding = PaddingValues(horizontal = dims.screenPadding, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(offlineResults, key = { it.itemId }) { download ->
                        EditorialCard(
                            title = download.title,
                            imageUrl = download.thumbnailUrl,
                            subtitle = download.seriesName ?: "Downloaded",
                            onClick = { onItemClick(download.itemId) }
                        )
                    }
                }
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = dims.gridMinCellSize),
                    contentPadding = PaddingValues(horizontal = dims.screenPadding, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.results, key = { it.id }) { item ->
                        val subtitle = buildSearchSubtitle(item)
                        val icon = when (item.type) {
                            BaseItemKind.MOVIE -> Icons.Outlined.Movie
                            BaseItemKind.SERIES -> Icons.Outlined.Tv
                            else -> null
                        }
                        EditorialCard(
                            title = item.name ?: "",
                            imageUrl = viewModel.imageRepo.getThumbUrl(item.id),
                            subtitle = subtitle,
                            typeIcon = icon,
                            onClick = { onItemClick(item.id.toString()) }
                        )
                    }
                }
            }
        }
    }
}

private fun buildSearchSubtitle(item: BaseItemDto): String? {
    val year = item.productionYear?.toString()
    return when (item.type) {
        BaseItemKind.MOVIE -> listOfNotNull("Movie", year).joinToString(" \u00b7 ")
        BaseItemKind.SERIES -> listOfNotNull("Series", year).joinToString(" \u00b7 ")
        else -> year
    }
}

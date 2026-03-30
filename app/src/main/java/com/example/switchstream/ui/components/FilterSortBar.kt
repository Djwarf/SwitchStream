package com.example.switchstream.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Text
import com.example.switchstream.ui.theme.AccentBlue
import com.example.switchstream.ui.theme.SurfaceFocus
import com.example.switchstream.ui.theme.SurfaceVariant
import com.example.switchstream.ui.theme.TextSecondary
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.SortOrder

enum class WatchedFilter {
    ALL, WATCHED, UNWATCHED
}

private val SORT_OPTIONS = listOf(
    ItemSortBy.SORT_NAME,
    ItemSortBy.PRODUCTION_YEAR,
    ItemSortBy.COMMUNITY_RATING,
    ItemSortBy.DATE_CREATED
)

private fun ItemSortBy.displayLabel(): String = when (this) {
    ItemSortBy.SORT_NAME -> "Name"
    ItemSortBy.PRODUCTION_YEAR -> "Year"
    ItemSortBy.COMMUNITY_RATING -> "Rating"
    ItemSortBy.DATE_CREATED -> "Date Added"
    else -> name
}

private fun WatchedFilter.displayLabel(): String = when (this) {
    WatchedFilter.ALL -> "All"
    WatchedFilter.WATCHED -> "Watched"
    WatchedFilter.UNWATCHED -> "Unwatched"
}

@Composable
fun FilterSortBar(
    sortBy: ItemSortBy,
    onSortChange: (ItemSortBy) -> Unit,
    sortOrder: SortOrder = SortOrder.ASCENDING,
    onSortOrderChange: (SortOrder) -> Unit = {},
    availableGenres: List<String>,
    selectedGenres: List<String>,
    onGenreToggle: (String) -> Unit,
    watchedFilter: WatchedFilter,
    onWatchedFilterChange: (WatchedFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Sort button - cycles through options
        FilterChip(
            label = "Sort: ${sortBy.displayLabel()}",
            isActive = false,
            onClick = {
                val currentIndex = SORT_OPTIONS.indexOf(sortBy)
                val nextIndex = (currentIndex + 1) % SORT_OPTIONS.size
                onSortChange(SORT_OPTIONS[nextIndex])
            }
        )

        // Sort order toggle (ascending / descending)
        SortOrderChip(
            sortOrder = sortOrder,
            onClick = {
                val newOrder = if (sortOrder == SortOrder.ASCENDING)
                    SortOrder.DESCENDING else SortOrder.ASCENDING
                onSortOrderChange(newOrder)
            }
        )

        // Watched filter - cycles through options
        FilterChip(
            label = watchedFilter.displayLabel(),
            isActive = watchedFilter != WatchedFilter.ALL,
            onClick = {
                val next = when (watchedFilter) {
                    WatchedFilter.ALL -> WatchedFilter.UNWATCHED
                    WatchedFilter.UNWATCHED -> WatchedFilter.WATCHED
                    WatchedFilter.WATCHED -> WatchedFilter.ALL
                }
                onWatchedFilterChange(next)
            }
        )

        Spacer(modifier = Modifier.width(4.dp))

        // Genre chips
        if (availableGenres.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(availableGenres) { genre ->
                    val isSelected = genre in selectedGenres
                    FilterChip(
                        label = genre,
                        isActive = isSelected,
                        onClick = { onGenreToggle(genre) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        modifier = modifier.onFocusChanged { isFocused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(20.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = SurfaceVariant,
            focusedContainerColor = SurfaceFocus
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(
                    width = if (isActive) 1.dp else 0.dp,
                    color = if (isActive) AccentBlue else SurfaceVariant
                ),
                shape = RoundedCornerShape(20.dp)
            ),
            focusedBorder = Border(
                border = BorderStroke(
                    width = if (isActive) 1.dp else 0.dp,
                    color = if (isActive) AccentBlue else SurfaceFocus
                ),
                shape = RoundedCornerShape(20.dp)
            )
        )
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isActive) AccentBlue else TextSecondary,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun SortOrderChip(
    sortOrder: SortOrder,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        modifier = modifier.onFocusChanged { isFocused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(20.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = SurfaceVariant,
            focusedContainerColor = SurfaceFocus
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(
                    width = 0.dp,
                    color = SurfaceVariant
                ),
                shape = RoundedCornerShape(20.dp)
            ),
            focusedBorder = Border(
                border = BorderStroke(
                    width = 0.dp,
                    color = SurfaceFocus
                ),
                shape = RoundedCornerShape(20.dp)
            )
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (sortOrder == SortOrder.ASCENDING)
                    Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                contentDescription = if (sortOrder == SortOrder.ASCENDING) "Ascending" else "Descending",
                tint = TextSecondary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = if (sortOrder == SortOrder.ASCENDING) "Asc" else "Desc",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}

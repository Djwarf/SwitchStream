package com.example.switchstream.ui.screens.genre

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.switchstream.ui.components.EditorialCard
import com.example.switchstream.ui.components.ErrorState
import com.example.switchstream.ui.components.SectionHeader
import com.example.switchstream.ui.components.ShimmerHomeScreen
import com.example.switchstream.ui.theme.LocalDimensions
import com.example.switchstream.ui.theme.PureBlack

@Composable
fun GenreScreen(
    viewModel: GenreViewModel,
    onItemClick: (itemId: String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val dims = LocalDimensions.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack.copy(alpha = 0.75f))
    ) {
        when {
            uiState.isLoading -> ShimmerHomeScreen()
            uiState.error != null -> ErrorState(
                message = uiState.error!!,
                onRetry = {}
            )
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = dims.topBarClearance + 16.dp,
                        bottom = 48.dp
                    )
                ) {
                    uiState.sections.forEach { section ->
                        item(key = "header_${section.genreName}") {
                            SectionHeader(title = section.genreName)
                        }
                        item(key = "row_${section.genreName}") {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = dims.screenPadding),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(section.items, key = { it.id }) { item ->
                                    EditorialCard(
                                        title = item.name ?: "",
                                        imageUrl = viewModel.imageRepo.getPrimaryImageUrl(item.id),
                                        onClick = { onItemClick(item.id.toString()) },
                                        subtitle = item.productionYear?.toString()
                                    )
                                }
                            }
                        }
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                    }
                }
            }
        }
    }
}

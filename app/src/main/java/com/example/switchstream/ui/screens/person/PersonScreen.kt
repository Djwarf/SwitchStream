package com.example.switchstream.ui.screens.person

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import com.example.switchstream.ui.components.EditorialCard
import com.example.switchstream.ui.components.EmptyState
import com.example.switchstream.ui.components.ErrorState
import com.example.switchstream.ui.components.ShimmerHomeScreen
import com.example.switchstream.ui.theme.LocalDimensions
import com.example.switchstream.ui.theme.PureBlack
import com.example.switchstream.ui.theme.TextPrimary
import com.example.switchstream.ui.theme.TextSecondary

@Composable
fun PersonScreen(
    viewModel: PersonViewModel,
    onItemClick: (itemId: String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack.copy(alpha = 0.75f))
    ) {
        when {
            uiState.isLoading -> ShimmerHomeScreen()
            uiState.error != null -> ErrorState(
                message = uiState.error!!,
                onRetry = viewModel::refresh
            )
            uiState.items.isEmpty() -> EmptyState("No movies or shows found")
            else -> PersonContent(
                uiState = uiState,
                imageRepo = viewModel.imageRepo,
                onItemClick = onItemClick
            )
        }
    }
}

@Composable
private fun PersonContent(
    uiState: PersonUiState,
    imageRepo: com.example.switchstream.data.repository.ImageRepository,
    onItemClick: (String) -> Unit
) {
    val dims = LocalDimensions.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = dims.topBarClearance + 32.dp)
    ) {
        Text(
            text = uiState.personName,
            style = MaterialTheme.typography.headlineLarge,
            color = TextPrimary,
            modifier = Modifier.padding(horizontal = dims.screenPadding)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "${uiState.items.size} title${if (uiState.items.size != 1) "s" else ""}",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = dims.screenPadding)
        )

        Spacer(modifier = Modifier.height(24.dp))

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = dims.gridMinCellSize),
            contentPadding = PaddingValues(horizontal = dims.screenPadding, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            items(uiState.items, key = { it.id }) { item ->
                EditorialCard(
                    title = item.name ?: "",
                    imageUrl = imageRepo.getPrimaryImageUrl(item.id),
                    onClick = { onItemClick(item.id.toString()) },
                    subtitle = item.productionYear?.toString()
                )
            }
        }
    }
}

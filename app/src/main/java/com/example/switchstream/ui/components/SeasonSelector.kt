package com.example.switchstream.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Text
import com.example.switchstream.ui.theme.AccentBlue
import com.example.switchstream.ui.theme.PureWhite
import com.example.switchstream.ui.theme.GlassSurface
import com.example.switchstream.ui.theme.GlassSurfaceLight
import com.example.switchstream.ui.theme.TextSecondary
import org.jellyfin.sdk.model.api.BaseItemDto

@Composable
fun SeasonSelector(
    seasons: List<BaseItemDto>,
    selectedIndex: Int,
    onSeasonSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        itemsIndexed(seasons) { index, season ->
            val isSelected = index == selectedIndex
            var isFocused by remember { mutableStateOf(false) }

            val containerColor = when {
                isSelected -> AccentBlue
                isFocused -> GlassSurfaceLight
                else -> GlassSurface
            }

            val textColor = when {
                isSelected -> PureWhite
                else -> TextSecondary
            }

            Surface(
                onClick = { onSeasonSelected(index) },
                modifier = Modifier
                    .onFocusChanged { isFocused = it.isFocused },
                shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(20.dp)),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = containerColor,
                    focusedContainerColor = if (isSelected) AccentBlue else GlassSurfaceLight
                )
            ) {
                Text(
                    text = season.name ?: "Season ${index + 1}",
                    style = MaterialTheme.typography.labelLarge,
                    color = textColor,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                )
            }
        }
    }
}

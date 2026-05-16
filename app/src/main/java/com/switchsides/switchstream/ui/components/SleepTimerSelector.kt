package com.switchsides.switchstream.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.switchsides.switchstream.ui.theme.AccentBlue
import com.switchsides.switchstream.ui.theme.Divider
import com.switchsides.switchstream.ui.theme.GlassBorder
import com.switchsides.switchstream.ui.theme.GlassSurface
import com.switchsides.switchstream.ui.theme.GlassSurfaceLight
import com.switchsides.switchstream.ui.theme.OverlayBlack
import com.switchsides.switchstream.ui.theme.TextPrimary
import com.switchsides.switchstream.ui.util.autoFocusOnAppear

sealed class SleepTimerChoice(val label: String) {
    object Off : SleepTimerChoice("Off")
    data class Minutes(val value: Int) : SleepTimerChoice("$value minutes")
    object EndOfEpisode : SleepTimerChoice("End of episode")
}

private val OPTIONS: List<SleepTimerChoice> = listOf(
    SleepTimerChoice.Off,
    SleepTimerChoice.Minutes(15),
    SleepTimerChoice.Minutes(30),
    SleepTimerChoice.Minutes(45),
    SleepTimerChoice.Minutes(60),
    SleepTimerChoice.EndOfEpisode
)

@Composable
fun SleepTimerSelector(
    activeMinutes: Int?,
    endOfEpisodeActive: Boolean,
    onSelect: (SleepTimerChoice) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OverlayBlack)
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.Back) {
                    onDismiss()
                    true
                } else {
                    false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(500.dp)
                .background(GlassSurface, RoundedCornerShape(16.dp))
                .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                .padding(24.dp)
        ) {
            Text(
                text = "Sleep Timer",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Divider)
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn {
                itemsIndexed(OPTIONS) { index, option ->
                    val isSelected = when (option) {
                        is SleepTimerChoice.Off ->
                            activeMinutes == null && !endOfEpisodeActive
                        is SleepTimerChoice.Minutes ->
                            activeMinutes == option.value && !endOfEpisodeActive
                        is SleepTimerChoice.EndOfEpisode ->
                            endOfEpisodeActive
                    }
                    var isFocused by remember { mutableStateOf(false) }

                    Surface(
                        onClick = { onSelect(option) },
                        modifier = (if (index == 0) Modifier.autoFocusOnAppear() else Modifier)
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .clickable { onSelect(option) }
                            .onFocusChanged { isFocused = it.isFocused },
                        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(8.dp)),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = if (isFocused) GlassSurfaceLight else GlassSurface,
                            focusedContainerColor = GlassSurfaceLight
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isSelected) {
                                androidx.compose.material3.Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Selected",
                                    tint = AccentBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = option.label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isSelected) AccentBlue else TextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

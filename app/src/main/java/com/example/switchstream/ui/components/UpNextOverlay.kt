package com.example.switchstream.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.example.switchstream.ui.theme.AccentBlue
import com.example.switchstream.ui.theme.GlassBorder
import com.example.switchstream.ui.theme.GlassSurface
import com.example.switchstream.ui.theme.Divider
import com.example.switchstream.ui.theme.SurfaceElevated
import com.example.switchstream.ui.theme.TextPrimary
import com.example.switchstream.ui.theme.TextSecondary
import org.jellyfin.sdk.model.api.BaseItemDto

@Composable
fun UpNextOverlay(
    nextEpisode: BaseItemDto,
    imageUrl: String?,
    countdown: Int,
    onPlayNow: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomEnd
    ) {
        Column(
            modifier = Modifier
                .width(400.dp)
                .padding(16.dp)
                .background(
                    color = GlassSurface,
                    shape = RoundedCornerShape(16.dp)
                )
                .border(
                    width = 1.dp,
                    color = GlassBorder,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(16.dp)
        ) {
            // "Up Next" label
            Text(
                text = "Up Next",
                style = MaterialTheme.typography.labelMedium,
                color = AccentBlue
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Thumbnail
            AsyncImage(
                model = imageUrl,
                contentDescription = nextEpisode.name,
                modifier = Modifier
                    .width(200.dp)
                    .height(112.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Episode number
            val episodeNumber = nextEpisode.indexNumber
            if (episodeNumber != null) {
                Text(
                    text = "E$episodeNumber",
                    style = MaterialTheme.typography.labelMedium,
                    color = AccentBlue
                )
            }

            // Title
            Text(
                text = nextEpisode.name ?: "",
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Countdown
            Text(
                text = "Playing in $countdown...",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FocusableButton(
                    text = "Play Now",
                    onClick = onPlayNow,
                    isPrimary = true
                )
                FocusableButton(
                    text = "Cancel",
                    onClick = onCancel,
                    isPrimary = false
                )
            }
        }
    }
}

package com.switchsides.switchstream.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text as TvText
import com.switchsides.switchstream.ui.theme.AccentBlue
import com.switchsides.switchstream.ui.theme.AccentRed
import com.switchsides.switchstream.ui.theme.EditorialRowLabel
import com.switchsides.switchstream.ui.theme.EditorialSerifFamily
import com.switchsides.switchstream.ui.theme.PureWhite

@Composable
private fun EditorialLoadingCard() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TvText(
                    text = "SWITCH",
                    style = MaterialTheme.typography.titleLarge,
                    color = PureWhite,
                    letterSpacing = 3.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                TvText(
                    text = "stream",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = EditorialSerifFamily,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = AccentBlue,
                    letterSpacing = 0.sp
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .width(32.dp)
                    .height(2.dp)
                    .background(AccentRed)
            )
            Spacer(modifier = Modifier.height(14.dp))
            TvText(
                text = "NOW LOADING",
                style = EditorialRowLabel,
                color = PureWhite.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun ShimmerHomeScreen() {
    EditorialLoadingCard()
}

@Composable
fun ShimmerDetailScreen() {
    EditorialLoadingCard()
}

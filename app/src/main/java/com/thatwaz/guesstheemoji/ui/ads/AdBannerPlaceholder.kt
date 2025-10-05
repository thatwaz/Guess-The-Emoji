package com.thatwaz.guesstheemoji.ui.ads

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp

@Composable
fun AdBannerPlaceholder(modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = cs.surface,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
        shape = RectangleShape
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Ad Banner",
                color = cs.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}


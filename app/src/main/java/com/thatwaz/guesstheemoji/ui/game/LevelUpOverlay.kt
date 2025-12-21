package com.thatwaz.guesstheemoji.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp


@Composable
fun LevelUpOverlay(
    visible: Boolean,
    level: Int,
    onContinue: () -> Unit,
    continueEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (!visible) return

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "LEVEL UP!",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Level $level",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
            Spacer(Modifier.height(18.dp))

            Button(
                onClick = onContinue,
                enabled = continueEnabled
            ) {
                Text("Continue")
            }
        }
    }
}


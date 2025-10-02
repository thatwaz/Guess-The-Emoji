package com.thatwaz.guesstheemoji.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(onPlay: () -> Unit, onSettings: () -> Unit) {
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,   // center vertically
            horizontalAlignment = Alignment.CenterHorizontally // center horizontally
        ) {
            // Emoji banner / theme row
            Text(
                text = "🎬🍔🎵🐯📺",
                style = MaterialTheme.typography.displaySmall
            )

            Spacer(Modifier.height(16.dp))

            // Title
            Text(
                "Guess the Emojis",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(32.dp))

            // Play button
            Button(
                onClick = onPlay,
                modifier = Modifier.fillMaxWidth(0.8f) // not full width, looks nicer
            ) {
                Text("▶ Play")
            }

            Spacer(Modifier.height(16.dp))

            // Settings button
            OutlinedButton(
                onClick = onSettings,
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Text("⚙ Settings")
            }
        }
    }
}

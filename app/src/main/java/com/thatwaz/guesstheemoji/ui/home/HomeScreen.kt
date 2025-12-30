package com.thatwaz.guesstheemoji.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.thatwaz.guesstheemoji.R

@Composable
fun HomeScreen(
    hasActiveRun: Boolean,
    onContinue: () -> Unit,
    onNewGame: () -> Unit,
    onScores: () -> Unit,
    onSettings: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val isDark = isSystemInDarkTheme()

    val bg = if (isDark) {
        Brush.verticalGradient(listOf(cs.surface, cs.background))
    } else {
        Brush.verticalGradient(listOf(cs.surfaceVariant.copy(alpha = 0.4f), cs.surface))
    }

    val frameGradient = if (isDark) {
        Brush.linearGradient(
            listOf(cs.primary.copy(alpha = 0.35f), cs.secondary.copy(alpha = 0.35f))
        )
    } else {
        Brush.linearGradient(
            listOf(
                cs.secondaryContainer.copy(alpha = 0.55f),
                cs.primaryContainer.copy(alpha = 0.55f)
            )
        )
    }

    Scaffold(contentWindowInsets = WindowInsets.safeDrawing) { inner ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .background(bg)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Guess The Emoji",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.SansSerif
                    ),
                    color = cs.primary,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.80f)
                        .aspectRatio(1f)
                        .graphicsLayer {
                            rotationZ = -4f
                            shadowElevation = 16f
                            shape = RoundedCornerShape(20.dp)
                            clip = true
                        }
                        .background(frameGradient, shape = RoundedCornerShape(20.dp))
                        .padding(8.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.feature_graphic2),
                        contentDescription = "Emoji banner",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    text = "Fun Emoji Brain Teaser Game",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = cs.secondary,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(28.dp))

                if (hasActiveRun) {
                    Button(
                        onClick = onContinue,
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("▶ Resume Game", style = MaterialTheme.typography.titleMedium)
                    }

                    Spacer(Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = onNewGame,
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🆕 New Game", style = MaterialTheme.typography.titleMedium)
                    }
                } else {
                    Button(
                        onClick = onNewGame,
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("▶ Play", style = MaterialTheme.typography.titleMedium)
                    }
                }

                Spacer(Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onSettings,
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("⚙ Settings", style = MaterialTheme.typography.titleMedium)
                }

                Spacer(Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onScores,
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("🏆 Scores", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}















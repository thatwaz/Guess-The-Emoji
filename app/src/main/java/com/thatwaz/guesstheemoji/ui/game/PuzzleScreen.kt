package com.thatwaz.guesstheemoji.ui.game

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PuzzleScreen(
    vm: GameViewModel,
    showInterstitial: () -> Unit,
    BannerAd: @Composable () -> Unit
) {
    val s by vm.ui.collectAsState()

    LaunchedEffect(s.solved) {
        if (s.solved && vm.shouldShowInterstitial(System.currentTimeMillis())) {
            showInterstitial()
            vm.onInterstitialShown(System.currentTimeMillis())
        }
    }

    // Whole page respects status/nav bars.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Card 1: PUZZLE (scrollable, weighted)
        Column(
            modifier = Modifier
                .weight(1f, fill = true)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(top = 12.dp),               // pull puzzle down from top
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Level ${s.level}", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(6.dp))
            Text(
                "Lives ${"❤️".repeat(s.livesLeft)} • Attempts ${s.attemptsLeft}/${com.thatwaz.guesstheemoji.domain.Rules.MAX_ATTEMPTS}",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(24.dp))
            Text(s.emojis, style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(16.dp))
            Text(spacedMask(s.masked), style = MaterialTheme.typography.headlineMedium)

            if (s.wrong.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Wrong: ${s.wrong.sorted().joinToString(" ").uppercase()}",
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(Modifier.height(24.dp))
        }

        // Card 2: KEYBOARD (fixed height area; never scrolls away)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Debug: set a faint background while tuning layout; set to Color.Transparent later if you want.
            // Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)))

            HangmanKeyboard(
                guessed = s.guessed,
                wrong = s.wrong,
                enabled = !(s.solved || (s.failed && s.attemptsLeft == 0))
            ) { vm.onLetterTap(it) }

            Spacer(Modifier.height(12.dp))

            when {
                s.solved -> Button(onClick = { vm.next() }, modifier = Modifier.fillMaxWidth()) { Text("Next") }
                s.failed && s.attemptsLeft == 0 -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Answer: ${s.answer}")
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { vm.next() }, modifier = Modifier.fillMaxWidth()) { Text("Next") }
                }
            }
        }

        // Card 3: AD (pinned to bottom, above gesture bar)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()  // keep above gesture nav
        ) { BannerAd() }
    }
}

private fun spacedMask(m: String) =
    m.map { if (it == '_') "_ " else "$it " }.joinToString("")




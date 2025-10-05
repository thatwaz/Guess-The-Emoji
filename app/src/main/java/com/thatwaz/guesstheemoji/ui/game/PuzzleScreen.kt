package com.thatwaz.guesstheemoji.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.thatwaz.guesstheemoji.data.Category
import com.thatwaz.guesstheemoji.domain.Rules

@Composable
fun PuzzleScreen(
    vm: GameViewModel,
    showInterstitial: () -> Unit,
    BannerAd: @Composable () -> Unit
) {
    val s by vm.ui.collectAsState()
    val cs = MaterialTheme.colorScheme
    val typo = MaterialTheme.typography

    // Interstitial after solve
    LaunchedEffect(s.solved) {
        if (s.solved && vm.shouldShowInterstitial(System.currentTimeMillis())) {
            showInterstitial()
            vm.onInterstitialShown(System.currentTimeMillis())
        }
    }

    // Haptics
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(s.wrong.size) { if (s.wrong.isNotEmpty()) haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
    LaunchedEffect(s.solved)      { if (s.solved)           haptic.performHapticFeedback(HapticFeedbackType.LongPress) }

    // Game Over dialog
    if (s.livesLeft <= 0) {
        GameOverDialog(
            levelReached = s.level,
            lastEmojis = s.emojis,
            lastAnswer = s.answer,
            onPlayAgain = { vm.next() }
        )
    }

    // ✅ Explicit themed background so dark mode applies visually
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .background(cs.background)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // HUD
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Lives ${"❤️".repeat(s.livesLeft.coerceAtLeast(0))}",
                style = typo.bodyLarge,
                color = cs.onBackground
            )
            Text(
                text = "Attempts ${s.attemptsLeft}/${Rules.MAX_ATTEMPTS}",
                style = typo.bodyLarge,
                color = cs.onBackground
            )
        }

        Spacer(Modifier.height(8.dp))

        // Centered puzzle area
        Box(
            modifier = Modifier
                .weight(1f, fill = true)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Level ${s.level}", style = typo.titleLarge, color = cs.onBackground)

                Spacer(Modifier.height(6.dp))
                Text(
                    s.category.label(),
                    style = typo.labelLarge,
                    color = cs.primary
                )

                Spacer(Modifier.height(22.dp))

                // Emoji line (flash on wrong)
                FlashColor(
                    triggerKey = s.wrong.size,
                    baseColor = cs.onSurface
                ) { current ->
                    Text(
                        s.emojis,
                        style = typo.displayLarge,
                        color = current,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(14.dp))

                // Masked answer (shake + flash)
                val size = maskedTextSizeFor(s.answer)
                FlashColor(
                    triggerKey = s.wrong.size,
                    baseColor = cs.onSurface
                ) { current ->
                    Shakable(triggerKey = s.wrong.size) {
                        Text(
                            spacedByWord(s.masked, s.answer),
                            fontSize = size,
                            lineHeight = (size.value * 1.2f).sp,
                            color = current,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                if (s.wrong.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Wrong: ${s.wrong.sorted().joinToString(" ").uppercase()}",
                        color = cs.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Keyboard + actions
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 👉 Make sure HangmanKeyboard uses cs.* colors internally (see note below)
            HangmanKeyboard(
                guessed = s.guessed,
                wrong = s.wrong,
                enabled = (s.livesLeft > 0) && !s.solved && !s.failed
            ) { vm.onLetterTap(it) }

            Spacer(Modifier.height(12.dp))
            when {
                s.solved -> Button(
                    onClick = { vm.next() },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Next") }

                s.failed && s.livesLeft > 0 -> Button(
                    onClick = { vm.next() },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Next") }
            }
        }

        // Ad slot
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) { BannerAd() }
    }
}


/* ====================== HUD ====================== */

//@Composable
//private fun TopHud(
//    lives: Int,
//    attemptsLeft: Int,
//    maxAttempts: Int
//) {
//    Row(
//        modifier = Modifier.fillMaxWidth(),
//        verticalAlignment = Alignment.CenterVertically,
//        horizontalArrangement = Arrangement.SpaceBetween
//    ) {
//        Text(
//            text = "Lives ${"❤️".repeat(lives.coerceAtLeast(0))}",
//            style = MaterialTheme.typography.bodyLarge
//        )
//        Text(
//            text = "Attempts $attemptsLeft/$maxAttempts",
//            style = MaterialTheme.typography.bodyLarge
//        )
//    }
//}

/* ====================== Helpers: wrapping & sizing ====================== */


// Keep letters of a word together; use a visible, break-friendly space between words.
// - NBSP (\u00A0) between letters prevents mid-word breaks.
// - EN SPACE (\u2002) between words: visible and preserved even at line starts.
private fun spacedByWord(mask: String, answer: String): String {
    val nbsp = '\u00A0'      // non-breaking space (between letters)
    val enSpace = '\u2002'   // en space (between words, visible, break-friendly)
    val out = StringBuilder()

    for (i in mask.indices) {
        val a = answer[i]
        val m = mask[i]
        if (a.isWhitespace()) {
            out.append(enSpace)              // visible word separator
        } else {
            if (i > 0 && !answer[i - 1].isWhitespace()) out.append(nbsp)
            out.append(m)
        }
    }
    return out.toString()
}

// Light auto-sizing for long phrases
@Composable
private fun maskedTextSizeFor(answer: String) = when {
    answer.length > 28 -> 18.sp
    answer.length > 22 -> 20.sp
    answer.length > 16 -> 22.sp
    else               -> 24.sp
}



/* ====================== Effects: Shake & Flash ====================== */

// Horizontal shake when triggerKey changes (e.g., wrong guess)
@Composable
private fun Shakable(
    triggerKey: Any,
    shakes: Int = 3,
    amplitude: Dp = 8.dp,
    durationMillis: Int = 300,
    content: @Composable () -> Unit
) {
    val px = with(LocalDensity.current) { amplitude.toPx() }
    val offsetX = remember { Animatable(0f) }

    LaunchedEffect(triggerKey) {
        val segments: List<Float> = (0 until shakes).flatMap { listOf(-px, px) } + listOf(0f)
        val step = (durationMillis / segments.size).coerceAtLeast(1)

        offsetX.animateTo(
            targetValue = 0f,
            animationSpec = keyframes {
                this.durationMillis = durationMillis
                var t = 0
                segments.forEach { v ->
                    v at t
                    t += step
                }
            }
        )
    }

    Box(modifier = Modifier.graphicsLayer { translationX = offsetX.value }) {
        content()
    }
}

// Brief red flash on trigger, then return to base color (float-based; wide-BOM safe)
@Composable
private fun FlashColor(
    triggerKey: Any,
    baseColor: Color,
    flashColor: Color = MaterialTheme.colorScheme.error,
    flashUpMs: Int = 90,
    flashDownMs: Int = 220,
    content: @Composable (Color) -> Unit
) {
    val t = remember { Animatable(0f) } // 0 = base, 1 = flash
    LaunchedEffect(baseColor) { t.snapTo(0f) }
    LaunchedEffect(triggerKey) {
        t.animateTo(1f, animationSpec = tween(flashUpMs))
        t.animateTo(0f, animationSpec = tween(flashDownMs))
    }
    val current = androidx.compose.ui.graphics.lerp(baseColor, flashColor, t.value)
    content(current)
}

/* ====================== Labels ====================== */

private fun Category.label(): String = when (this) {
    Category.MOVIES_TV      -> "Movies & TV"
    Category.FOOD_DRINK     -> "Food & Drink"
    Category.SONGS_MUSIC    -> "Songs & Music"
    Category.PHRASES_IDIOMS -> "Phrases & Idioms"
    Category.ANIMALS_NATURE -> "Animals & Nature"
}

/* ====================== Game Over Dialog ====================== */

@Composable
private fun GameOverDialog(
    levelReached: Int,
    lastEmojis: String,
    lastAnswer: String,
    onPlayAgain: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* keep explicit */ },
        title = { Text("Game Over") },
        text = {
            Column {
                Text("You ran out of lives.")
                Spacer(Modifier.height(8.dp))
                Text("Level reached: $levelReached")
                Spacer(Modifier.height(12.dp))
                // Show the last missed puzzle so it isn’t hidden behind the dialog
                Text(text = lastEmojis, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(6.dp))
                Text(text = "Answer: $lastAnswer")
            }
        },
        confirmButton = {
            TextButton(onClick = onPlayAgain) { Text("Play Again") }
        }
    )
}












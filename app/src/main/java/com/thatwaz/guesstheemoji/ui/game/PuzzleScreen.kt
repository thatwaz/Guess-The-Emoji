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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random


@Composable
fun PuzzleScreen(
    vm: GameViewModel,
    showInterstitial: (onDismiss: () -> Unit) -> Unit,
    bannerAd: @Composable () -> Unit,
    onShowScores: () -> Unit,
    onShowHome: () -> Unit
) {
    val s by vm.ui.collectAsState()
    val cs = MaterialTheme.colorScheme
    val typo = MaterialTheme.typography
    val scope = rememberCoroutineScope()


    // ✅ Level-up overlay state + simple "lock" so we can't double-trigger
    var showLevelUp by remember { mutableStateOf(false) }
    var continueLocked by remember { mutableStateOf(false) }

    // ✅ When tierUpPulse increments, open overlay and WAIT for user
    LaunchedEffect(s.tierUpPulse) {
        if (s.tierUpPulse > 0) {
            showLevelUp = true
            continueLocked = false
        }
    }


    // ✅ Handler: user taps Continue
    fun onContinueFromLevelUp() {
        if (continueLocked) return
        continueLocked = true

        val now = System.currentTimeMillis()
        val shouldAd = vm.shouldShowInterstitial(now)

        if (shouldAd) {
            var finished = false

            // ✅ SAFETY: if the ad never calls back, don't freeze
            scope.launch {
                delay(1500)
                if (!finished) {
                    vm.next()
                    showLevelUp = false
                    continueLocked = false
                }
            }

            // Show ad; ONLY after dismiss do we advance + mark shown
            showInterstitial {
                if (finished) return@showInterstitial
                finished = true

                vm.onInterstitialShown(System.currentTimeMillis())
                vm.next()
                showLevelUp = false
                continueLocked = false
            }
        } else {
            vm.next()
            showLevelUp = false
            continueLocked = false
        }
    }


    // Haptics
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(s.wrong.size) {
        if (s.wrong.isNotEmpty()) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
    LaunchedEffect(s.solved) {
        if (s.solved) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    // Game Over dialog
// Only show dialog if you WANT it and the run hasn't already triggered navigation
    if (s.livesLeft <= 0) {
        GameOverDialog(
            levelReached = s.tier,
            puzzleReached = s.puzzleNumber,
            lastEmojis = s.emojis,
            lastAnswer = s.answer,
            onPlayAgain = { vm.startNewRun() },
            onViewScores = { onShowScores() },
            onQuit = {
                // end run cleanly + go home
                vm.quitToHome()
                onShowHome()      // navigate to home
            }
        )
    }




    // ✅ Outer box: main UI + overlay on top
    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .background(cs.background)
    ) {
        // ✅ Main screen content (single column)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // ===================== HUD =====================
            Column(modifier = Modifier.fillMaxWidth()) {
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
                        text = "Score ${s.score}",
                        style = typo.bodyLarge,
                        color = cs.onBackground
                    )
                }

                Spacer(Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Attempts ${s.attemptsLeft}/${Rules.MAX_ATTEMPTS}",
                        style = typo.bodyMedium,
                        color = cs.onBackground
                    )
                    Text(
                        text = "Level ${s.tier} (${s.solvesInTier}/${Rules.LEVEL_UP_EVERY_SOLVES})",
                        style = typo.bodyMedium,
                        color = cs.primary
                    )
                }

                Spacer(Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = { s.solvesInTier / Rules.LEVEL_UP_EVERY_SOLVES.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = cs.primary,
                    trackColor = cs.surfaceVariant,
                )
            }

            Spacer(Modifier.height(8.dp))

            // ===================== CENTER PUZZLE AREA =====================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true) // ✅ keeps keyboard at bottom
            ) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Puzzle ${s.puzzleNumber}",
                        style = typo.bodySmall,
                        color = cs.onSurfaceVariant
                    )

                    Spacer(Modifier.height(10.dp))

                    // ✅ category label is back
                    Text(
                        text = s.category.label(),
                        style = typo.labelLarge,
                        color = cs.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(2.dp))

                    Text(
                        text = s.category.subtitleText(),
                        style = typo.bodyMedium,
                        color = cs.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(22.dp))

                    FlashColor(
                        triggerKey = s.wrong.size,
                        baseColor = cs.onSurface
                    ) { current ->
                        Text(
                            text = s.emojis,
                            style = typo.displayLarge,
                            color = current,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    val size = maskedTextSizeFor(s.answer)
                    FlashColor(
                        triggerKey = s.wrong.size,
                        baseColor = cs.onSurface
                    ) { current ->
                        Shakable(triggerKey = s.wrong.size) {
                            Text(
                                text = spacedByWord(s.masked, s.answer),
                                fontSize = size,
                                lineHeight = (size.value * 1.2f).sp,
                                color = current,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    SolvedCelebration(visible = s.solved)

                    if (s.wrong.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "Wrong: ${s.wrong.sorted().joinToString(" ").uppercase()}",
                            color = cs.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                ConfettiBurst(visible = s.solved)
            }

            // ===================== KEYBOARD + ACTIONS =====================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HangmanKeyboard(
                    guessed = s.guessed,
                    wrong = s.wrong,
                    enabled = (s.livesLeft > 0) && !s.solved && !s.failed && !showLevelUp
                ) { vm.onLetterTap(it) }

                Spacer(Modifier.height(12.dp))

                // ✅ When level-up overlay is showing, hide Next buttons (overlay controls flow)
                if (!showLevelUp) {
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
            }

            // ===================== BANNER AD =====================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) { bannerAd() }
        }

        // ✅ Overlay drawn above everything
        LevelUpOverlay(
            visible = showLevelUp,
            level = s.tier,
            onContinue = ::onContinueFromLevelUp,
            continueEnabled = !continueLocked
        )
    }
}




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

/* ====================== Tiny Solved Celebration ====================== */

@Composable
private fun SolvedCelebration(visible: Boolean) {
    if (!visible) return

    val scale = remember { Animatable(0.8f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(visible) {
        if (visible) {
            scale.snapTo(0.8f)
            alpha.snapTo(0f)
            scale.animateTo(1.05f, tween(durationMillis = 220))
            scale.animateTo(1.0f, tween(durationMillis = 120))
            alpha.animateTo(1f, tween(durationMillis = 220))
        } else {
            alpha.snapTo(0f)
        }
    }

    Box(
        modifier = Modifier
            .padding(top = 10.dp)
            .graphicsLayer {
                this.scaleX = scale.value
                this.scaleY = scale.value
                this.alpha = alpha.value
            }
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(999.dp)
            )
            .padding(horizontal = 16.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Nice! Puzzle solved 🎉",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

/* ====================== Labels ====================== */

private fun Category.label(): String = when (this) {
    Category.MOVIES_TV      -> "Movies & TV"
    Category.FOOD_DRINK     -> "Food & Drink"
    Category.SONGS_MUSIC    -> "Songs & Music"
    Category.PHRASES_IDIOMS -> "Phrases / Misc"
    Category.ANIMALS_NATURE -> "Animals & Nature"
    Category.CRAZY_COMBOS   -> "Crazy Nonsense Combos"
}

fun Category.subtitleText(): String = when (this) {
    Category.MOVIES_TV ->
        "Blockbusters, classics, and total guessers."
    Category.FOOD_DRINK ->
        "Tastes better when you solve it."
    Category.SONGS_MUSIC ->
        "Humming is allowed. Singing, optional."
    Category.PHRASES_IDIOMS ->
        "Sayings, stuff people say, and random things that fit nowhere else."
    Category.ANIMALS_NATURE ->
        "Nature-ish things… probably."
    Category.CRAZY_COMBOS ->
        "We don’t know either. Just roll with it."
}

/* ====================== Game Over Dialog ====================== */

@Composable
private fun GameOverDialog(
    levelReached: Int,
    puzzleReached: Int,
    lastEmojis: String,
    lastAnswer: String,
    onPlayAgain: () -> Unit,
    onViewScores: () -> Unit,
    onQuit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* explicit */ },
        title = { Text("Game Over") },
        text = {
            Column {
                Text("You ran out of lives.")
                Spacer(Modifier.height(8.dp))
                Text("Level reached: $levelReached")
                Spacer(Modifier.height(6.dp))
                Text("Puzzle reached: $puzzleReached")
                Spacer(Modifier.height(12.dp))
                Text(text = lastEmojis, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(6.dp))
                Text(text = "Answer: $lastAnswer")

                Spacer(Modifier.height(16.dp))

                // ✅ Tertiary action
                TextButton(
                    onClick = onQuit,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Quit to Home")
                }
            }
        },

        dismissButton = {
            TextButton(onClick = onViewScores) {
                Text("View Scores")
            }
        },

        confirmButton = {
            TextButton(onClick = onPlayAgain) {
                Text("Play Again")
            }
        }
    )
}

    /* ====================== Confetti Burst ====================== */

@Composable
private fun ConfettiBurst(visible: Boolean) {
    if (!visible) return

    // Create a bunch of particles once
    val particles = remember { List(40) { ConfettiParticle() } }

    Box(modifier = Modifier.fillMaxSize()) {
        particles.forEach { it.Draw() }
    }
}

private class ConfettiParticle {
    private val startX = Random.nextFloat()          // 0..1 across screen width
    private val duration = (1200..2200).random()     // ms
    private val sizeDp = (4..10).random().dp
    private val color = Color(
        Random.nextFloat(),
        Random.nextFloat(),
        Random.nextFloat(),
        1f
    )

    private val offsetY = Animatable(-0.2f)          // start slightly above view

    @Composable
    fun Draw() {
        val density = LocalDensity.current
        val sizePx = with(density) { sizeDp.toPx() }

        LaunchedEffect(Unit) {
            // Drop from top to bottom once per solve
            offsetY.snapTo(-0.2f)
            offsetY.animateTo(
                targetValue = 1.2f,
                animationSpec = tween(durationMillis = duration)
            )
        }

        Box(
            modifier = Modifier
                .graphicsLayer {
                    translationX = startX * 900f - sizePx / 2f
                    translationY = offsetY.value * 1600f - sizePx / 2f
                }
                .size(sizeDp)
                .background(color, RoundedCornerShape(2.dp))
        )
    }
}














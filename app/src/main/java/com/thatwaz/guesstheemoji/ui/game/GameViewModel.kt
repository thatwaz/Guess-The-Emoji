package com.thatwaz.guesstheemoji.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thatwaz.guesstheemoji.data.Category
import com.thatwaz.guesstheemoji.data.EmojiPuzzle
import com.thatwaz.guesstheemoji.data.EmojiPuzzles
import com.thatwaz.guesstheemoji.data.Keys
import com.thatwaz.guesstheemoji.data.Prefs
import com.thatwaz.guesstheemoji.domain.Rules
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch


data class GameState(
    val level: Int = 1,                      // puzzle number (keep as-is)
    val attemptsLeft: Int = Rules.MAX_ATTEMPTS,
    val livesLeft: Int = Rules.ARCADE_LIVES,
    val adsRemoved: Boolean = false,
    val solvesInLevel: Int = 0,     // 0..LEVEL_UP_EVERY_SOLVES-1
    val levelUpPulse: Int = 0,      // increments on level up
    val solvedSinceInt: Int = 0,
    val lastIntAt: Long = 0L,
    val puzzleNumber: Int = 1,       // ✅ increases every puzzle
    // ✅ new progression
    val score: Int = 0,
    val tier: Int = 1,
    val solvesInTier: Int = 0,               // 0..LEVEL_UP_EVERY_SOLVES-1
    val tierUpPulse: Int = 0,                // increments when tier increases

    val emojis: String = "",
    val answer: String = "",
    val category: Category = Category.PHRASES_IDIOMS,
    val masked: String = "",
    val guessed: Set<Char> = emptySet(),
    val wrong: Set<Char> = emptySet(),
    val solved: Boolean = false,
    val failed: Boolean = false
)


class GameViewModel(private val prefs: Prefs): ViewModel() {
    private val _ui = MutableStateFlow(GameState())
    val ui: StateFlow<GameState> = _ui

    private var order: List<Int> = emptyList()


    init {
        viewModelScope.launch {
            val p = prefs.flow.firstOrNull()
            val level = p?.get(Keys.LEVEL) ?: 1
            val attempts = p?.get(Keys.ATTEMPTS) ?: Rules.MAX_ATTEMPTS
            val lives = p?.get(Keys.LIVES) ?: Rules.ARCADE_LIVES
            val adsRemoved = p?.get(Keys.ADS_REMOVED) ?: false
            val solvedSince = p?.get(Keys.SOLVED_SINCE_INT) ?: 0
            val lastInt = p?.get(Keys.LAST_INT_AT) ?: 0L
            loadLevel(level, attempts, lives, adsRemoved, solvedSince, lastInt)
        }
    }

    private fun ensureOrder() {
        if (order.isEmpty()) order = EmojiPuzzles.puzzles.indices.shuffled()
    }

    private fun puzzleFor(level: Int): EmojiPuzzle {
        ensureOrder()
        val idx = order[(level - 1).mod(order.size)]
        return EmojiPuzzles.puzzles[idx]
    }

    private fun isRevealable(c:Char)= c.isLetter()
    private fun mask(answer:String, guessed:Set<Char>) =
        answer.map { ch ->
            when {
                !isRevealable(ch) -> ch
                guessed.contains(ch.lowercaseChar()) -> ch
                else -> '_'
            }
        }.joinToString("")

    private fun loadLevel(
        puzzleNumber: Int,
        attempts: Int,
        lives: Int,
        adsRemoved: Boolean,
        solvedSince: Int,
        lastInt: Long
    ) {
        val safePuzzleNumber = kotlin.math.max(1, puzzleNumber)
        val p = puzzleFor(safePuzzleNumber)

        val safeAttempts = if (attempts <= 0) Rules.MAX_ATTEMPTS else attempts

        // Clamp lives into 0..ARCADE_LIVES
        val targetLives = lives.coerceIn(0, Rules.ARCADE_LIVES)
        if (targetLives != lives) {
            viewModelScope.launch { prefs.setLives(targetLives) }
        }

        // ✅ Preserve progression fields from current state
        val prev = _ui.value

        _ui.value = prev.copy(
            // ✅ progression / meta
            level = prev.level,                // level only changes on 4-solve logic elsewhere
            puzzleNumber = safePuzzleNumber,
            score = prev.score,
            solvesInLevel = prev.solvesInLevel,
            levelUpPulse = prev.levelUpPulse,

            // ✅ persisted flags/counters
            adsRemoved = adsRemoved,
            solvedSinceInt = solvedSince,
            lastIntAt = lastInt,

            // ✅ round setup
            attemptsLeft = safeAttempts,
            livesLeft = targetLives,
            emojis = p.emojis,
            answer = p.answer,
            category = p.category,
            masked = mask(p.answer, emptySet()),

            // ✅ round reset
            guessed = emptySet(),
            wrong = emptySet(),
            solved = false,
            failed = false
        )
    }





// Small helper to DRY up "go to a specific puzzle with attempts/lives"
// Go to a specific puzzleNumber with attempts/lives
private fun advanceToPuzzle(puzzleNumber: Int, attempts: Int, lives: Int) {
    viewModelScope.launch {
        // For now, reuse existing prefs key (LEVEL) to store puzzleNumber
        // so you don’t break existing installs.
        prefs.setLevel(puzzleNumber)

        prefs.setAttempts(attempts)
        prefs.setLives(lives)

        loadLevel(
            puzzleNumber = puzzleNumber,
            attempts = attempts,
            lives = lives,
            adsRemoved = _ui.value.adsRemoved,
            solvedSince = _ui.value.solvedSinceInt,
            lastInt = _ui.value.lastIntAt
        )
    }
}



    fun onLetterTap(ch: Char) {
        val s = _ui.value
        if (s.solved || s.failed || s.attemptsLeft <= 0) return

        val letter = ch.lowercaseChar()
        if (!letter.isLetter() || s.guessed.contains(letter)) return

        val newGuessed = s.guessed + letter
        val hit = s.answer.any { it.lowercaseChar() == letter }

        if (hit) {
            val newMasked = mask(s.answer, newGuessed)
            val solvedNow = newMasked.none { it == '_' }

            if (!solvedNow) {
                _ui.value = s.copy(masked = newMasked, guessed = newGuessed)
                return
            }

            // ✅ SOLVED
            val points = 100 + if (s.wrong.isEmpty()) 25 else 0

            val newSolvesInTier = s.solvesInTier + 1
            val tierUp = newSolvesInTier >= Rules.LEVEL_UP_EVERY_SOLVES
            val nextTier = if (tierUp) s.tier + 1 else s.tier
            val nextSolvesInTier = if (tierUp) 0 else newSolvesInTier
            val nextPulse = if (tierUp) s.tierUpPulse + 1 else s.tierUpPulse

            _ui.value = s.copy(
                masked = newMasked,
                guessed = newGuessed,
                solved = true,

                score = s.score + points,
                tier = nextTier,
                solvesInTier = nextSolvesInTier,
                tierUpPulse = nextPulse,

                // Keep if you still want it for analytics; otherwise remove later
                solvedSinceInt = s.solvedSinceInt + 1
            )

            viewModelScope.launch {
                // You can remove this later if you stop using solvedSinceInt
                prefs.incSolvedSinceInt()

                // Persist clean attempts so you don’t reload mid-round
                prefs.setAttempts(Rules.MAX_ATTEMPTS)
            }
        } else {
            val attempts = s.attemptsLeft - 1
            val roundFailed = attempts <= 0
            val newLives = if (roundFailed) (s.livesLeft - 1).coerceAtLeast(0) else s.livesLeft

            _ui.value = s.copy(
                guessed = newGuessed,
                wrong = s.wrong + letter,
                attemptsLeft = attempts,
                failed = roundFailed,
                livesLeft = newLives,
                masked = if (roundFailed) s.answer else s.masked
            )

            viewModelScope.launch {
                prefs.setLives(newLives)
            }
        }
    }


    private fun handleHit(s: GameState, newGuessed: Set<Char>) {
        val newMasked = mask(s.answer, newGuessed)
        val solvedNow = newMasked.none { it == '_' }

        // Not solved yet → just update mask/guessed and return
        if (!solvedNow) {
            _ui.value = s.copy(
                masked = newMasked,
                guessed = newGuessed
            )
            return
        }

        // ✅ SOLVED (this block runs exactly once per puzzle)
        val nextSolvedSince = s.solvedSinceInt + 1

        // If you haven't added these fields yet, either:
        // 1) add them to GameState, OR
        // 2) temporarily remove these 4 lines.
        val points = 100 + if (s.wrong.isEmpty()) 25 else 0
        val newSolvesInTier = s.solvesInTier + 1
        val tierUp = newSolvesInTier >= 4
        val nextSolvesInTier = if (tierUp) 0 else newSolvesInTier

        _ui.value = s.copy(
            masked = newMasked,
            guessed = newGuessed,
            solved = true,
            solvedSinceInt = nextSolvedSince,

            // ---- Progression (requires fields in GameState) ----
            score = s.score + points,
            tier = if (tierUp) s.tier + 1 else s.tier,
            solvesInTier = nextSolvesInTier,
            tierUpPulse = if (tierUp) s.tierUpPulse + 1 else s.tierUpPulse
        )

        // Persist important bits
        viewModelScope.launch {
            prefs.incSolvedSinceInt()
            prefs.setAttempts(Rules.MAX_ATTEMPTS)
        }
    }

    private fun handleMiss(s: GameState, newGuessed: Set<Char>, letter: Char) {
        val attempts = s.attemptsLeft - 1
        val roundFailed = attempts <= 0
        val newLives = if (roundFailed) (s.livesLeft - 1).coerceAtLeast(0) else s.livesLeft
        val newWrong = s.wrong + letter

        _ui.value = s.copy(
            guessed = newGuessed,
            wrong = newWrong,
            attemptsLeft = attempts,
            failed = roundFailed,
            livesLeft = newLives,
            masked = if (roundFailed) s.answer else s.masked // reveal on fail
        )

        viewModelScope.launch {
            prefs.setLives(newLives)
            // If you ever want to persist attempts as well, do it here.
            // prefs.setAttempts(attempts.coerceAtLeast(0))
        }
    }


    fun next() {
        val s = _ui.value

        // Game Over → full reset and new order
        if (s.livesLeft <= 0) {
            order = emptyList() // optional: reshuffle after game over

            // ✅ Reset progression for a brand new run
            _ui.value = s.copy(
                tier = 1,
                solvesInTier = 0,
                tierUpPulse = 0,
                score = 0,
                // if you track puzzle number separately, reset it too:
                // puzzleNumber = 1
            )

            // ✅ Start over at puzzle #1
            advanceToPuzzle(
                puzzleNumber = 1,
                attempts = Rules.MAX_ATTEMPTS,
                lives = Rules.ARCADE_LIVES
            )
            return
        }

        // Normal: advance to next puzzle (not tier)
        val nextPuzzle = s.puzzleNumber + 1 // <-- make sure GameState has puzzleNumber
        advanceToPuzzle(
            puzzleNumber = nextPuzzle,
            attempts = Rules.MAX_ATTEMPTS,
            lives = s.livesLeft
        )
    }






    fun shouldShowInterstitial(now: Long): Boolean {
        val s = _ui.value
        if (s.adsRemoved) return false
        if (now - s.lastIntAt < Rules.INTERSTITIAL_MIN_MS) return false
        return true
    }

    fun onInterstitialShown(now: Long) {
        val s = _ui.value
        _ui.value = s.copy(lastIntAt = now)
        viewModelScope.launch { prefs.setLastIntAt(now) }
    }


    fun setAdsRemoved(r:Boolean) {
        _ui.value = _ui.value.copy(adsRemoved=r)
        viewModelScope.launch{ prefs.setAdsRemoved(r) }
    }
}


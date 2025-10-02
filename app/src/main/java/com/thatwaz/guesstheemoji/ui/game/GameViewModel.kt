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
    val level:Int=1,
    val attemptsLeft:Int=Rules.MAX_ATTEMPTS,
    val livesLeft:Int=Rules.ARCADE_LIVES,
    val adsRemoved:Boolean=false,
    val solvedSinceInt:Int=0,
    val lastIntAt:Long=0L,
    val emojis:String="",
    val answer:String="",
    val category: Category = Category.PHRASES_IDIOMS,
    val masked:String="",
    val guessed:Set<Char> = emptySet(),
    val wrong:Set<Char> = emptySet(),
    val solved:Boolean=false,
    val failed:Boolean=false
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
        level: Int,
        attempts: Int,
        lives: Int,
        adsRemoved: Boolean,
        solvedSince: Int,
        lastInt: Long
    ) {
        val p = puzzleFor(level)

        val safeAttempts = if (attempts <= 0) Rules.MAX_ATTEMPTS else attempts

        // ✅ Clamp lives into 1..ARCADE_LIVES and persist if it was different
        val targetLives = lives.coerceIn(1, Rules.ARCADE_LIVES)
        if (targetLives != lives) {
            viewModelScope.launch { prefs.setLives(targetLives) }
        }

        _ui.value = GameState(
            level = kotlin.math.max(1, level),
            attemptsLeft = safeAttempts,
            livesLeft = targetLives,
            adsRemoved = adsRemoved,
            solvedSinceInt = solvedSince,
            lastIntAt = lastInt,
            emojis = p.emojis,
            answer = p.answer,
            category = p.category,
            masked = mask(p.answer, emptySet())
        )
    }


    // Small helper to DRY up "go to a specific level with attempts/lives"
    private fun advanceTo(level: Int, attempts: Int, lives: Int) {
        viewModelScope.launch {
            prefs.setLevel(level)
            prefs.setAttempts(attempts)
            prefs.setLives(lives)
            loadLevel(
                level = level,
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

        val L = ch.lowercaseChar()
        if (!L.isLetter() || s.guessed.contains(L)) return

        val hit = s.answer.lowercase().any { it == L }
        val newGuessed = s.guessed + L

        if (hit) {
            val newMasked = mask(s.answer, newGuessed)
            val solved = newMasked.none { it == '_' }
            val nextSolvedSince = if (solved) s.solvedSinceInt + 1 else s.solvedSinceInt

            _ui.value = s.copy(
                masked = newMasked,
                guessed = newGuessed,
                solved = solved,
                solvedSinceInt = nextSolvedSince
            )

            if (solved) {
                viewModelScope.launch {
                    prefs.incSolvedSinceInt()
                    // Persist a clean attempts value so a background/kill doesn't reload mid-round
                    prefs.setAttempts(Rules.MAX_ATTEMPTS)
                }
            }
        } else {
            val attempts = s.attemptsLeft - 1
            val roundFailed = attempts <= 0
            val newLives = if (roundFailed) (s.livesLeft - 1).coerceAtLeast(0) else s.livesLeft

            _ui.value = s.copy(
                guessed = newGuessed,
                wrong = s.wrong + L,
                attemptsLeft = attempts,
                failed = roundFailed,
                livesLeft = newLives,
                masked = if (roundFailed) s.answer else s.masked // reveal on fail
            )

            viewModelScope.launch {
                prefs.setLives(newLives)
                // Option A (current): keep attempts as-is; next() will reset to MAX.
                // Option B (eager reset): uncomment the next line to persist MAX immediately:
                // prefs.setAttempts(Rules.MAX_ATTEMPTS)
            }
        }
    }

    fun next() {
        val s = _ui.value

        // Game Over → full reset and new order
        if (s.livesLeft <= 0) {
            order = emptyList() // optional: new shuffle after game over
            advanceTo(
                level = 1,
                attempts = Rules.MAX_ATTEMPTS,
                lives = Rules.ARCADE_LIVES
            )
            return
        }

        // Continue to next level with fresh attempts
        val nextLevel = s.level + 1
        advanceTo(
            level = nextLevel,
            attempts = Rules.MAX_ATTEMPTS,
            lives = s.livesLeft
        )
    }

    fun shouldShowInterstitial(now:Long): Boolean {
        val s=_ui.value
        if (s.adsRemoved || !s.solved) return false
        if (s.solvedSinceInt==0 || s.solvedSinceInt % Rules.INTERSTITIAL_EVERY_NSOLVES != 0) return false
        if (now - s.lastIntAt < Rules.INTERSTITIAL_MIN_MS) return false
        return true
    }

    fun onInterstitialShown(now:Long) {
        val s=_ui.value
        _ui.value = s.copy(lastIntAt=now, solvedSinceInt=0)
        viewModelScope.launch { prefs.setLastIntAt(now); prefs.resetSolvedSinceInt() }
    }

    fun setAdsRemoved(r:Boolean) {
        _ui.value = _ui.value.copy(adsRemoved=r)
        viewModelScope.launch{ prefs.setAdsRemoved(r) }
    }
}


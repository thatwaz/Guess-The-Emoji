package com.thatwaz.guesstheemoji.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val masked:String="",
    val guessed:Set<Char> = emptySet(),
    val wrong:Set<Char> = emptySet(),
    val solved:Boolean=false,
    val failed:Boolean=false
)

class GameViewModel(private val prefs: Prefs): ViewModel() {
    private val _ui = MutableStateFlow(GameState())
    val ui: StateFlow<GameState> = _ui

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

    private fun puzzleFor(level:Int)= EmojiPuzzles.puzzles[(level-1).mod(EmojiPuzzles.puzzles.size)]

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

        // CLAMP stale state so keyboard is enabled on first screen
        val safeAttempts = if (attempts <= 0) com.thatwaz.guesstheemoji.domain.Rules.MAX_ATTEMPTS else attempts
        val safeLives    = if (lives <= 0) com.thatwaz.guesstheemoji.domain.Rules.ARCADE_LIVES else lives

        _ui.value = GameState(
            level = kotlin.math.max(1, level),
            attemptsLeft = safeAttempts,
            livesLeft = safeLives,
            adsRemoved = adsRemoved,
            solvedSinceInt = solvedSince,
            lastIntAt = lastInt,
            emojis = p.emojis,
            answer = p.answer,
            masked = mask(p.answer, emptySet())
        )
    }


    fun onLetterTap(ch:Char) {
        val s = _ui.value
        if (s.solved || s.failed || s.attemptsLeft<=0) return
        val L = ch.lowercaseChar()
        if (!L.isLetter() || s.guessed.contains(L)) return

        val ansLower = s.answer.lowercase()
        val hit = ansLower.any { it==L }
        val newGuessed = s.guessed + L

        if (hit) {
            val newMasked = mask(s.answer, newGuessed)
            val solved = newMasked.none { it=='_' }
            val nextSolvedSince = if (solved) s.solvedSinceInt+1 else s.solvedSinceInt
            _ui.value = s.copy(masked=newMasked, guessed=newGuessed, solved=solved, solvedSinceInt=nextSolvedSince)
            if (solved) viewModelScope.launch { prefs.incSolvedSinceInt() }
        } else {
            val attempts = s.attemptsLeft-1
            val failThisPuzzle = attempts<=0
            val newLives = if (failThisPuzzle) (s.livesLeft-1).coerceAtLeast(0) else s.livesLeft
            _ui.value = s.copy(
                guessed=newGuessed,
                wrong=s.wrong + L,
                attemptsLeft=attempts,
                failed = failThisPuzzle && newLives==s.livesLeft,
                livesLeft=newLives
            )
            viewModelScope.launch {
                prefs.setAttempts(attempts)
                prefs.setLives(newLives)
            }
        }
    }

    fun next() {
        val s=_ui.value
        if (s.livesLeft <= 0) {
            viewModelScope.launch {
                prefs.setLevel(1)
                prefs.setAttempts(com.thatwaz.guesstheemoji.domain.Rules.MAX_ATTEMPTS)
                prefs.setLives(com.thatwaz.guesstheemoji.domain.Rules.ARCADE_LIVES)
                loadLevel(1, com.thatwaz.guesstheemoji.domain.Rules.MAX_ATTEMPTS,
                    com.thatwaz.guesstheemoji.domain.Rules.ARCADE_LIVES,
                    s.adsRemoved, s.solvedSinceInt, s.lastIntAt)
            }
            return
        }
        val nextLevel = s.level+1
        viewModelScope.launch {
            prefs.setLevel(nextLevel); prefs.setAttempts(Rules.MAX_ATTEMPTS)
            loadLevel(nextLevel, Rules.MAX_ATTEMPTS, s.livesLeft, s.adsRemoved, s.solvedSinceInt, s.lastIntAt)
        }
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
    fun setAdsRemoved(r:Boolean) { _ui.value = _ui.value.copy(adsRemoved=r); viewModelScope.launch{ prefs.setAdsRemoved(r)} }
}

package com.thatwaz.guesstheemoji.ui.game

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thatwaz.guesstheemoji.data.Category
import com.thatwaz.guesstheemoji.data.EmojiPuzzle
import com.thatwaz.guesstheemoji.data.EmojiPuzzles
import com.thatwaz.guesstheemoji.data.Keys
import com.thatwaz.guesstheemoji.data.Prefs
import com.thatwaz.guesstheemoji.data.ScoreEntry
import com.thatwaz.guesstheemoji.data.ScoreStore
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
    val gameOverPulse: Int = 0,
    val solvesInTier: Int = 0,               // 0..LEVEL_UP_EVERY_SOLVES-1
    val tierUpPulse: Int = 0,                // increments when tier increases
    val hasActiveRun: Boolean = false,
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
    private var orderCycle: Int = -1   // ✅ tracks which full-pass we’re on

    private var deck: IntArray = intArrayOf()
    private var deckPos: Int = 0           // global position
    private var runStartPos: Int = 0       // position at start of this run

    private val scoreStore = ScoreStore(prefs)




    init {
        viewModelScope.launch {
            val p = prefs.flow.firstOrNull()

            val level = p?.get(Keys.LEVEL) ?: 1
            val hasActiveRun = p?.get(Keys.HAS_ACTIVE_RUN) ?: false
            val attempts = p?.get(Keys.ATTEMPTS) ?: Rules.MAX_ATTEMPTS
            val lives = p?.get(Keys.LIVES) ?: Rules.ARCADE_LIVES
            val adsRemoved = p?.get(Keys.ADS_REMOVED) ?: false
            val solvedSince = p?.get(Keys.SOLVED_SINCE_INT) ?: 0
            val lastInt = p?.get(Keys.LAST_INT_AT) ?: 0L

            // ✅ OPTION B: load persisted deck state
            val puzzleCount = EmojiPuzzles.puzzles.size
            val deckCsv = p?.get(Keys.PUZZLE_DECK) ?: ""
            val savedPos = p?.get(Keys.PUZZLE_POS) ?: 0
            val savedRunStart = p?.get(Keys.RUN_START_POS) ?: savedPos

            deck = deckCsv.toIntArrayCsv()
            deckPos = savedPos
            runStartPos = savedRunStart

            // ✅ Fix invalid/missing deck (first install or puzzle list changed)
            if (puzzleCount == 0) {
                // nothing to load; keep defaults
            } else if (deck.size != puzzleCount) {
                deck = buildDeck(puzzleCount)
                deckPos = 0
                runStartPos = 0

                prefs.setPuzzleDeckCsv(deck.toCsv())
                prefs.setPuzzlePos(0)
                prefs.setRunStartPos(0)
            }

            // ✅ now load the current puzzle into UI state
            loadLevel(level, attempts, lives, adsRemoved, solvedSince, lastInt)

            // ✅ restore run flag
            _ui.value = _ui.value.copy(hasActiveRun = hasActiveRun)
        }
    }



    private fun buildDeck(size: Int): IntArray =
        (0 until size).shuffled().toIntArray()

    private fun IntArray.toCsv(): String = joinToString(",")
    private fun String.toIntArrayCsv(): IntArray =
        if (isBlank()) intArrayOf() else split(",").map { it.toInt() }.toIntArray()

    private fun ensureOrder() {
        if (order.isEmpty()) {
            order = EmojiPuzzles.puzzles.indices.shuffled()
            orderCycle = 0
        }
    }

    private fun puzzleFor(puzzleNumber: Int): EmojiPuzzle {
        val size = EmojiPuzzles.puzzles.size
        if (size == 0) error("No puzzles available")

        // Safety: ensure deck is valid
        if (deck.size != size) {
            deck = buildDeck(size)
            deckPos = 0
            runStartPos = 0
            viewModelScope.launch {
                prefs.setPuzzleDeckCsv(deck.toCsv())
                prefs.setPuzzlePos(0)
                prefs.setRunStartPos(0)
            }
        }

        val zeroBased = (puzzleNumber - 1).coerceAtLeast(0)

        val globalIndex = runStartPos + zeroBased

        // If we passed the end, reshuffle and wrap
        val cycle = globalIndex / size
        val posInCycle = globalIndex % size

        if (cycle > 0) {
            // We completed at least one full deck since runStartPos
            // Simplest: reshuffle for the new cycle and reset runStartPos inside this cycle
            deck = buildDeck(size)
            val newRunStart = 0
            runStartPos = newRunStart

            viewModelScope.launch {
                prefs.setPuzzleDeckCsv(deck.toCsv())
                prefs.setPuzzlePos(0)
                prefs.setRunStartPos(0)
            }

            // now use posInCycle for new deck (posInCycle is still fine)
            val idx = deck[posInCycle]
            return EmojiPuzzles.puzzles[idx]
        }

        val idx = deck[posInCycle]

        Log.d(
            "PUZZLE_DECK",
            "runStartPos=$runStartPos deckPos=$deckPos puzzleNumber=$puzzleNumber posInCycle=$posInCycle idx=$idx"
        )
        return EmojiPuzzles.puzzles[idx]
    }


    private fun updateAndPersistDeckPosFor(puzzleNumber: Int) {
        val size = EmojiPuzzles.puzzles.size
        if (size <= 0) return

        val zeroBased = (puzzleNumber - 1).coerceAtLeast(0)

        // deckPos = where we are globally right now
        deckPos = (runStartPos + zeroBased) % size

        viewModelScope.launch {
            prefs.setPuzzlePos(deckPos)
        }
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
            level = prev.level,
            puzzleNumber = safePuzzleNumber,
            score = prev.score,
            solvesInLevel = prev.solvesInLevel,
            levelUpPulse = prev.levelUpPulse,

            // ✅ IMPORTANT: preserve run state
            hasActiveRun = prev.hasActiveRun,

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





    // Go to a specific puzzleNumber with attempts/lives (+ run state)
    private fun advanceToPuzzle(
        puzzleNumber: Int,
        attempts: Int,
        lives: Int,
        hasActiveRun: Boolean = true // ✅ default
    ) {
        viewModelScope.launch {
            prefs.setLevel(puzzleNumber)
            prefs.setAttempts(attempts)
            prefs.setLives(lives)
            prefs.setHasActiveRun(hasActiveRun)

            updateAndPersistDeckPosFor(puzzleNumber)

            loadLevel(
                puzzleNumber = puzzleNumber,
                attempts = attempts,
                lives = lives,
                adsRemoved = _ui.value.adsRemoved,
                solvedSince = _ui.value.solvedSinceInt,
                lastInt = _ui.value.lastIntAt
            )
            _ui.value = _ui.value.copy(hasActiveRun = hasActiveRun)
        }
    }





    fun onLetterTap(ch: Char) {
        val curr = _ui.value
        if (curr.solved || curr.failed || curr.attemptsLeft <= 0) return

        val letter = ch.lowercaseChar()
        if (!letter.isLetter() || curr.guessed.contains(letter)) return

        // ✅ Mark run active the first time they actually play
        if (!curr.hasActiveRun) {
            _ui.value = curr.copy(hasActiveRun = true)
            viewModelScope.launch { prefs.setHasActiveRun(true) }
        }

        // IMPORTANT: re-read after the possible copy above
        val s = _ui.value

        val newGuessed = s.guessed + letter
        val hit = s.answer.any { it.lowercaseChar() == letter }

        if (hit) {
            handleHit(letter = letter, s = s, newGuessed = newGuessed)
        } else {
            handleMiss(letter = letter, s = s, newGuessed = newGuessed)
        }
    }

    /** ✅ Hit branch */
    private fun handleHit(letter: Char, s: GameState, newGuessed: Set<Char>) {
        val newMasked = mask(s.answer, newGuessed)
        val solvedNow = newMasked.none { it == '_' }

        // Not solved yet → just update mask/guessed
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

            solvedSinceInt = s.solvedSinceInt + 1

            // ❌ DO NOT change puzzleNumber here
        )

        viewModelScope.launch {
            prefs.incSolvedSinceInt()
            prefs.setAttempts(Rules.MAX_ATTEMPTS)
        }

    }

    /** ✅ Miss branch */
    private fun handleMiss(letter: Char, s: GameState, newGuessed: Set<Char>) {
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

        // ✅ RUN ENDS NOW
        if (roundFailed && newLives <= 0) {
            endRunAndSaveScore(s.copy(livesLeft = newLives, failed = true))
        }
    }



    fun continueGame() {
        // just marks active; game route will show whatever is currently loaded
        viewModelScope.launch {
            prefs.setHasActiveRun(true)
            _ui.value = _ui.value.copy(hasActiveRun = true)
        }
    }



    fun next() {
        val s = _ui.value

        // ✅ Game Over → SAVE SCORE ONCE, then reset + go home/scores
        if (s.livesLeft <= 0) {


            // ✅ mark run ended so we don't loop "game over" forever
            _ui.value = s.copy(hasActiveRun = false)
            viewModelScope.launch { prefs.setHasActiveRun(false) }

            // ✅ reset run state
            order = emptyList()
            orderCycle = -1
            _ui.value = _ui.value.copy(
                tier = 1,
                solvesInTier = 0,
                tierUpPulse = 0,
                score = 0,
                puzzleNumber = 1,
                livesLeft = Rules.ARCADE_LIVES,
                attemptsLeft = Rules.MAX_ATTEMPTS,
                solved = false,
                failed = false,
                guessed = emptySet(),
                wrong = emptySet()
            )

            // Persist new run baseline so resume doesn't revive the dead run
            viewModelScope.launch {
                prefs.setLevel(1)
                prefs.setLives(Rules.ARCADE_LIVES)
                prefs.setAttempts(Rules.MAX_ATTEMPTS)
            }

            return
        }

        // Normal: advance to next puzzle
        // Normal: advance to next puzzle
        val nextPuzzle = s.puzzleNumber + 1
        advanceToPuzzle(
            puzzleNumber = nextPuzzle,
            attempts = Rules.MAX_ATTEMPTS,
            lives = s.livesLeft,
            hasActiveRun = true
        )

    }

    fun quitToHome() {
        // 1) Update in-memory UI immediately so dialog won't reappear
        _ui.value = _ui.value.copy(
            hasActiveRun = false,
            // reset the "dead run" flags so game screen won't show game over
            livesLeft = Rules.ARCADE_LIVES,
            attemptsLeft = Rules.MAX_ATTEMPTS,
            solved = false,
            failed = false,
            guessed = emptySet(),
            wrong = emptySet(),
            puzzleNumber = 1,
            tier = 1,
            solvesInTier = 0,
            tierUpPulse = 0,
            score = 0
        )

        // 2) Persist so Home won't show "Resume"
        viewModelScope.launch {
            prefs.setHasActiveRun(false)
            prefs.setLevel(1)
            prefs.setAttempts(Rules.MAX_ATTEMPTS)
            prefs.setLives(Rules.ARCADE_LIVES)
            prefs.resetSolvedSinceInt()
            prefs.setLastIntAt(0L)
        }
    }



    fun startNewRun() {
        // Run begins at the current global position
        runStartPos = deckPos

        // ✅ RESET in-memory run state FIRST (so loadLevel() "preserves" the reset values)
        _ui.value = _ui.value.copy(
            tier = 1,
            solvesInTier = 0,
            tierUpPulse = 0,
            score = 0,

            puzzleNumber = 1,
            attemptsLeft = Rules.MAX_ATTEMPTS,
            livesLeft = Rules.ARCADE_LIVES,

            guessed = emptySet(),
            wrong = emptySet(),
            solved = false,
            failed = false,

            hasActiveRun = true
        )

        viewModelScope.launch {
            prefs.setHasActiveRun(true)
            prefs.setLevel(1)
            prefs.setAttempts(Rules.MAX_ATTEMPTS)
            prefs.setLives(Rules.ARCADE_LIVES)

            // ✅ Option B state
            prefs.setRunStartPos(runStartPos)

            loadLevel(
                puzzleNumber = 1,
                attempts = Rules.MAX_ATTEMPTS,
                lives = Rules.ARCADE_LIVES,
                adsRemoved = _ui.value.adsRemoved,
                solvedSince = _ui.value.solvedSinceInt,
                lastInt = _ui.value.lastIntAt
            )
        }
    }





//    fun startNewRun() {
//        order = emptyList()
//        orderCycle = -1   // ✅ ADD THIS
//
//        _ui.value = _ui.value.copy(
//            tier = 1,
//            solvesInTier = 0,
//            tierUpPulse = 0,
//            score = 0,
//            puzzleNumber = 1,
//            attemptsLeft = Rules.MAX_ATTEMPTS,
//            livesLeft = Rules.ARCADE_LIVES,
//            guessed = emptySet(),
//            wrong = emptySet(),
//            solved = false,
//            failed = false,
//            hasActiveRun = true
//        )
//
//        viewModelScope.launch {
//            prefs.setHasActiveRun(true)
//            prefs.setLevel(1)
//            prefs.setAttempts(Rules.MAX_ATTEMPTS)
//            prefs.setLives(Rules.ARCADE_LIVES)
//
//            loadLevel(
//                puzzleNumber = 1,
//                attempts = Rules.MAX_ATTEMPTS,
//                lives = Rules.ARCADE_LIVES,
//                adsRemoved = _ui.value.adsRemoved,
//                solvedSince = _ui.value.solvedSinceInt,
//                lastInt = _ui.value.lastIntAt
//            )
//        }
//    }


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

    private fun endRunAndSaveScore(s: GameState) {
        // prevent duplicates if UI triggers multiple times
        if (!s.hasActiveRun) return

        // mark ended immediately in-memory
        _ui.value = s.copy(
            hasActiveRun = false,
            gameOverPulse = s.gameOverPulse + 1
        )

        viewModelScope.launch {
            // mark ended in prefs
            prefs.setHasActiveRun(false)

            // ✅ SAVE SCORE HERE (reliable)
            scoreStore.addScore(
                ScoreEntry(
                    score = s.score,
                    tier = s.tier,
                    puzzleNumber = s.puzzleNumber,
                    endedAtEpochMs = System.currentTimeMillis()
                )
            )
        }
    }



    fun setAdsRemoved(r:Boolean) {
        _ui.value = _ui.value.copy(adsRemoved=r)
        viewModelScope.launch{ prefs.setAdsRemoved(r) }
    }
}


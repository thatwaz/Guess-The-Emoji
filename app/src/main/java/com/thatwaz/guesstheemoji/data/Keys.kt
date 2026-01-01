package com.thatwaz.guesstheemoji.data

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.thatwaz.guesstheemoji.domain.Rules
import kotlinx.coroutines.flow.map

val Context.ds by preferencesDataStore("gte_prefs")

object Keys {
    val LEVEL = intPreferencesKey("level")
    val ATTEMPTS = intPreferencesKey("attempts")
    val LIVES = intPreferencesKey("lives")
    val ADS_REMOVED = booleanPreferencesKey("ads_removed")
    val SOLVED_SINCE_INT = intPreferencesKey("solved_since_int")
    val LAST_INT_AT = longPreferencesKey("last_int_at")
    val HAS_ACTIVE_RUN = booleanPreferencesKey("has_active_run")
    val PUZZLE_DECK = stringPreferencesKey("puzzle_deck_csv")
    val PUZZLE_POS = intPreferencesKey("puzzle_deck_pos")
    val RUN_START_POS = intPreferencesKey("run_start_pos")
    val THEME_MODE = intPreferencesKey("theme_mode") // 0=system, 1=light, 2=dark
    val SCORE = intPreferencesKey("score")
    val TIER = intPreferencesKey("tier")
    val SOLVES_IN_TIER = intPreferencesKey("solves_in_tier")
    val TIER_UP_PULSE = intPreferencesKey("tier_up_pulse")


}

class Prefs(val ctx: Context) {
    val flow = ctx.ds.data.map { it }

    suspend fun setLevel(v: Int) = ctx.ds.edit { it[Keys.LEVEL] = v }           // currently used as puzzleNumber
    suspend fun setAttempts(v: Int) = ctx.ds.edit { it[Keys.ATTEMPTS] = v }
    suspend fun setLives(v: Int) = ctx.ds.edit { it[Keys.LIVES] = v }
    suspend fun setAdsRemoved(v: Boolean) = ctx.ds.edit { it[Keys.ADS_REMOVED] = v }

    suspend fun setPuzzleDeckCsv(csv: String) = edit { it[Keys.PUZZLE_DECK] = csv }
    suspend fun setPuzzlePos(pos: Int) = edit { it[Keys.PUZZLE_POS] = pos }
    suspend fun setRunStartPos(pos: Int) = edit { it[Keys.RUN_START_POS] = pos }

    suspend fun setThemeMode(mode: Int) = ctx.ds.edit { it[Keys.THEME_MODE] = mode }
    suspend fun setScore(v: Int) = ctx.ds.edit { it[Keys.SCORE] = v }
    suspend fun setTier(v: Int) = ctx.ds.edit { it[Keys.TIER] = v }
    suspend fun setSolvesInTier(v: Int) = ctx.ds.edit { it[Keys.SOLVES_IN_TIER] = v }
    suspend fun setTierUpPulse(v: Int) = ctx.ds.edit { it[Keys.TIER_UP_PULSE] = v }



    suspend fun incSolvedSinceInt() = ctx.ds.edit {
        it[Keys.SOLVED_SINCE_INT] = (it[Keys.SOLVED_SINCE_INT] ?: 0) + 1
    }
    suspend fun resetSolvedSinceInt() = ctx.ds.edit { it[Keys.SOLVED_SINCE_INT] = 0 }

    suspend fun setLastIntAt(t: Long) = ctx.ds.edit { it[Keys.LAST_INT_AT] = t }

    suspend fun setHasActiveRun(v: Boolean) = ctx.ds.edit { it[Keys.HAS_ACTIVE_RUN] = v }

    // ✅ One-call reset for "Start New Game"
    suspend fun resetRunToNewGame() = ctx.ds.edit {
        it[Keys.HAS_ACTIVE_RUN] = false
        it[Keys.LEVEL] = 1                 // puzzleNumber
        it[Keys.ATTEMPTS] = Rules.MAX_ATTEMPTS
        it[Keys.LIVES] = Rules.ARCADE_LIVES
        it[Keys.SOLVED_SINCE_INT] = 0
        it[Keys.LAST_INT_AT] = 0L
        // leave ADS_REMOVED alone
    }

    suspend fun resetProgression() = ctx.ds.edit {
        it[Keys.SCORE] = 0
        it[Keys.TIER] = 1
        it[Keys.SOLVES_IN_TIER] = 0
        it[Keys.TIER_UP_PULSE] = 0
    }




    suspend fun edit(block: (MutablePreferences) -> Unit) {
        ctx.ds.edit { prefs -> block(prefs) }
    }
}


//class Prefs(private val ctx: Context) {
//    val flow = ctx.ds.data.map { it }
//    suspend fun setLevel(v:Int)=ctx.ds.edit{it[Keys.LEVEL]=v}
//    suspend fun setAttempts(v:Int)=ctx.ds.edit{it[Keys.ATTEMPTS]=v}
//    suspend fun setLives(v:Int)=ctx.ds.edit{it[Keys.LIVES]=v}
//    suspend fun setAdsRemoved(v:Boolean)=ctx.ds.edit{it[Keys.ADS_REMOVED]=v}
//    suspend fun incSolvedSinceInt()=ctx.ds.edit{it[Keys.SOLVED_SINCE_INT]=(it[Keys.SOLVED_SINCE_INT]?:0)+1}
//    suspend fun resetSolvedSinceInt()=ctx.ds.edit{it[Keys.SOLVED_SINCE_INT]=0}
//    suspend fun setLastIntAt(t:Long)=ctx.ds.edit{it[Keys.LAST_INT_AT]=t}
//    suspend fun setHasActiveRun(v: Boolean) = ctx.ds.edit { it[Keys.HAS_ACTIVE_RUN] = v }
//    suspend fun getHasActiveRunDefaultFalse() = flow.map { it[Keys.HAS_ACTIVE_RUN] ?: false }
//
//}

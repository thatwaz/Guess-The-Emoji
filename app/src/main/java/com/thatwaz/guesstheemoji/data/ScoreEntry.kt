package com.thatwaz.guesstheemoji.data

import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

@Serializable
data class ScoreEntry(
    val score: Int,
    val tier: Int,
    val puzzleNumber: Int,
    val endedAtEpochMs: Long
)

class ScoreStore(private val prefs: Prefs) {

    companion object {
        private val KEY_SCORES_JSON = stringPreferencesKey("scores_json")

        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        private val listSer = ListSerializer(ScoreEntry.serializer())
    }

    fun scoresFlow(): Flow<List<ScoreEntry>> =
        prefs.flow.map { p ->
            val raw = p[KEY_SCORES_JSON] ?: return@map emptyList()
            runCatching { json.decodeFromString(listSer, raw) }
                .getOrElse { emptyList() }
        }

    suspend fun addScore(entry: ScoreEntry, maxKeep: Int = 30) {
        val current = scoresFlow().first()
        val updated = (listOf(entry) + current).take(maxKeep)
        saveScores(updated)
    }

    suspend fun clearScores() {
        prefs.edit { it.remove(KEY_SCORES_JSON) }
    }

    suspend fun deleteScore(entry: ScoreEntry) {
        val current = scoresFlow().first()
        val updated = current.filterNot {
            // This is your "ID" right now; good enough for MVP
            it.endedAtEpochMs == entry.endedAtEpochMs
        }
        saveScores(updated)
    }

    private suspend fun saveScores(list: List<ScoreEntry>) {
        prefs.edit { p ->
            p[KEY_SCORES_JSON] = json.encodeToString(listSer, list)
        }
    }
}


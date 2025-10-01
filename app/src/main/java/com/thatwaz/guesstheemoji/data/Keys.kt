package com.thatwaz.guesstheemoji.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.ds by preferencesDataStore("gte_prefs")

object Keys {
    val LEVEL = intPreferencesKey("level")
    val ATTEMPTS = intPreferencesKey("attempts")
    val LIVES = intPreferencesKey("lives")
    val ADS_REMOVED = booleanPreferencesKey("ads_removed")
    val SOLVED_SINCE_INT = intPreferencesKey("solved_since_int")
    val LAST_INT_AT = longPreferencesKey("last_int_at")
}

class Prefs(private val ctx: Context) {
    val flow = ctx.ds.data.map { it }
    suspend fun setLevel(v:Int)=ctx.ds.edit{it[Keys.LEVEL]=v}
    suspend fun setAttempts(v:Int)=ctx.ds.edit{it[Keys.ATTEMPTS]=v}
    suspend fun setLives(v:Int)=ctx.ds.edit{it[Keys.LIVES]=v}
    suspend fun setAdsRemoved(v:Boolean)=ctx.ds.edit{it[Keys.ADS_REMOVED]=v}
    suspend fun incSolvedSinceInt()=ctx.ds.edit{it[Keys.SOLVED_SINCE_INT]=(it[Keys.SOLVED_SINCE_INT]?:0)+1}
    suspend fun resetSolvedSinceInt()=ctx.ds.edit{it[Keys.SOLVED_SINCE_INT]=0}
    suspend fun setLastIntAt(t:Long)=ctx.ds.edit{it[Keys.LAST_INT_AT]=t}
}

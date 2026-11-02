package com.eta.saveup.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "saveup_prefs")

class AppPrefs(private val ctx: Context) {

    companion object {
        private val HAPTICS = booleanPreferencesKey("haptics_enabled")
        private val DAILY_GOAL = longPreferencesKey("daily_goal_cent")
        private val ACCENT = intPreferencesKey("accent_index")
        private val ONBOARDED = booleanPreferencesKey("onboarded")
    }

    val hapticsEnabled: Flow<Boolean> = ctx.dataStore.data.map { it[HAPTICS] ?: true }
    val dailyGoalCent: Flow<Long> = ctx.dataStore.data.map { it[DAILY_GOAL] ?: 0L }
    val accentIndex: Flow<Int> = ctx.dataStore.data.map { it[ACCENT] ?: 0 }
    val onboarded: Flow<Boolean> = ctx.dataStore.data.map { it[ONBOARDED] ?: false }

    suspend fun setHaptics(enabled: Boolean) {
        ctx.dataStore.edit { it[HAPTICS] = enabled }
    }

    suspend fun setDailyGoal(cent: Long) {
        ctx.dataStore.edit { it[DAILY_GOAL] = cent }
    }

    suspend fun setAccent(index: Int) {
        ctx.dataStore.edit { it[ACCENT] = index }
    }

    suspend fun setOnboarded(v: Boolean) {
        ctx.dataStore.edit { it[ONBOARDED] = v }
    }
}

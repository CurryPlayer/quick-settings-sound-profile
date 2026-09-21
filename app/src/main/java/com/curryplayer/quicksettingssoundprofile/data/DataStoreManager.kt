package com.curryplayer.quicksettingssoundprofile.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class DataStoreManager(private val context: Context) {
    companion object {
        val ZEN_RULE_ID = stringPreferencesKey("zen_rule_id")
        val ICON_THEME = intPreferencesKey("icon_theme")
        val TIMER_END_TIME = longPreferencesKey("timer_end_time")
        val PREVIOUS_RINGER_MODE = intPreferencesKey("previous_ringer_mode")
        val LAST_SELECTED_MUTE_DURATION_MINUTES = intPreferencesKey("last_selected_mute_duration_minutes")
    }

    val zenRuleId: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[ZEN_RULE_ID] ?: ""
    }

    val iconTheme: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[ICON_THEME] ?: 0
    }

    val timerEndTime: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[TIMER_END_TIME] ?: 0L
    }

    val previousRingerMode: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PREVIOUS_RINGER_MODE] ?: -1
    }

    val lastMuteDurationMinutes: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[LAST_SELECTED_MUTE_DURATION_MINUTES] ?: 60
    }

    suspend fun setZenRuleId(value: String) {
        context.dataStore.edit { preferences ->
            preferences[ZEN_RULE_ID] = value
        }
    }

    suspend fun setIconTheme(value: Int) {
        context.dataStore.edit { preferences ->
            preferences[ICON_THEME] = value
        }
    }

    suspend fun saveMuteDurationMinutes(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[LAST_SELECTED_MUTE_DURATION_MINUTES] = minutes
        }
    }

    suspend fun saveTimer(endTime: Long, previousMode: Int) {
        context.dataStore.edit { preferences ->
            preferences[TIMER_END_TIME] = endTime
            preferences[PREVIOUS_RINGER_MODE] = previousMode
        }
    }

    suspend fun clearTimer() {
        context.dataStore.edit { preferences ->
            preferences[TIMER_END_TIME] = 0L
            //preferences[PREVIOUS_RINGER_MODE] = -1
        }
    }
}

package com.curryplayer.quicksettingssoundprofile.data

import android.content.Context
import android.media.AudioManager
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.curryplayer.quicksettingssoundprofile.models.MuteDurationOption
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
        val LAST_SELECTED_MUTE_OPTION = intPreferencesKey("last_selected_mute_option")
        val CUSTOM_MUTE_DURATION_MINUTES = intPreferencesKey("custom_mute_duration_minutes")
        val TILE_ADDED = booleanPreferencesKey("tile_added")
        val SHOW_TIMER_END_TIME = booleanPreferencesKey("show_timer_end_time")
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

    /**
     * Cold [Flow] emitting the previous ringer mode and subsequent updates.
     * Emits -1 if no previous mode is set.
     */
    val previousRingerMode: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PREVIOUS_RINGER_MODE] ?: -1
    }

    /**
     * Cold [Flow] emitting the last selected mute duration in minutes and subsequent updates.
     * Emits 60 if no previous duration is set.
     */
    val lastMuteDurationMinutes: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[LAST_SELECTED_MUTE_DURATION_MINUTES] ?: MuteDurationOption.MINUTES_60.minutes
    }

    /**
     * Cold [Flow] emitting the last selected mute duration option and subsequent updates.
     * Falls back to deriving the option from [LAST_SELECTED_MUTE_DURATION_MINUTES] if not explicitly saved yet.
     */
    val lastMuteDurationOption: Flow<MuteDurationOption> = context.dataStore.data.map { preferences ->
        val optionOrdinal = preferences[LAST_SELECTED_MUTE_OPTION]
        if (optionOrdinal != null) {
            MuteDurationOption.fromOrdinal(optionOrdinal)
        } else {
            val minutes = preferences[LAST_SELECTED_MUTE_DURATION_MINUTES] ?: MuteDurationOption.MINUTES_60.minutes
            MuteDurationOption.fromMinutes(minutes)
        }
    }

    /**
     * Cold [Flow] emitting the saved custom mute duration in minutes and subsequent updates.
     * Emits 42 (the answer to everything) if no custom duration has been set yet.
     */
    val customMuteDurationMinutes: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[CUSTOM_MUTE_DURATION_MINUTES] ?: MuteDurationOption.CUSTOM.minutes
    }

    /**
     * Cold [Flow] emitting whether the Quick Settings tile is currently placed by the user.
     * Android offers no API to query this, so the flag is maintained by the tile service
     * (onTileAdded / onTileRemoved / onStartListening) and by the result of the add tile request.
     */
    val tileAdded: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[TILE_ADDED] ?: false
    }

    /**
     * Cold [Flow] emitting whether the tile subtitle shows the end time of the temporary mute
     * (e.g. "Until 12:49") instead of the remaining duration (e.g. "1 h 5 m left").
     * Emits false (remaining duration) by default.
     */
    val showTimerEndTime: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SHOW_TIMER_END_TIME] ?: false
    }

    suspend fun setShowTimerEndTime(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SHOW_TIMER_END_TIME] = value
        }
    }

    suspend fun setTileAdded(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[TILE_ADDED] = value
        }
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

    suspend fun saveMuteDurationOption(option: MuteDurationOption) {
        context.dataStore.edit { preferences ->
            preferences[LAST_SELECTED_MUTE_OPTION] = option.ordinal
        }
    }

    suspend fun saveCustomMuteDurationMinutes(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[CUSTOM_MUTE_DURATION_MINUTES] = minutes
        }
    }

    suspend fun setPreviousRingerMode(mode: Int) {
        // only accept normal and vibrate mode as previous ringer mode
        if (isValidPreviousRingerMode(mode)) {
            context.dataStore.edit { preferences ->
                preferences[PREVIOUS_RINGER_MODE] = mode
            }
        }
    }

    suspend fun saveTimer(endTime: Long, previousMode: Int) {
        context.dataStore.edit { preferences ->
            preferences[TIMER_END_TIME] = endTime
            // only accept normal and vibrate mode as previous ringer mode
            if (isValidPreviousRingerMode(previousMode)) {
                preferences[PREVIOUS_RINGER_MODE] = previousMode
            } else if (preferences[PREVIOUS_RINGER_MODE] == null) {
                preferences[PREVIOUS_RINGER_MODE] = AudioManager.RINGER_MODE_NORMAL
            }
        }
    }

    suspend fun clearTimer() {
        context.dataStore.edit { preferences ->
            preferences[TIMER_END_TIME] = 0L
        }
    }

    private fun isValidPreviousRingerMode(mode: Int): Boolean {
        return mode == AudioManager.RINGER_MODE_NORMAL || mode == AudioManager.RINGER_MODE_VIBRATE
    }
}

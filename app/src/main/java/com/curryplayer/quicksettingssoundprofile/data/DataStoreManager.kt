package com.curryplayer.quicksettingssoundprofile.data

import android.content.Context
import android.content.Context.AUDIO_SERVICE
import android.media.AudioManager
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class DataStoreManager(private val context: Context) {
    companion object {
        val ACTIVATE_DND = booleanPreferencesKey("activate_dnd")
        val MUTE_MEDIA = booleanPreferencesKey("mute_media")
        val VOLUME_LEVEL = intPreferencesKey("volume_level")
    }

    val activateDnd: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[ACTIVATE_DND] ?: false
    }

    val muteMedia: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[MUTE_MEDIA] ?: false
    }

    val volumeLevel: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[VOLUME_LEVEL] ?: run {
            val audioManager = context.getSystemService(AUDIO_SERVICE) as AudioManager
            audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        }
    }

    suspend fun setActivateDnd(value: Boolean) {
        Log.i("DataStore", "Setting DnD now to $value")
        context.dataStore.edit { preferences ->
            preferences[ACTIVATE_DND] = value
        }
    }

    suspend fun setMuteMedia(value: Boolean) {
        Log.i("DataStore", "Setting muteMedia now to $value")
        context.dataStore.edit { preferences ->
            preferences[MUTE_MEDIA] = value
        }
    }

    suspend fun setVolumeLevel(value: Int) {
        Log.i("DataStore", "Setting volumeLevel now to $value")
        context.dataStore.edit { preferences ->
            preferences[VOLUME_LEVEL] = value
        }
    }
}

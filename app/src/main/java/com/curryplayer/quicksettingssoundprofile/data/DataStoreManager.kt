package com.curryplayer.quicksettingssoundprofile.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class DataStoreManager(private val context: Context) {
    companion object {
        val ZEN_RULE_ID = stringPreferencesKey("zen_rule_id")
    }

    val zenRuleId: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[ZEN_RULE_ID] ?: ""
    }

    suspend fun setZenRuleId(value: String) {
        context.dataStore.edit { preferences ->
            preferences[ZEN_RULE_ID] = value
        }
    }
}

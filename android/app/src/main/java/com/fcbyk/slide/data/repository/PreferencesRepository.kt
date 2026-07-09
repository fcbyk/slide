package com.fcbyk.slide.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "slide_prefs")

class PreferencesRepository(private val context: Context) {

    companion object {
        private val KEY_SERVER_URL = stringPreferencesKey("server_url")
        private val KEY_DARK_MODE = booleanPreferencesKey("dark_mode")
    }

    val serverUrl: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_SERVER_URL] ?: ""
    }

    val isDarkMode: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_DARK_MODE] ?: false
    }

    suspend fun setServerUrl(url: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SERVER_URL] = url.trimEnd('/')
        }
    }

    suspend fun setDarkMode(dark: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DARK_MODE] = dark
        }
    }
}

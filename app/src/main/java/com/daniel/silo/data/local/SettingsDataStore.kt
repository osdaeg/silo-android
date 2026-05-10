package com.daniel.silo.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "silo_prefs")

data class SiloSettings(
    val serverUrl: String = "http://192.168.88.100:7123",
    val apiToken: String = ""
)

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val SERVER_URL = stringPreferencesKey("server_url")
    private val API_TOKEN  = stringPreferencesKey("api_token")

    val settings: Flow<SiloSettings> = context.dataStore.data.map { prefs ->
        SiloSettings(
            serverUrl = prefs[SERVER_URL] ?: "http://192.168.88.100:7123",
            apiToken  = prefs[API_TOKEN]  ?: ""
        )
    }

    suspend fun saveSettings(settings: SiloSettings) {
        context.dataStore.edit { prefs ->
            prefs[SERVER_URL] = settings.serverUrl
            prefs[API_TOKEN]  = settings.apiToken
        }
    }
}

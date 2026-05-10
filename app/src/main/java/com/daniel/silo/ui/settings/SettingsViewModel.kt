package com.daniel.silo.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daniel.silo.data.local.SettingsDataStore
import com.daniel.silo.data.local.SiloSettings
import com.daniel.silo.domain.repository.SiloRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val store: SettingsDataStore,
    private val repository: SiloRepository
) : ViewModel() {

    val settings = store.settings.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), SiloSettings()
    )

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage = _syncMessage.asStateFlow()

    fun save(serverUrl: String, apiToken: String) = viewModelScope.launch {
        store.saveSettings(SiloSettings(serverUrl.trim(), apiToken.trim()))
    }

    fun syncNow() = viewModelScope.launch {
        _syncMessage.value = "Sincronizando..."
        try {
            repository.syncPending()
            repository.refreshAll()
            _syncMessage.value = "OK"
        } catch (e: Exception) {
            _syncMessage.value = "Error: " + e.message
        }
    }

    fun clearSyncMessage() { _syncMessage.value = null }
}

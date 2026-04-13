package com.daniel.silo.ui.links

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daniel.silo.domain.model.SiloCollection
import com.daniel.silo.domain.repository.SiloRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddLinkUiState(
    val url: String = "",
    val title: String = "",
    val description: String = "",
    val selectedCollectionId: Long? = null,
    val collections: List<SiloCollection> = emptyList(),
    val isFetchingTitle: Boolean = false,
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val savedOffline: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AddLinkViewModel @Inject constructor(
    private val repository: SiloRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AddLinkUiState())
    val state: StateFlow<AddLinkUiState> = _state

    init {
        viewModelScope.launch {
            repository.observeCollections().collect { cols: List<SiloCollection> ->
                _state.update { it.copy(collections = cols) }
            }
        }
    }

    fun setUrl(url: String) {
        _state.update { it.copy(url = url) }
        if (url.startsWith("http") && url.length > 10) fetchTitle(url)
    }
    fun setTitle(t: String) = _state.update { it.copy(title = t) }
    fun setDescription(d: String) = _state.update { it.copy(description = d) }
    fun setCollection(id: Long?) = _state.update { it.copy(selectedCollectionId = id) }

    private fun fetchTitle(url: String) = viewModelScope.launch {
        _state.update { it.copy(isFetchingTitle = true) }
        val t = repository.fetchTitle(url)
        _state.update { it.copy(title = t ?: it.title, isFetchingTitle = false) }
    }

    fun save() = viewModelScope.launch {
        val s = _state.value
        if (s.url.isBlank()) return@launch
        _state.update { it.copy(isSaving = true) }
        repository.addLink(s.url.trim(), s.title.ifBlank { null }, s.description.ifBlank { null }, s.selectedCollectionId)
            .fold(
                onSuccess = { _state.update { it.copy(isSaving = false, saved = true) } },
                onFailure = { _state.update { it.copy(isSaving = false, savedOffline = true) } }
            )
    }

    fun prefill(url: String, title: String? = null) {
        _state.update { it.copy(url = url, title = title ?: "") }
        if (title == null && url.startsWith("http")) fetchTitle(url)
    }
}

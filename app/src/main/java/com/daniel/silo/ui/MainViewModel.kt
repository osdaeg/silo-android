package com.daniel.silo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.daniel.silo.domain.model.Link
import com.daniel.silo.domain.model.SiloCollection
import com.daniel.silo.domain.model.SyncResult
import com.daniel.silo.domain.repository.SiloRepository
import com.daniel.silo.sync.SyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LinksUiState(
    val links: List<Link> = emptyList(),
    val collections: List<SiloCollection> = emptyList(),
    val selectedCollectionId: Long? = null,
    val query: String = "",
    val pendingCount: Int = 0,
    val isLoading: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: SiloRepository
) : ViewModel() {

    private val _selectedCollectionId = MutableStateFlow<Long?>(null)
    private val _query = MutableStateFlow("")
    private val _isLoading = MutableStateFlow(false)
    private val _message = MutableStateFlow<String?>(null)

    private val _links = _selectedCollectionId.flatMapLatest { colId ->
        _query.flatMapLatest { q -> repository.observeLinks(colId, q) }
    }

    val uiState: StateFlow<LinksUiState> = combine(
        combine(_links, repository.observeCollections(), _selectedCollectionId) { links, cols, colId ->
            Triple(links, cols, colId)
        },
        combine(_query, repository.observePendingCount(), _isLoading, _message) { q, pending, loading, msg ->
            listOf<Any?>(q, pending, loading, msg)
        }
    ) { (links, cols, colId), rest ->
        @Suppress("UNCHECKED_CAST")
        LinksUiState(
            links       = links as List<Link>,
            collections = cols  as List<SiloCollection>,
            selectedCollectionId = colId as Long?,
            query       = rest[0] as String,
            pendingCount = rest[1] as Int,
            isLoading   = rest[2] as Boolean,
            message     = rest[3] as String?
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LinksUiState())

    init {
        refresh()
    }

    fun selectCollection(id: Long?) { _selectedCollectionId.value = id }
    fun setQuery(q: String) { _query.value = q }

    fun refresh() = viewModelScope.launch {
        _isLoading.value = true
        repository.refreshAll()
        _isLoading.value = false
    }

    fun deleteLink(id: Long) = viewModelScope.launch {
        repository.deleteLink(id).onFailure { _message.value = "Error al borrar" }
        _message.value = "Enlace borrado"
        clearMessageDelayed()
    }

    fun moveLink(id: Long, collectionId: Long?, collectionName: String?) = viewModelScope.launch {
        repository.moveLink(id, collectionId, collectionName)
            .onFailure { _message.value = "Error al mover" }
        clearMessageDelayed()
    }

    fun addCollection(name: String) = viewModelScope.launch {
        repository.addCollection(name).onFailure { _message.value = "Error al crear colección" }
    }

    fun deleteCollection(id: Long) = viewModelScope.launch {
        repository.deleteCollection(id).onFailure { _message.value = "Error al borrar colección" }
    }

    fun clearMessage() { _message.value = null }

    private fun clearMessageDelayed() = viewModelScope.launch {
        kotlinx.coroutines.delay(3000)
        _message.value = null
    }
}

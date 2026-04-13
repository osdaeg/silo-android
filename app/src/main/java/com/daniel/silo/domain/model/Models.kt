package com.daniel.silo.domain.model

data class SiloCollection(
    val id: Long,
    val name: String,
    val createdAt: String
)

data class Link(
    val id: Long,
    val url: String,
    val title: String,
    val description: String?,
    val collectionId: Long?,
    val collectionName: String?,
    val syncedToRaindrop: Boolean,
    val createdAt: String,
    val pendingSync: Boolean = false,
    val pendingDelete: Boolean = false
)

sealed class SyncResult {
    data class Success(val synced: Int) : SyncResult()
    data class Error(val message: String) : SyncResult()
}

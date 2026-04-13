package com.daniel.silo.domain.repository

import com.daniel.silo.data.local.*
import com.daniel.silo.data.remote.*
import com.daniel.silo.domain.model.Link
import com.daniel.silo.domain.model.SiloCollection
import com.daniel.silo.domain.model.SyncResult
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SiloRepository @Inject constructor(
    private val linkDao: LinkDao,
    private val collectionDao: CollectionDao,
    private val apiProvider: ApiProvider
) {
    fun observeLinks(collectionId: Long? = null, query: String = ""): Flow<List<Link>> {
        val q = "%$query%"
        return when {
            collectionId != null && query.isNotBlank() -> linkDao.observeSearchInCollection(collectionId, q)
            collectionId != null -> linkDao.observeByCollection(collectionId)
            query.isNotBlank() -> linkDao.observeSearch(q)
            else -> linkDao.observeAll()
        }.map { it.map(LinkEntity::toDomain) }
    }

    fun observeCollections(): Flow<List<SiloCollection>> =
        collectionDao.observeAll().map { it.map(CollectionEntity::toDomain) }

    fun observePendingCount(): Flow<Int> = linkDao.observePendingCount()

    suspend fun refreshAll(): Result<Unit> = runCatching {
        val api = apiProvider.api()
        val links = api.getLinks().bodyOrThrow()
        val collections = api.getCollections().bodyOrThrow()
        collectionDao.deleteAll()
        collectionDao.upsertAll(collections.map(CollectionDto::toEntity))
        val pending = linkDao.getPending()
        val serverIds = links.map { it.id }.toSet()
        linkDao.deleteAllSynced()
        linkDao.upsertAll(links.map(LinkDto::toEntity))
        pending.filter { it.id !in serverIds }.forEach { linkDao.upsert(it) }
    }

    suspend fun addLink(url: String, title: String?, description: String?, collectionId: Long?): Result<Unit> = runCatching {
        val api = apiProvider.apiOrNull()
        if (api != null) {
            val created = api.createLink(CreateLinkBody(url, title, description, collectionId)).bodyOrThrow()
            linkDao.upsert(created.toEntity())
        } else {
            val minId = linkDao.getMinId() ?: 0L
            val tempId = if (minId < 0) minId - 1 else -1L
            val colName = collectionId?.let { collectionDao.getAll().find { c -> c.id == it }?.name }
            linkDao.upsert(LinkEntity(
                id = tempId, url = url, title = title ?: url,
                description = description, collectionId = collectionId,
                collectionName = colName, syncedToRaindrop = false,
                createdAt = "", pendingSync = true
            ))
        }
    }

    suspend fun deleteLink(id: Long): Result<Unit> = runCatching {
        val api = apiProvider.apiOrNull()
        when {
            api != null && id > 0 -> { api.deleteLink(id); linkDao.deleteById(id) }
            id < 0 -> linkDao.deleteById(id)
            else -> linkDao.markPendingDelete(id)
        }
    }

    suspend fun moveLink(id: Long, collectionId: Long?, collectionName: String?): Result<Unit> = runCatching {
        val api = apiProvider.apiOrNull()
        if (api != null && id > 0) {
            val updated = api.patchLink(id, PatchLinkBody(collectionId = collectionId)).bodyOrThrow()
            linkDao.upsert(updated.toEntity())
        } else {
            linkDao.updateCollection(id, collectionId, collectionName)
        }
    }

    suspend fun fetchTitle(url: String): String? = runCatching {
        apiProvider.api().fetchTitle(url).body()?.title
    }.getOrNull()

    suspend fun addCollection(name: String): Result<Unit> = runCatching {
        val created = apiProvider.api().createCollection(CreateCollectionBody(name)).bodyOrThrow()
        collectionDao.upsertAll(listOf(created.toEntity()))
    }

    suspend fun deleteCollection(id: Long): Result<Unit> = runCatching {
        apiProvider.api().deleteCollection(id)
        refreshAll().getOrThrow()
    }

    suspend fun syncPending(): SyncResult {
        val api = apiProvider.apiOrNull() ?: return SyncResult.Error("Sin conexion")
        val pending = linkDao.getPending()
        var synced = 0
        for (link in pending) {
            try {
                when {
                    link.pendingDelete -> { api.deleteLink(link.id); linkDao.deleteById(link.id); synced++ }
                    link.id < 0 -> {
                        val created = api.createLink(CreateLinkBody(link.url, link.title, link.description, link.collectionId)).bodyOrThrow()
                        linkDao.deleteById(link.id)
                        linkDao.upsert(created.toEntity())
                        synced++
                    }
                    link.pendingSync -> {
                        val updated = api.patchLink(link.id, PatchLinkBody(link.title, link.description, link.collectionId)).bodyOrThrow()
                        linkDao.upsert(updated.toEntity())
                        synced++
                    }
                }
            } catch (_: Exception) { }
        }
        return SyncResult.Success(synced)
    }
}

fun LinkDto.toEntity() = LinkEntity(id, url, title ?: url, description, collectionId, collectionName, syncedToRaindrop, createdAt)
fun LinkEntity.toDomain() = Link(id, url, title, description, collectionId, collectionName, syncedToRaindrop, createdAt, pendingSync, pendingDelete)
fun CollectionDto.toEntity() = CollectionEntity(id, name, createdAt)
fun CollectionEntity.toDomain() = SiloCollection(id, name, createdAt)

fun <T> retrofit2.Response<T>.bodyOrThrow(): T =
    if (isSuccessful) body()!! else error("HTTP " + code().toString())

package com.daniel.silo.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ── Entities ──────────────────────────────────────────────────────────────────

@Entity(tableName = "links")
data class LinkEntity(
    @PrimaryKey val id: Long,
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

@Entity(tableName = "collections")
data class CollectionEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val createdAt: String
)

// ── DAOs ──────────────────────────────────────────────────────────────────────

@Dao
interface LinkDao {

    @Query("SELECT * FROM links WHERE pendingDelete = 0 ORDER BY id DESC")
    fun observeAll(): Flow<List<LinkEntity>>

    @Query("SELECT * FROM links WHERE pendingDelete = 0 AND collectionId = :collectionId ORDER BY id DESC")
    fun observeByCollection(collectionId: Long): Flow<List<LinkEntity>>

    @Query("SELECT * FROM links WHERE pendingDelete = 0 AND (title LIKE :q OR url LIKE :q) ORDER BY id DESC")
    fun observeSearch(q: String): Flow<List<LinkEntity>>

    @Query("SELECT * FROM links WHERE pendingDelete = 0 AND collectionId = :collectionId AND (title LIKE :q OR url LIKE :q) ORDER BY id DESC")
    fun observeSearchInCollection(collectionId: Long, q: String): Flow<List<LinkEntity>>

    @Query("SELECT * FROM links WHERE pendingSync = 1 OR pendingDelete = 1")
    suspend fun getPending(): List<LinkEntity>

    @Query("SELECT COUNT(*) FROM links WHERE pendingSync = 1 OR pendingDelete = 1")
    fun observePendingCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(link: LinkEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(links: List<LinkEntity>)

    @Query("DELETE FROM links WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM links")
    suspend fun deleteAll()

    @Query("DELETE FROM links WHERE pendingSync = 0 AND pendingDelete = 0")
    suspend fun deleteAllSynced()

    @Query("UPDATE links SET pendingDelete = 1 WHERE id = :id")
    suspend fun markPendingDelete(id: Long)

    @Query("UPDATE links SET collectionId = :collectionId, collectionName = :collectionName, pendingSync = 1 WHERE id = :id")
    suspend fun updateCollection(id: Long, collectionId: Long?, collectionName: String?)

    // Offline-created links get a temporary negative id
    @Query("SELECT MIN(id) FROM links")
    suspend fun getMinId(): Long?
}

@Dao
interface CollectionDao {

    @Query("SELECT * FROM collections ORDER BY name ASC")
    fun observeAll(): Flow<List<CollectionEntity>>

    @Query("SELECT * FROM collections ORDER BY name ASC")
    suspend fun getAll(): List<CollectionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(collections: List<CollectionEntity>)

    @Query("DELETE FROM collections")
    suspend fun deleteAll()
}

// ── Database ──────────────────────────────────────────────────────────────────

@Database(
    entities = [LinkEntity::class, CollectionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class SiloDatabase : RoomDatabase() {
    abstract fun linkDao(): LinkDao
    abstract fun collectionDao(): CollectionDao
}

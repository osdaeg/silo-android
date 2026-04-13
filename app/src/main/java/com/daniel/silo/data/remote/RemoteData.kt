package com.daniel.silo.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.*

// ── DTOs ──────────────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class LinkDto(
    val id: Long,
    val url: String,
    val title: String?,
    val description: String?,
    @Json(name = "collection_id") val collectionId: Long?,
    @Json(name = "collection_name") val collectionName: String?,
    @Json(name = "synced_to_raindrop") val syncedToRaindrop: Boolean,
    @Json(name = "created_at") val createdAt: String
)

@JsonClass(generateAdapter = true)
data class CollectionDto(
    val id: Long,
    val name: String,
    @Json(name = "created_at") val createdAt: String
)

@JsonClass(generateAdapter = true)
data class CreateLinkBody(
    val url: String,
    val title: String?,
    val description: String?,
    @Json(name = "collection_id") val collectionId: Long?
)

@JsonClass(generateAdapter = true)
data class PatchLinkBody(
    val title: String? = null,
    val description: String? = null,
    @Json(name = "collection_id") val collectionId: Long? = null
)

@JsonClass(generateAdapter = true)
data class CreateCollectionBody(val name: String)

@JsonClass(generateAdapter = true)
data class FetchTitleDto(val title: String?)

@JsonClass(generateAdapter = true)
data class SyncResponseDto(val synced: Int, val status: String)

// ── API ───────────────────────────────────────────────────────────────────────

interface SiloApi {

    @GET("links")
    suspend fun getLinks(
        @Query("collection_id") collectionId: Long? = null,
        @Query("q") q: String? = null
    ): Response<List<LinkDto>>

    @POST("links")
    suspend fun createLink(@Body body: CreateLinkBody): Response<LinkDto>

    @PATCH("links/{id}")
    suspend fun patchLink(@Path("id") id: Long, @Body body: PatchLinkBody): Response<LinkDto>

    @DELETE("links/{id}")
    suspend fun deleteLink(@Path("id") id: Long): Response<Unit>

    @GET("links/fetch-title")
    suspend fun fetchTitle(@Query("url") url: String): Response<FetchTitleDto>

    @GET("collections")
    suspend fun getCollections(): Response<List<CollectionDto>>

    @POST("collections")
    suspend fun createCollection(@Body body: CreateCollectionBody): Response<CollectionDto>

    @DELETE("collections/{id}")
    suspend fun deleteCollection(@Path("id") id: Long): Response<Unit>

    @POST("sync")
    suspend fun sync(): Response<SyncResponseDto>
}

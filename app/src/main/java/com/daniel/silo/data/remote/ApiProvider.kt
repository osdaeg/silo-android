package com.daniel.silo.data.remote

import com.daniel.silo.data.local.SettingsDataStore
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiProvider @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) {
    private var cachedApi: SiloApi? = null
    private var cachedUrl: String = ""
    private var cachedToken: String = ""

    suspend fun api(): SiloApi {
        val settings = settingsDataStore.settings.first()
        if (cachedApi == null || settings.serverUrl != cachedUrl || settings.apiToken != cachedToken) {
            cachedUrl   = settings.serverUrl
            cachedToken = settings.apiToken
            cachedApi   = buildApi(cachedUrl, cachedToken)
        }
        return cachedApi!!
    }

    /** Returns null only if the server is unreachable (network error) */
    suspend fun apiOrNull(): SiloApi? = try {
        api().also { a ->
            // Only test TCP connectivity, not HTTP response
            a.getCollections()
        }
    } catch (e: ConnectException) { null }
      catch (e: SocketTimeoutException) { null }
      catch (e: java.net.UnknownHostException) { null }
      catch (e: javax.net.ssl.SSLException) { null }
      // HTTP errors (401, 500, etc) mean the server IS reachable -> return api
      catch (_: Exception) { api() }

    private fun buildApi(baseUrl: String, token: String): SiloApi {
        val url = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val client = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
                chain.proceed(req)
            }
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .build()
        return Retrofit.Builder()
            .baseUrl(url)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(SiloApi::class.java)
    }
}

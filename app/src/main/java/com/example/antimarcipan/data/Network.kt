package com.example.antimarcipan.data

import com.example.antimarcipan.data.edhrec.EdhrecApi
import com.example.antimarcipan.data.scryfall.ScryfallApi
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Builds the Retrofit services. Kept as a tiny hand-rolled singleton so the
 * scaffold has no DI dependency; swap for Hilt/Koin when the app grows.
 */
object Network {

    private const val SCRYFALL_BASE = "https://api.scryfall.com/"

    // EDHREC's community JSON host. Unofficial — no public contract, be gentle.
    private const val EDHREC_BASE = "https://json.edhrec.com/"

    private const val USER_AGENT = "MtgScanner/0.1 (personal hobby app)"

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    /** Adds required headers and enforces Scryfall's ~10 req/s rate limit. */
    private val politeInterceptor = Interceptor { chain ->
        // Simple throttle: never fire faster than one request per 100 ms.
        synchronized(lock) {
            val now = System.currentTimeMillis()
            val wait = lastRequestAt + MIN_INTERVAL_MS - now
            if (wait > 0) Thread.sleep(wait)
            lastRequestAt = System.currentTimeMillis()
        }
        val request = chain.request().newBuilder()
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
            .build()
        chain.proceed(request)
    }

    private val lock = Any()
    private var lastRequestAt = 0L
    private const val MIN_INTERVAL_MS = 100L

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(politeInterceptor)
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                }
            )
            .build()
    }

    private val contentType = "application/json".toMediaType()

    val scryfall: ScryfallApi by lazy {
        Retrofit.Builder()
            .baseUrl(SCRYFALL_BASE)
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(ScryfallApi::class.java)
    }

    val edhrec: EdhrecApi by lazy {
        Retrofit.Builder()
            .baseUrl(EDHREC_BASE)
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(EdhrecApi::class.java)
    }
}

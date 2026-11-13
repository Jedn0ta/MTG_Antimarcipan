package com.example.antimarcipan

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.decode.SvgDecoder
import coil.request.CachePolicy
import com.example.antimarcipan.data.CollectionRepository
import com.example.antimarcipan.data.DeckRepository
import com.example.antimarcipan.data.SynergyRepository
import com.example.antimarcipan.data.backup.BackupRepository
import com.example.antimarcipan.data.local.AppDatabase
import com.example.antimarcipan.data.settings.AppSettings
import com.example.antimarcipan.data.update.UpdateRepository

/** Hand-rolled dependency container — swap for Hilt/Koin as the app grows. */
class AppContainer(app: Application) {
    private val db = AppDatabase.get(app)
    val appSettings = AppSettings(app)
    val collectionRepository = CollectionRepository(db.collectionDao(), app.applicationContext)
    val deckRepository = DeckRepository(db.deckDao())
    val synergyRepository = SynergyRepository(db.collectionDao())
    val updateRepository = UpdateRepository(app)
    val backupRepository = BackupRepository(app, db)
}

class MtgApp : Application(), ImageLoaderFactory {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        installCrashLogger()
        container = AppContainer(this)
    }

    /** Explicitní Coil loader — vlastní OkHttp (bez rate-limiteru), cache, crossfade. */
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            // Scryfall odmítá požadavky s výchozí hlavičkou „okhttp" — posíláme vlastní.
            .okHttpClient {
                okhttp3.OkHttpClient.Builder()
                    .addInterceptor { chain ->
                        chain.proceed(
                            chain.request().newBuilder()
                                .header("User-Agent", "Antimarcipan/0.1 (personal hobby app)")
                                .header("Accept", "image/*,*/*;q=0.8")
                                .build()
                        )
                    }
                    .build()
            }
            .components { add(SvgDecoder.Factory()) }
            .crossfade(true)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.25).build() }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(256L * 1024 * 1024)
                    .build()
            }
            .build()

    /** Při pádu zapíše stack trace do souboru, aby šel zkopírovat v Nastavení → O aplikaci. */
    private fun installCrashLogger() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, e ->
            runCatching {
                val sw = java.io.StringWriter()
                e.printStackTrace(java.io.PrintWriter(sw))
                val stamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.ROOT).format(java.util.Date())
                java.io.File(filesDir, CRASH_LOG).appendText(
                    "=== $stamp · verze ${BuildConfig.VERSION_NAME} · Android ${android.os.Build.VERSION.RELEASE} · " +
                        "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} · vlákno ${thread.name}\n$sw\n"
                )
            }
            previous?.uncaughtException(thread, e)
        }
    }

    companion object {
        const val CRASH_LOG = "crash.log"
    }
}

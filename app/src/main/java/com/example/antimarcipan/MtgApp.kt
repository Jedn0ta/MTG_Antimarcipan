package com.example.antimarcipan

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.example.antimarcipan.data.CollectionRepository
import com.example.antimarcipan.data.DeckRepository
import com.example.antimarcipan.data.SynergyRepository
import com.example.antimarcipan.data.backup.BackupRepository
import com.example.antimarcipan.data.local.AppDatabase
import com.example.antimarcipan.data.shop.CernyRytirRepository
import com.example.antimarcipan.data.update.UpdateRepository

/** Hand-rolled dependency container — swap for Hilt/Koin as the app grows. */
class AppContainer(app: Application) {
    private val db = AppDatabase.get(app)
    val collectionRepository = CollectionRepository(db.collectionDao())
    val deckRepository = DeckRepository(db.deckDao())
    val synergyRepository = SynergyRepository(db.collectionDao())
    val updateRepository = UpdateRepository(app)
    val shopRepository = CernyRytirRepository()
    val backupRepository = BackupRepository(app, db)
}

class MtgApp : Application(), ImageLoaderFactory {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }

    /** Explicitní Coil loader — vlastní OkHttp (bez rate-limiteru), cache, crossfade. */
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
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
}

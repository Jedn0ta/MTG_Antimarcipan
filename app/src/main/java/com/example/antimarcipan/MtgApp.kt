package com.example.antimarcipan

import android.app.Application
import com.example.antimarcipan.data.CollectionRepository
import com.example.antimarcipan.data.DeckRepository
import com.example.antimarcipan.data.SynergyRepository
import com.example.antimarcipan.data.local.AppDatabase
import com.example.antimarcipan.data.update.UpdateRepository

/** Hand-rolled dependency container — swap for Hilt/Koin as the app grows. */
class AppContainer(app: Application) {
    private val db = AppDatabase.get(app)
    val collectionRepository = CollectionRepository(db.collectionDao())
    val deckRepository = DeckRepository(db.deckDao())
    val synergyRepository = SynergyRepository(db.collectionDao())
    val updateRepository = UpdateRepository(app)
}

class MtgApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

package com.example.antimarcipan.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [CollectionCard::class, Deck::class, DeckCard::class],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun collectionDao(): CollectionDao
    abstract fun deckDao(): DeckDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        /** v1 → v2: přidán sloupec coverImagePath u balíků. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE decks ADD COLUMN coverImagePath TEXT")
            }
        }

        /** v2 → v3: přidán sloupec localImagePath u karet (offline náhled). */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE collection_cards ADD COLUMN localImagePath TEXT")
            }
        }

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mtg-scanner.db",
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { instance = it }
            }
    }
}

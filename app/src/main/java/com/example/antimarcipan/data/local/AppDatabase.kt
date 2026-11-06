package com.example.antimarcipan.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [CollectionCard::class, Deck::class, DeckCard::class],
    version = 8,
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

        /** v3 → v4: přidán sloupec format u balíků. */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE decks ADD COLUMN format TEXT NOT NULL DEFAULT 'FREEFORM'")
            }
        }

        /** v4 → v5: přidán sloupec gathererUrl u karet. */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE collection_cards ADD COLUMN gathererUrl TEXT")
            }
        }

        /** v5 → v6: přidán sloupec producedMana u karet. */
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE collection_cards ADD COLUMN producedMana TEXT NOT NULL DEFAULT ''")
            }
        }

        /** v6 → v7: přidán sloupec colors u balíků (ručně zvolené barvy). */
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE decks ADD COLUMN colors TEXT")
            }
        }

        /** v7 → v8: příznaky owned / wishlist u karet. */
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE collection_cards ADD COLUMN owned INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE collection_cards ADD COLUMN wishlist INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mtg-scanner.db",
                ).addMigrations(
                    MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6,
                    MIGRATION_6_7, MIGRATION_7_8,
                ).build().also { instance = it }
            }
    }
}

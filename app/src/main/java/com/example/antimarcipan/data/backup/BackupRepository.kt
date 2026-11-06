package com.example.antimarcipan.data.backup

import android.content.Context
import android.net.Uri
import com.example.antimarcipan.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Ruční záloha a obnova dat (sbírka + balíky) do/z jednoho souboru.
 * Doplněk k automatické cloudové záloze — zaručená pojistka proti ztrátě
 * dat při odinstalaci nebo přenosu do jiného telefonu.
 */
class BackupRepository(
    private val context: Context,
    private val db: AppDatabase,
) {
    private val dbName = "mtg-scanner.db"

    /** Zapíše aktuální databázi do zvoleného souboru (SAF uri). */
    suspend fun exportTo(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            // Sloučí WAL do hlavního souboru, ať je záloha kompletní.
            db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(TRUNCATE)").close()
            val dbFile = context.getDatabasePath(dbName)
            context.contentResolver.openOutputStream(uri)?.use { out ->
                dbFile.inputStream().use { it.copyTo(out) }
            } ?: error("Nepodařilo se otevřít cílový soubor.")
            Unit
        }
    }

    /**
     * Obnoví databázi ze souboru. Po obnově je potřeba aplikaci restartovat,
     * aby se znovu otevřela nová databáze.
     */
    suspend fun importFrom(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(TRUNCATE)").close()
            db.close()
            val dbFile = context.getDatabasePath(dbName)
            // Smaž WAL/SHM, ať nekolidují se starým stavem.
            context.getDatabasePath("$dbName-wal").delete()
            context.getDatabasePath("$dbName-shm").delete()
            context.contentResolver.openInputStream(uri)?.use { input ->
                dbFile.outputStream().use { input.copyTo(it) }
            } ?: error("Nepodařilo se otevřít zálohu.")
            Unit
        }
    }

    fun suggestedFileName(): String {
        val ts = java.text.SimpleDateFormat("yyyyMMdd-HHmm", java.util.Locale.US)
            .format(java.util.Date())
        return "antimarcipan-zaloha-$ts.db"
    }
}

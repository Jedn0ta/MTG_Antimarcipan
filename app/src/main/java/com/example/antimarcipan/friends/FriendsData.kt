package com.example.antimarcipan.friends

import android.util.Base64
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import com.example.antimarcipan.data.local.CollectionCard
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.Inflater

@Entity(tableName = "friends")
data class Friend(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val addedAt: Long = System.currentTimeMillis(),
)

/** Balík od kamaráda — uložený jako snímek (JSON), nezávislý na mé sbírce. */
@Entity(tableName = "friend_decks", indices = [Index("friendId")])
data class FriendDeck(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val friendId: Long,
    val name: String,
    val format: String,
    val json: String,
    val receivedAt: Long = System.currentTimeMillis(),
)

@Dao
interface FriendDao {
    @Query("SELECT * FROM friends ORDER BY name COLLATE NOCASE")
    fun friends(): Flow<List<Friend>>

    @Query("SELECT * FROM friend_decks ORDER BY receivedAt DESC")
    fun decks(): Flow<List<FriendDeck>>

    @Query("SELECT * FROM friends WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun findByName(name: String): Friend?

    @Insert
    suspend fun insertFriend(f: Friend): Long

    @Insert
    suspend fun insertDeck(d: FriendDeck): Long

    @Query("DELETE FROM friend_decks WHERE id = :id")
    suspend fun deleteDeck(id: Long)

    @Query("DELETE FROM friend_decks WHERE friendId = :id")
    suspend fun deleteDecksOf(id: Long)

    @Query("DELETE FROM friends WHERE id = :id")
    suspend fun deleteFriend(id: Long)

    @Query("UPDATE friends SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)
}

// ------------------------------------------------------------- Formát sdílení

@Serializable
data class SharedCard(
    val id: String,
    val n: String,
    val q: Int,
    val t: String? = null,
    val mv: Double? = null,
    val mc: String? = null,
    val ci: String = "",
    val kw: String = "",
    val o: String? = null,
    val img: String? = null,
    val cmd: Boolean = false,
)

@Serializable
data class SharedDeck(
    val v: Int = 1,
    val owner: String,
    val name: String,
    val format: String,
    val cards: List<SharedCard>,
) {
    /** Karty jako CollectionCard (pro náhledy, detail a rádce). */
    fun toCards(): List<Pair<CollectionCard, Int>> = cards.map { c ->
        CollectionCard(
            scryfallId = c.id, name = c.n, setCode = null, setName = null, collectorNumber = null, rarity = null,
            manaCost = c.mc, cmc = c.mv, typeLine = c.t, oracleText = c.o, colorIdentity = c.ci,
            keywords = c.kw, imageUrl = c.img, priceUsd = null, quantity = c.q, owned = false,
        ) to c.q
    }

    val total: Int get() = cards.sumOf { it.q }
}

object DeckShareCodec {
    private const val PREFIX = "AMDECK1:"
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }

    fun encode(deck: SharedDeck): String {
        val raw = json.encodeToString(SharedDeck.serializer(), deck).toByteArray()
        val d = Deflater(Deflater.BEST_COMPRESSION).apply { setInput(raw); finish() }
        val out = ByteArrayOutputStream()
        val buf = ByteArray(4096)
        while (!d.finished()) out.write(buf, 0, d.deflate(buf))
        return PREFIX + Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP or Base64.URL_SAFE)
    }

    /** Najde kód v libovolném textu (zpráva, schránka) a dekóduje ho. */
    fun decode(text: String): SharedDeck? = runCatching {
        val start = text.indexOf(PREFIX)
        if (start < 0) return null
        val code = text.substring(start + PREFIX.length).takeWhile { it.isLetterOrDigit() || it == '-' || it == '_' || it == '=' }
        val bytes = Base64.decode(code, Base64.NO_WRAP or Base64.URL_SAFE)
        val inf = Inflater().apply { setInput(bytes) }
        val out = ByteArrayOutputStream()
        val buf = ByteArray(4096)
        while (!inf.finished()) {
            val n = inf.inflate(buf)
            if (n == 0 && (inf.needsInput() || inf.needsDictionary())) break
            out.write(buf, 0, n)
        }
        json.decodeFromString(SharedDeck.serializer(), out.toString(Charsets.UTF_8.name()))
    }.getOrNull()

    fun parseJson(s: String): SharedDeck? = runCatching { json.decodeFromString(SharedDeck.serializer(), s) }.getOrNull()
    fun toJson(d: SharedDeck): String = json.encodeToString(SharedDeck.serializer(), d)
}

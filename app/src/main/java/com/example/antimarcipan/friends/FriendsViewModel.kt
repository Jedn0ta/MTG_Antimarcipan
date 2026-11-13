package com.example.antimarcipan.friends

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.antimarcipan.data.CollectionRepository
import com.example.antimarcipan.data.DeckRepository
import com.example.antimarcipan.data.local.Deck
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Sdílený text přijatý z jiné appky (Quick Share, messenger…) — čeká na import. */
object FriendsInbox {
    val pending = MutableStateFlow<String?>(null)
}

class FriendsViewModel(
    private val dao: FriendDao,
    private val deckRepo: DeckRepository,
    private val collectionRepo: CollectionRepository,
    private val ctx: Context,
) : ViewModel() {
    private val prefs = ctx.getSharedPreferences("friends", Context.MODE_PRIVATE)

    val friends: StateFlow<List<Friend>> = dao.friends().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val friendDecks: StateFlow<List<FriendDeck>> = dao.decks().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val myDecks: StateFlow<List<Deck>> = deckRepo.decks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val myName = MutableStateFlow(prefs.getString("myName", "") ?: "")
    fun setMyName(n: String) { myName.value = n.trim(); prefs.edit().putString("myName", n.trim()).apply() }

    /** Připraví text ke sdílení (čitelný seznam + kód pro import). */
    suspend fun shareText(deckId: Long): String {
        val deck = deckRepo.deck(deckId) ?: return ""
        val cards = deckRepo.deckCardsWithQty(deckId).first()
        val shared = SharedDeck(
            owner = myName.value.ifBlank { "Kamarád" },
            name = deck.name,
            format = deck.format,
            cards = cards.map { e ->
                val c = e.card
                SharedCard(
                    id = c.scryfallId, n = c.name, q = e.deckQuantity, t = c.typeLine, mv = c.cmc, mc = c.manaCost,
                    ci = c.colorIdentity, kw = c.keywords, o = c.oracleText, img = c.imageUrl,
                    cmd = c.scryfallId == deck.commanderScryfallId,
                )
            },
        )
        val list = cards.sortedBy { it.card.name }.joinToString("\n") { "${it.deckQuantity} ${it.card.name}" }
        return "Balík „${deck.name}“ od ${shared.owner} (${shared.total} karet) — otevři v appce Antimarcipán:\n\n$list\n\n" +
            DeckShareCodec.encode(shared)
    }

    /** Uloží přijatý balík ke kamarádovi (podle jména, případně založí nového). */
    fun importDeck(deck: SharedDeck, friendName: String, onDone: (String) -> Unit) {
        viewModelScope.launch {
            val name = friendName.trim().ifBlank { deck.owner }
            val fid = dao.findByName(name)?.id ?: dao.insertFriend(Friend(name = name))
            dao.insertDeck(FriendDeck(friendId = fid, name = deck.name, format = deck.format, json = DeckShareCodec.toJson(deck)))
            onDone(name)
        }
    }

    fun deleteDeck(id: Long) = viewModelScope.launch { dao.deleteDeck(id) }
    fun deleteFriend(id: Long) = viewModelScope.launch { dao.deleteDecksOf(id); dao.deleteFriend(id) }
    fun addFriend(name: String) = viewModelScope.launch { if (name.isNotBlank() && dao.findByName(name.trim()) == null) dao.insertFriend(Friend(name = name.trim())) }

    /** Rádce: jak hrát mým balíkem proti kamarádovu. */
    suspend fun matchup(opp: SharedDeck, myDeckId: Long): MatchupReport {
        val mine = deckRepo.deckCardsWithQty(myDeckId).first().map { it.card to it.deckQuantity }
        val coll = collectionRepo.cards.first()
        return MatchupAdvisor.analyze(opp.toCards(), mine, coll)
    }
}

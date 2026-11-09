package com.example.antimarcipan.data

import com.example.antimarcipan.data.local.CollectionCard
import com.example.antimarcipan.data.scryfall.ScryfallCard

fun ScryfallCard.toCollectionCard(quantity: Int = 1): CollectionCard =
    CollectionCard(
        scryfallId = id,
        name = name,
        setCode = set,
        setName = setName,
        collectorNumber = collectorNumber,
        rarity = rarity,
        manaCost = manaCost,
        cmc = cmc,
        typeLine = typeLine,
        oracleText = oracleText,
        colorIdentity = colorIdentity.joinToString(","),
        producedMana = producedMana.joinToString(","),
        keywords = keywords.joinToString(","),
        imageUrl = bestImageUrl,
        gathererUrl = relatedUris?.gatherer,
        priceUsd = prices?.usd,
        priceEur = prices?.eur,
        quantity = quantity,
    )

fun CollectionCard.colorIdentityList(): List<String> =
    colorIdentity.split(",").map { it.trim() }.filter { it.isNotEmpty() }

/** Model pro Coil: přednostně lokální soubor, jinak vzdálená URL. */
fun CollectionCard.thumbModel(): Any? =
    localImagePath?.let { java.io.File(it) } ?: imageUrl

/** Odkaz na kartu na Gathereru; když konkrétní tisk chybí, hledání podle jména. */
fun CollectionCard.gathererLink(): String {
    gathererUrl?.takeIf { it.isNotBlank() }?.let { return it }
    val q = java.net.URLEncoder.encode("[\"$name\"]", "UTF-8")
    return "https://gatherer.wizards.com/Pages/Search/Default.aspx?name=$q"
}

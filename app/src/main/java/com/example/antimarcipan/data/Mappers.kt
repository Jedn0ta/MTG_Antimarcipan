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
        keywords = keywords.joinToString(","),
        imageUrl = bestImageUrl,
        priceUsd = prices?.usd,
        quantity = quantity,
    )

fun CollectionCard.colorIdentityList(): List<String> =
    colorIdentity.split(",").map { it.trim() }.filter { it.isNotEmpty() }

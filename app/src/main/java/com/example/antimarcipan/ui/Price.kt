package com.example.antimarcipan.ui

import com.example.antimarcipan.data.local.CollectionCard

/** Cena kusu: přednostně EUR (Cardmarket), jinak USD. */
fun CollectionCard.priceLabel(): String? {
    priceEur?.toDoubleOrNull()?.let { return "%.2f €".format(it) }
    priceUsd?.toDoubleOrNull()?.let { return "$%.2f".format(it) }
    return null
}

/** Celková cena seznamu (kus × počet), EUR + případně USD u karet bez ceny v EUR. */
fun totalPriceLabel(items: List<Pair<CollectionCard, Int>>): String? {
    var eur = 0.0; var usd = 0.0
    items.forEach { (c, q) ->
        val e = c.priceEur?.toDoubleOrNull()
        if (e != null) eur += e * q else c.priceUsd?.toDoubleOrNull()?.let { usd += it * q }
    }
    val parts = buildList {
        if (eur > 0) add("%.2f €".format(eur))
        if (usd > 0) add("$%.2f".format(usd))
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" + ")
}

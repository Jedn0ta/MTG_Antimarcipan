package com.example.antimarcipan.data.edhrec

/** Converts a card name into the slug EDHREC uses in its URLs. */
object Slug {

    fun commanderSlug(name: String): String {
        // For double-faced / split commanders EDHREC uses only the front name.
        val front = name.substringBefore("//").trim()
        return front
            .lowercase()
            .replace("'", "")
            .replace(",", "")
            .replace(".", "")
            .replace("&", "and")
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
    }
}

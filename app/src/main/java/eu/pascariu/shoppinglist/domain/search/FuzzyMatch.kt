package eu.pascariu.shoppinglist.domain.search

import java.text.Normalizer

fun fuzzyMatch(query: String, name: String): Boolean {
    if (query.isEmpty()) return true
    val normalizedQuery = Normalizer.normalize(query, Normalizer.Form.NFC)
    val normalizedName = Normalizer.normalize(name, Normalizer.Form.NFC)
    var nameIndex = 0
    for (queryChar in normalizedQuery) {
        if (queryChar.isWhitespace()) continue
        while (nameIndex < normalizedName.length && !normalizedName[nameIndex].equals(queryChar, ignoreCase = true)) {
            nameIndex++
        }
        if (nameIndex == normalizedName.length) return false
        nameIndex++
    }
    return true
}

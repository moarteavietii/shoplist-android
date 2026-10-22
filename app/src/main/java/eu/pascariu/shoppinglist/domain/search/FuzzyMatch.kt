package eu.pascariu.shoppinglist.domain.search

fun fuzzyMatch(query: String, name: String): Boolean {
    if (query.isEmpty()) return true
    var nameIndex = 0
    for (queryChar in query) {
        if (queryChar.isWhitespace()) continue
        while (nameIndex < name.length && !name[nameIndex].equals(queryChar, ignoreCase = true)) {
            nameIndex++
        }
        if (nameIndex == name.length) return false
        nameIndex++
    }
    return true
}

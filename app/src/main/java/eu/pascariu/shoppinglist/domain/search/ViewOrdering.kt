package eu.pascariu.shoppinglist.domain.search

import eu.pascariu.shoppinglist.data.model.Shop

internal const val GLOBAL_VIEW_NAME = "Global"

/** Sentinel id for the display-only Global row shown when the fetched shops lack a Global shop. */
const val GLOBAL_FALLBACK_ID = -2L

fun orderViewsGlobalFirst(shops: List<Shop>): List<Shop> =
    shops.sortedWith(
        compareByDescending<Shop> { it.name == GLOBAL_VIEW_NAME }
            .thenBy { it.name.lowercase() }
            .thenBy { it.name },
    )

/** Shop list for the item modals' visibility checklist: Global first, with a display-only Global when missing. */
fun modalAvailableViews(shops: List<Shop>): List<Shop> {
    val ordered = orderViewsGlobalFirst(shops)
    if (ordered.any { it.name == GLOBAL_VIEW_NAME }) return ordered
    return listOf(Shop(id = GLOBAL_FALLBACK_ID, name = GLOBAL_VIEW_NAME, createdAt = "")) + ordered
}

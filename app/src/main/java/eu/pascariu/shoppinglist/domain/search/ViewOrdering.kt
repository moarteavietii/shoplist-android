package eu.pascariu.shoppinglist.domain.search

import eu.pascariu.shoppinglist.data.model.Shop

internal const val GLOBAL_VIEW_NAME = "Global"

fun orderViewsGlobalFirst(shops: List<Shop>): List<Shop> =
    shops.sortedWith(
        compareByDescending<Shop> { it.name == GLOBAL_VIEW_NAME }
            .thenBy { it.name.lowercase() }
            .thenBy { it.name },
    )

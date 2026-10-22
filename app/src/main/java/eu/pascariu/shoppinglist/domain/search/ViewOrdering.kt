package eu.pascariu.shoppinglist.domain.search

import eu.pascariu.shoppinglist.data.model.Shop

private const val GLOBAL_SHOP_NAME = "Global"

fun orderViewsGlobalFirst(shops: List<Shop>): List<Shop> =
    shops.sortedWith(
        compareByDescending<Shop> { it.name == GLOBAL_SHOP_NAME }
            .thenBy { it.name.lowercase() }
            .thenBy { it.name },
    )

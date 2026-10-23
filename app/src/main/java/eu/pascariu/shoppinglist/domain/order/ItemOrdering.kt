package eu.pascariu.shoppinglist.domain.order

import eu.pascariu.shoppinglist.data.model.Item
import eu.pascariu.shoppinglist.data.prefs.SortOrder

fun orderItems(items: List<Item>, viewId: Long?, order: SortOrder): List<Item> = when (order) {
    SortOrder.MANUAL -> sortManual(items, viewId)
    SortOrder.ALPHABETICAL -> items.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
    SortOrder.FREQUENCY -> items.sortedWith(
        compareByDescending<Item> { it.checkCount }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name },
    )
}

private fun sortManual(items: List<Item>, viewId: Long?): List<Item> {
    if (viewId == null) return items
    return items.sortedWith(
        compareBy<Item> { it.orderMap[viewId.toString()] ?: "\uFFFF" }
            .thenBy { it.id },
    )
}

fun dropKeyForIndex(orderedKeys: List<String>, dropIndex: Int): String {
    val before = orderedKeys.getOrNull(dropIndex - 1)
    val after = orderedKeys.getOrNull(dropIndex)
    return generateKeyBetween(before, after)
}

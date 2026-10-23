package eu.pascariu.shoppinglist.data.realtime

import eu.pascariu.shoppinglist.data.model.Item
import eu.pascariu.shoppinglist.data.model.ItemEvent
import eu.pascariu.shoppinglist.data.model.Shop

data class ChangeToastLabel(
    val text: String,
)

fun deriveChangeToasts(
    event: ItemEvent,
    previousItem: Item?,
    shops: List<Shop>,
): List<ChangeToastLabel> {
    return when (event.type) {
        "item:created" -> deriveCreated(event.item, previousItem)
        "item:updated" -> deriveUpdated(event.item, previousItem, shops)
        "item:deleted" -> deriveDeleted(previousItem, shops)
        else -> emptyList()
    }
}

private fun deriveCreated(item: Item?, previousItem: Item?): List<ChangeToastLabel> {
    item ?: return emptyList()
    if (previousItem == item) return emptyList()
    return listOf(ChangeToastLabel("new item: ${item.name}"))
}

private fun deriveDeleted(previousItem: Item?, shops: List<Shop>): List<ChangeToastLabel> {
    previousItem ?: return emptyList()
    return listOf(ChangeToastLabel("deleted: ${previousItem.name}"))
}

private fun deriveUpdated(item: Item?, previousItem: Item?, shops: List<Shop>): List<ChangeToastLabel> {
    item ?: return emptyList()
    previousItem ?: return emptyList()
    if (item == previousItem) return emptyList()

    val labels = mutableListOf<ChangeToastLabel>()
    if (item.isChecked != previousItem.isChecked) {
        labels += ChangeToastLabel(
            if (item.isChecked) "checked: ${item.name}" else "unchecked: ${item.name}",
        )
    }
    if (item.name != previousItem.name) {
        labels += ChangeToastLabel("${previousItem.name} → ${item.name}")
    }
    if (item.quantity != previousItem.quantity) {
        labels += ChangeToastLabel("quantity: ${previousItem.quantity} → ${item.quantity} ${item.name}")
    }
    deriveVisibilityLabels(previousItem.visibleIn, item.visibleIn, item.name, shops)
        .let(labels::addAll)
    return labels
}

private fun deriveVisibilityLabels(
    previousVisibleIn: List<Long>,
    newVisibleIn: List<Long>,
    itemName: String,
    shops: List<Shop>,
): List<ChangeToastLabel> {
    val shopNameById = shops.associate { it.id to it.name }
    val removed = previousVisibleIn.filterNot { it in newVisibleIn }
    val added = newVisibleIn.filterNot { it in previousVisibleIn }
    val labels = mutableListOf<ChangeToastLabel>()
    for (shopId in removed) {
        val shopName = shopNameById[shopId] ?: continue
        labels += ChangeToastLabel("$itemName removed from $shopName")
    }
    for (shopId in added) {
        val shopName = shopNameById[shopId] ?: continue
        labels += ChangeToastLabel("$itemName added to $shopName")
    }
    return labels
}

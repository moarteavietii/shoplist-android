package eu.pascariu.shoppinglist.ui.list

import eu.pascariu.shoppinglist.data.model.Item
import eu.pascariu.shoppinglist.data.model.Shop

data class ListUiState(
    val items: List<Item> = emptyList(),
    val watermarkName: String = "Global",
    val searchQuery: String = "",
    val filteredItems: List<Item> = emptyList(),
    val showAddAffordance: Boolean = false,
    val showUndoBar: Boolean = false,
    val addItemModal: AddItemUiState? = null,
    val postSaveSignal: PostSaveSignal? = null,
    val errorMessage: String? = null,
)

data class PostSaveSignal(val id: Long)

data class AddItemUiState(
    val title: String,
    val availableViews: List<Shop>,
    val selectedViewIds: Set<Long>,
) {
    val allViewsSelected: Boolean get() = availableViews.all { it.id in selectedViewIds }
    val canSave: Boolean get() = title.isNotBlank()
}

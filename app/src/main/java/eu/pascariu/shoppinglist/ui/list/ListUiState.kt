package eu.pascariu.shoppinglist.ui.list

import eu.pascariu.shoppinglist.data.model.Item
import eu.pascariu.shoppinglist.data.model.Shop
import eu.pascariu.shoppinglist.data.prefs.SortOrder
import eu.pascariu.shoppinglist.ui.drawer.DrawerShop

data class ListUiState(
    val items: List<Item> = emptyList(),
    val watermarkName: String = "Global",
    val activeViewId: Long? = null,
    val isWidgetStackExpanded: Boolean = false,
    val sortModeEnabled: Boolean = true,
    val sortOrder: SortOrder = SortOrder.MANUAL,
    val dragEnabled: Boolean = false,
    val uncheckedCountByShop: Map<Long, Int> = emptyMap(),
    val drawerShops: List<DrawerShop> = emptyList(),
    val drawerOpen: Boolean = false,
    val searchQuery: String = "",
    val searchExpanded: Boolean = false,
    val showChecked: Boolean = true,
    val hideChecked: Boolean = false,
    val filteredItems: List<Item> = emptyList(),
    val showAddAffordance: Boolean = false,
    val showUndoBar: Boolean = false,
    val addItemModal: AddItemUiState? = null,
    val editItemModal: EditItemUiState? = null,
    val deleteConfirmItem: Item? = null,
    val shopModal: ShopModalUiState? = null,
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

data class EditItemUiState(
    val itemId: Long,
    val originalName: String,
    val name: String,
    val availableViews: List<Shop>,
    val selectedViewIds: Set<Long>,
    val originalVisibleIn: Set<Long> = selectedViewIds,
) {
    val allViewsSelected: Boolean get() = availableViews.all { it.id in selectedViewIds }
    val isUnchanged: Boolean get() = name.trim() == originalName.trim() && selectedViewIds == originalVisibleIn
    val canSave: Boolean get() = name.isNotBlank() && !isUnchanged
}

data class ShopModalUiState(
    val mode: ShopModalMode,
    val shopId: Long? = null,
    val shopName: String = "",
    val name: String = "",
    val saving: Boolean = false,
    val error: String? = null,
    val deleteConfirmOpen: Boolean = false,
) {
    val trimmedName: String get() = name.trim()
    val isUnchanged: Boolean get() = mode == ShopModalMode.EDIT && trimmedName == shopName.trim()
    val canSave: Boolean get() = trimmedName.isNotEmpty() && !isUnchanged && !saving
    val secondaryLabel: String
        get() = if (mode == ShopModalMode.EDIT && trimmedName == shopName.trim()) "Close" else "Discard"
}

enum class ShopModalMode { CREATE, EDIT }

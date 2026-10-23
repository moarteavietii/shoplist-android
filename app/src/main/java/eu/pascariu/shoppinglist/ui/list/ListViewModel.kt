package eu.pascariu.shoppinglist.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import eu.pascariu.shoppinglist.data.model.CreateItemRequest
import eu.pascariu.shoppinglist.data.model.Item
import eu.pascariu.shoppinglist.data.model.Shop
import eu.pascariu.shoppinglist.data.prefs.ListPreferencesDataSource
import eu.pascariu.shoppinglist.data.prefs.SortOrder
import eu.pascariu.shoppinglist.data.realtime.RealtimeClient
import eu.pascariu.shoppinglist.data.repo.ItemsRepository
import eu.pascariu.shoppinglist.data.repo.ShopsRepository
import eu.pascariu.shoppinglist.data.undo.UndoConfig
import eu.pascariu.shoppinglist.data.undo.UndoEntry
import eu.pascariu.shoppinglist.ui.widgetstack.WidgetStackControl
import eu.pascariu.shoppinglist.data.undo.UndoHistoryStore
import eu.pascariu.shoppinglist.domain.search.fuzzyMatch
import eu.pascariu.shoppinglist.domain.search.orderViewsGlobalFirst
import eu.pascariu.shoppinglist.domain.order.dropKeyForIndex
import eu.pascariu.shoppinglist.domain.order.manualOrderComparator
import eu.pascariu.shoppinglist.domain.order.orderItems
import eu.pascariu.shoppinglist.ui.drawer.DrawerShop
import javax.inject.Inject
import retrofit2.HttpException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@HiltViewModel
class ListViewModel @Inject constructor(
    private val itemsRepository: ItemsRepository,
    private val shopsRepository: ShopsRepository,
    private val undoHistoryStore: UndoHistoryStore,
    private val listPreferences: ListPreferencesDataSource,
) : ViewModel() {

    private val localState = MutableStateFlow(LocalUiState())
    private val _uiState = MutableStateFlow(ListUiState())
    val uiState: StateFlow<ListUiState> = _uiState.asStateFlow()

    private val inFlightChecks = MutableStateFlow(0)
    private val undoMutex = Mutex()
    private var isCreateInFlight = false
    private var nextPostSaveSignalId = 0L

    init {
        viewModelScope.launch {
            combine(
                itemsRepository.items,
                shopsRepository.shops,
                undoHistoryStore.entries,
                localState,
            ) { items, shops, undoEntries, local ->
                buildUiState(items, shops, undoEntries, local)
            }.collect { newState ->
                _uiState.value = newState
            }
        }
        viewModelScope.launch {
            listPreferences.isReady.first { it }
            restoreActiveView()
            val widgetStackExpanded = listPreferences.widgetStackExpanded.value
            val searchExpanded = listPreferences.searchExpanded.value
            val showChecked = listPreferences.showChecked.value
            val sortModeEnabled = listPreferences.sortModeEnabled.value
            val sortOrder = listPreferences.sortOrder.value
            localState.update {
                it.copy(
                    isWidgetStackExpanded = widgetStackExpanded,
                    searchExpanded = searchExpanded,
                    showChecked = showChecked,
                    sortModeEnabled = sortModeEnabled,
                    sortOrder = sortOrder,
                )
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            try {
                itemsRepository.refetch(listId = RealtimeClient.DEFAULT_LIST_ID)
                shopsRepository.refetch()
                localState.update { it.copy(errorMessage = null) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                localState.update { it.copy(errorMessage = "Couldn't refresh the list") }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        localState.update { it.copy(searchQuery = query) }
    }

    fun onSearchExpandedChanged(expanded: Boolean) {
        localState.update {
            it.copy(
                searchExpanded = expanded,
                searchQuery = if (expanded) it.searchQuery else "",
            )
        }
        viewModelScope.launch {
            listPreferences.setSearchExpanded(expanded)
        }
    }

    fun onShowCheckedChanged(show: Boolean) {
        localState.update { it.copy(showChecked = show) }
        viewModelScope.launch {
            listPreferences.setShowChecked(show)
        }
    }

    fun onSortModeToggled() {
        val enabled = !localState.value.sortModeEnabled
        localState.update { it.copy(sortModeEnabled = enabled) }
        viewModelScope.launch {
            listPreferences.setSortModeEnabled(enabled)
        }
    }

    fun onSortOrderChanged(order: SortOrder) {
        localState.update { it.copy(sortOrder = order) }
        viewModelScope.launch {
            listPreferences.setSortOrder(order)
        }
    }

    fun isDragEnabled(): Boolean = uiState.value.dragEnabled

    fun onItemDropped(item: Item, dropIndex: Int) {
        if (!isDragEnabled()) return
        val state = uiState.value
        val viewId = state.activeViewId ?: return
        val orderedKeys = state.items
            .sortedWith(manualOrderComparator(viewId))
            .filter { it.id != item.id }
            .mapNotNull { it.orderMap[viewId.toString()] }
        viewModelScope.launch {
            try {
                val newKey = dropKeyForIndex(orderedKeys = orderedKeys, dropIndex = dropIndex)
                itemsRepository.reorder(itemId = item.id, viewId = viewId, newKey = newKey)
                localState.update { it.copy(errorMessage = null) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                localState.update { it.copy(errorMessage = "Couldn't move the item") }
            }
        }
    }

    fun onCheckToggled(item: Item) {
        val target = !item.isChecked
        viewModelScope.launch {
            inFlightChecks.update { it + 1 }
            try {
                itemsRepository.setChecked(itemId = item.id, isChecked = target)
                undoHistoryStore.record(
                    UndoEntry(
                        itemId = item.id,
                        from = item.isChecked,
                        to = target,
                        at = System.currentTimeMillis(),
                    ),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            } finally {
                inFlightChecks.update { it - 1 }
            }
        }
    }

    fun onUndoTapped() {
        viewModelScope.launch {
            undoMutex.withLock {
                val settled = waitForInFlightChecks()
                if (!settled) return@withLock
                undoHistoryStore.prune()
                val entries = undoHistoryStore.entries.value
                val itemCache = itemsRepository.items.value.associateBy { it.id }
                for (entry in entries) {
                    val expired = System.currentTimeMillis() - entry.at >= UndoConfig.TTL.toMillis()
                    val item = itemCache[entry.itemId]
                    when {
                        expired || item == null || item.isChecked != entry.to -> {
                            undoHistoryStore.removeTop()
                            continue
                        }
                        else -> {
                            val removed = undoHistoryStore.removeTop()
                            if (removed == null) return@withLock
                            try {
                                inFlightChecks.update { it + 1 }
                                try {
                                    itemsRepository.setChecked(
                                        itemId = entry.itemId,
                                        isChecked = entry.from,
                                        isUndo = true,
                                    )
                                } finally {
                                    inFlightChecks.update { it - 1 }
                                }
                            } catch (e: CancellationException) {
                                throw e
                            } catch (_: Exception) {
                                undoHistoryStore.restoreTop(entry)
                            }
                            return@withLock
                        }
                    }
                }
            }
        }
    }

    fun onAddAffordanceTapped() {
        openAddItemModal(localState.value.searchQuery)
    }

    fun onAddItemTitleChanged(title: String) {
        localState.update { state ->
            state.copy(addItemModal = state.addItemModal?.copy(title = title))
        }
    }

    fun onAddItemViewToggled(viewId: Long) {
        localState.update { state ->
            state.copy(addItemModal = state.addItemModal?.let { modal ->
                val selected = modal.selectedViewIds.toMutableSet()
                if (viewId in selected) selected.remove(viewId) else selected.add(viewId)
                modal.copy(selectedViewIds = selected)
            })
        }
    }

    fun onAddItemToggleAllTapped() {
        localState.update { state ->
            state.copy(addItemModal = state.addItemModal?.let { modal ->
                val selected = if (modal.allViewsSelected) {
                    modal.availableViews.mapNotNull { view ->
                        if (view.name == GLOBAL_VIEW_NAME) view.id else null
                    }.toSet()
                } else {
                    modal.availableViews.map { it.id }.toSet()
                }
                modal.copy(selectedViewIds = selected)
            })
        }
    }

    fun onAddItemSave() {
        val modal = localState.value.addItemModal ?: return
        if (!modal.canSave) return
        if (isCreateInFlight) return
        isCreateInFlight = true
        viewModelScope.launch {
            try {
                itemsRepository.create(
                    listId = RealtimeClient.DEFAULT_LIST_ID,
                    request = CreateItemRequest(
                        name = modal.title.trim(),
                        visibleIn = modal.selectedViewIds.toList(),
                    ),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                isCreateInFlight = false
                localState.update { it.copy(errorMessage = "Couldn't save the item") }
                return@launch
            }
            localState.update {
                it.copy(
                    searchQuery = "",
                    postSaveSignal = PostSaveSignal(id = nextPostSaveSignalId++),
                    addItemModal = null,
                    errorMessage = null,
                )
            }
        }
    }

    fun onAddItemDiscard() {
        localState.update { it.copy(addItemModal = null) }
    }

    fun consumePostSaveSignal() {
        localState.update { it.copy(postSaveSignal = null) }
    }

    fun onErrorDismissed() {
        localState.update { it.copy(errorMessage = null) }
    }

    fun onViewSelected(shopId: Long) {
        localState.update { it.copy(activeViewId = shopId, drawerOpen = false) }
        viewModelScope.launch {
            listPreferences.setActiveShopId(shopId)
        }
    }

    private suspend fun restoreActiveView() {
        if (listPreferences.activeShopId.value == null) return
        shopsRepository.shops.first { it.isNotEmpty() }
        val persistedId = listPreferences.activeShopId.value ?: return
        val shopExists = shopsRepository.shops.value.any { it.id == persistedId }
        if (shopExists) {
            localState.update { it.copy(activeViewId = persistedId) }
        } else {
            listPreferences.setActiveShopId(null)
        }
    }

    fun onWidgetStackExpandedChanged(expanded: Boolean) {
        localState.update { it.copy(isWidgetStackExpanded = expanded) }
        viewModelScope.launch {
            listPreferences.setWidgetStackExpanded(expanded)
        }
    }

    fun onWidgetStackControlTapped(control: WidgetStackControl) {
        when (control) {
            WidgetStackControl.VIEW_SWITCHER -> onDrawerToggled()
            WidgetStackControl.SETTINGS -> Unit
            WidgetStackControl.SORT_MODE -> onSortModeToggled()
            WidgetStackControl.CHECKED_ITEMS_TOGGLE -> onShowCheckedChanged(!localState.value.showChecked)
            WidgetStackControl.SEARCH -> onSearchExpandedChanged(!localState.value.searchExpanded)
        }
    }

    fun onDrawerToggled() {
        localState.update { it.copy(drawerOpen = !it.drawerOpen) }
    }

    fun onDrawerDismissed() {
        localState.update { it.copy(drawerOpen = false) }
    }

    fun onShopEditTapped(shopId: Long) {
        val shop = shopsRepository.shops.value.firstOrNull { it.id == shopId } ?: return
        localState.update {
            it.copy(
                shopModal = ShopModalUiState(
                    mode = ShopModalMode.EDIT,
                    shopId = shop.id,
                    shopName = shop.name,
                    name = shop.name,
                ),
            )
        }
    }

    fun onAddShopTapped() {
        localState.update {
            it.copy(shopModal = ShopModalUiState(mode = ShopModalMode.CREATE))
        }
    }

    fun onShopNameChanged(name: String) {
        localState.update { state ->
            state.copy(shopModal = state.shopModal?.copy(name = name))
        }
    }

    fun onShopDiscard() {
        localState.update { it.copy(shopModal = null) }
    }

    fun onShopSave() {
        val modal = localState.value.shopModal ?: return
        if (!modal.canSave || modal.saving) return
        val shopId = if (modal.mode == ShopModalMode.EDIT) modal.shopId ?: return else null
        localState.update { it.copy(shopModal = it.shopModal?.copy(saving = true, error = null)) }
        viewModelScope.launch {
            try {
                val trimmed = modal.trimmedName
                if (shopId == null) {
                    shopsRepository.create(trimmed)
                } else {
                    shopsRepository.rename(shopId, trimmed)
                }
                localState.update { it.copy(shopModal = null) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                localState.update {
                    it.copy(shopModal = it.shopModal?.copy(saving = false, error = shopErrorMessage(e)))
                }
            }
        }
    }

    fun onShopDeleteTapped() {
        localState.update { it.copy(shopModal = it.shopModal?.copy(deleteConfirmOpen = true)) }
    }

    fun onShopDeleteCancelled() {
        localState.update { it.copy(shopModal = it.shopModal?.copy(deleteConfirmOpen = false)) }
    }

    fun onShopDeleteConfirmed() {
        val modal = localState.value.shopModal ?: return
        val shopId = modal.shopId ?: return
        localState.update { it.copy(shopModal = it.shopModal?.copy(saving = true, deleteConfirmOpen = false)) }
        viewModelScope.launch {
            try {
                shopsRepository.delete(shopId)
                localState.update { it.copy(shopModal = null) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                localState.update {
                    it.copy(shopModal = it.shopModal?.copy(saving = false, error = shopDeleteErrorMessage(e)))
                }
            }
        }
    }

    private fun shopErrorMessage(e: Exception): String =
        when {
            e is HttpException && e.code() == 409 -> "A shop with this name already exists"
            else -> "Could not save shop"
        }

    private fun shopDeleteErrorMessage(e: Exception): String =
        when {
            e is HttpException && e.code() == 403 -> "The Global shop cannot be deleted"
            else -> "Could not delete shop"
        }

    private fun openAddItemModal(prefilledTitle: String) {
        val shops = orderViewsGlobalFirst(shopsRepository.shops.value)
        localState.update { state ->
            state.copy(
                addItemModal = AddItemUiState(
                    title = prefilledTitle,
                    availableViews = shops,
                    selectedViewIds = shops.map { it.id }.toSet(),
                ),
            )
        }
    }

    private fun buildUiState(
        items: List<Item>,
        shops: List<Shop>,
        undoEntries: List<eu.pascariu.shoppinglist.data.undo.UndoEntry>,
        local: LocalUiState,
    ): ListUiState {
        val globalShop = shops.firstOrNull { it.name == GLOBAL_VIEW_NAME }
        val globalShopId = globalShop?.id
        val activeShop = shops.firstOrNull { it.id == local.activeViewId }
        val activeViewId = when {
            activeShop != null -> activeShop.id
            globalShopId != null -> globalShopId
            else -> null
        }
        val viewItems = if (activeViewId == null) {
            items
        } else {
            items.filter { activeViewId in it.visibleIn || it.visibleIn.isEmpty() }
        }
        val sorted = orderItems(items = viewItems, viewId = activeViewId, order = local.sortOrder)
        val query = local.searchQuery.trim()
        val queryActive = query.isNotEmpty()
        val filtered = if (queryActive) sorted.filter { fuzzyMatch(query, it.name) } else sorted
        val hideChecked = !local.showChecked &&
            !queryActive &&
            !(local.sortModeEnabled && local.sortOrder == SortOrder.MANUAL)
        val dragEnabled = local.sortModeEnabled &&
            local.sortOrder == SortOrder.MANUAL &&
            local.addItemModal == null &&
            local.shopModal == null &&
            local.searchQuery.isBlank()
        val activeName = when {
            activeShop != null -> activeShop.name
            globalShop != null -> globalShop.name
            else -> GLOBAL_VIEW_NAME
        }
        val uncheckedCounts = deriveUncheckedCountByShop(items, shops)
        val drawerShops = buildDrawerShops(
            shops = shops,
            globalShopId = globalShopId,
            activeViewId = activeViewId,
            uncheckedCountByShop = uncheckedCounts,
        )
        return ListUiState(
            items = sorted,
            watermarkName = activeName,
            activeViewId = activeViewId,
            isWidgetStackExpanded = local.isWidgetStackExpanded,
            sortModeEnabled = local.sortModeEnabled,
            sortOrder = local.sortOrder,
            dragEnabled = dragEnabled,
            uncheckedCountByShop = uncheckedCounts,
            drawerShops = drawerShops,
            drawerOpen = local.drawerOpen,
            searchQuery = local.searchQuery,
            searchExpanded = local.searchExpanded,
            showChecked = local.showChecked,
            hideChecked = hideChecked,
            filteredItems = filtered,
            showAddAffordance = query.isNotEmpty() && filtered.isEmpty(),
            showUndoBar = undoEntries.isNotEmpty(),
            addItemModal = local.addItemModal,
            shopModal = local.shopModal,
            postSaveSignal = local.postSaveSignal,
            errorMessage = local.errorMessage,
        )
    }

    private fun buildDrawerShops(
        shops: List<Shop>,
        globalShopId: Long?,
        activeViewId: Long?,
        uncheckedCountByShop: Map<Long, Int>,
    ): List<DrawerShop> {
        val global = shops.firstOrNull { it.name == GLOBAL_VIEW_NAME }
        val others = shops
            .filter { it.name != GLOBAL_VIEW_NAME }
            .sortedBy { it.name.lowercase() }
        val globalRow = DrawerShop(
            id = global?.id ?: GLOBAL_PLACEHOLDER_ID,
            name = GLOBAL_VIEW_NAME,
            isGlobal = true,
            isActive = activeViewId == null || activeViewId == global?.id,
            uncheckedCount = globalShopId?.let { uncheckedCountByShop[it] } ?: 0,
        )
        val otherRows = others.map { shop ->
            DrawerShop(
                id = shop.id,
                name = shop.name,
                isGlobal = false,
                isActive = shop.id == activeViewId,
                uncheckedCount = uncheckedCountByShop[shop.id] ?: 0,
            )
        }
        return listOf(globalRow) + otherRows
    }

    private fun deriveUncheckedCountByShop(items: List<Item>, shops: List<Shop>): Map<Long, Int> {
        val globalShopId = shops.firstOrNull { it.name == GLOBAL_VIEW_NAME }?.id
        val counts = mutableMapOf<Long, Int>()
        for (item in items) {
            if (item.isChecked) continue
            if (item.visibleIn.isEmpty()) {
                if (globalShopId != null) counts.merge(globalShopId, 1, Int::plus)
            } else {
                for (shopId in item.visibleIn) {
                    counts.merge(shopId, 1, Int::plus)
                }
            }
        }
        return shops.associate { shop -> shop.id to (counts[shop.id] ?: 0) }
    }

    private suspend fun waitForInFlightChecks(): Boolean {
        return try {
            withTimeout(UndoConfig.INFLIGHT_WAIT.toMillis()) {
                inFlightChecks.first { it == 0 }
            }
            true
        } catch (_: TimeoutCancellationException) {
            false
        }
    }

    private data class LocalUiState(
        val searchQuery: String = "",
        val searchExpanded: Boolean = false,
        val showChecked: Boolean = true,
        val sortModeEnabled: Boolean = true,
        val sortOrder: SortOrder = SortOrder.MANUAL,
        val activeViewId: Long? = null,
        val drawerOpen: Boolean = false,
        val isWidgetStackExpanded: Boolean = false,
        val postSaveSignal: PostSaveSignal? = null,
        val addItemModal: AddItemUiState? = null,
        val shopModal: ShopModalUiState? = null,
        val errorMessage: String? = null,
    )

    private companion object {
        const val GLOBAL_VIEW_NAME = "Global"
        const val GLOBAL_PLACEHOLDER_ID = -1L
    }
}

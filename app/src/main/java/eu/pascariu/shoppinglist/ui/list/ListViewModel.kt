package eu.pascariu.shoppinglist.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import eu.pascariu.shoppinglist.data.connectivity.BackendConnectivity
import eu.pascariu.shoppinglist.data.model.CreateItemRequest
import eu.pascariu.shoppinglist.data.model.Item
import eu.pascariu.shoppinglist.data.model.Shop
import eu.pascariu.shoppinglist.data.model.UpdateItemRequest
import eu.pascariu.shoppinglist.data.model.isExpiredAt
import eu.pascariu.shoppinglist.data.prefs.ListPreferencesDataSource
import eu.pascariu.shoppinglist.data.prefs.ThemeRepository
import eu.pascariu.shoppinglist.data.prefs.SortOrder
import eu.pascariu.shoppinglist.data.realtime.RealtimeClient
import eu.pascariu.shoppinglist.data.realtime.RealtimeSyncManagerContract
import eu.pascariu.shoppinglist.data.repo.ItemsRepository
import eu.pascariu.shoppinglist.data.repo.ShopsRepository
import eu.pascariu.shoppinglist.data.undo.UndoConfig
import eu.pascariu.shoppinglist.data.undo.UndoEntry
import eu.pascariu.shoppinglist.ui.quickmenu.QuickMenuControl
import eu.pascariu.shoppinglist.data.undo.UndoHistoryStore
import eu.pascariu.shoppinglist.domain.search.fuzzyMatch
import eu.pascariu.shoppinglist.domain.search.GLOBAL_FALLBACK_ID
import eu.pascariu.shoppinglist.di.ExpiryTicker
import eu.pascariu.shoppinglist.domain.search.GLOBAL_VIEW_NAME
import eu.pascariu.shoppinglist.domain.search.modalAvailableViews
import eu.pascariu.shoppinglist.domain.search.orderViewsGlobalFirst
import eu.pascariu.shoppinglist.domain.order.dropKeyForIndex
import eu.pascariu.shoppinglist.domain.order.manualOrderComparator
import eu.pascariu.shoppinglist.domain.order.orderItems
import eu.pascariu.shoppinglist.ui.drawer.DrawerShop
import javax.inject.Inject
import retrofit2.HttpException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicBoolean

@HiltViewModel
class ListViewModel @Inject constructor(
    private val itemsRepository: ItemsRepository,
    private val shopsRepository: ShopsRepository,
    private val connectivity: @JvmSuppressWildcards StateFlow<BackendConnectivity>,
    private val undoHistoryStore: UndoHistoryStore,
    private val listPreferences: ListPreferencesDataSource,
    private val themeRepository: ThemeRepository,
    private val realtimeSyncManager: RealtimeSyncManagerContract,
    @ExpiryTicker expiryTicker: StateFlow<Long>,
) : ViewModel() {

    private val localState = MutableStateFlow(LocalUiState())
    private val _uiState = MutableStateFlow(ListUiState())
    val uiState: StateFlow<ListUiState> = _uiState.asStateFlow()

    val isOffline: StateFlow<Boolean> =
        connectivity
            .map { it == BackendConnectivity.UNREACHABLE }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _changeToast = MutableStateFlow<ChangeToast?>(null)
    val changeToast: StateFlow<ChangeToast?> = _changeToast.asStateFlow()
    private val pendingToastTexts = ArrayDeque<String>()
    private var toastDismissJob: Job? = null
    private var nextChangeToastId = 0L

    private val inFlightChecks = MutableStateFlow(0)
    private val undoMutex = Mutex()
    private val undoInFlightGuard = AtomicBoolean(false)
    private var isCreateInFlight = false
    private var isUpdateInFlight = false
    private var nextPostSaveSignalId = 0L

    init {
        viewModelScope.launch {
            combine(
                itemsRepository.items,
                shopsRepository.shops,
                undoHistoryStore.entries,
                localState,
                expiryTicker,
            ) { items, shops, undoEntries, local, nowMillis ->
                buildUiState(items, shops, undoEntries, local, nowMillis)
            }.collect { newState ->
                _uiState.value = newState
            }
        }
        viewModelScope.launch {
            listPreferences.isReady.first { it }
            restoreActiveView()
            val quickMenuExpanded = listPreferences.quickMenuExpanded.value
            val searchExpanded = listPreferences.searchExpanded.value
            val showChecked = listPreferences.showChecked.value
            val sortModeEnabled = listPreferences.sortModeEnabled.value
            val sortOrder = listPreferences.sortOrder.value
            localState.update {
                it.copy(
                    isQuickMenuExpanded = quickMenuExpanded,
                    searchExpanded = searchExpanded,
                    showChecked = showChecked,
                    sortModeEnabled = sortModeEnabled,
                    sortOrder = sortOrder,
                )
            }
        }
        viewModelScope.launch {
            realtimeSyncManager.changeToastLabels.collect { labelBatch ->
                val enabled = themeRepository.changeToastsEnabled.value
                if (!enabled) {
                    pendingToastTexts.clear()
                    return@collect
                }
                showChangeToasts(labelBatch.map { it.text })
            }
        }
        viewModelScope.launch {
            var previous = connectivity.value
            connectivity.collect { state ->
                val wasUnreachable = previous == BackendConnectivity.UNREACHABLE
                previous = state
                if (wasUnreachable && state == BackendConnectivity.REACHABLE) {
                    refresh()
                }
            }
        }
    }

    private fun showChangeToasts(texts: List<String>) {
        if (texts.isEmpty()) return
        if (_changeToast.value != null) {
            pendingToastTexts.addAll(texts)
        } else {
            displayToast(texts.first())
            pendingToastTexts.addAll(texts.drop(1))
        }
        trimPendingToasts()
    }

    private fun trimPendingToasts() {
        while (pendingToastTexts.size > MAX_PENDING_TOASTS) {
            pendingToastTexts.removeFirst()
        }
    }

    private fun displayToast(text: String) {
        _changeToast.value = ChangeToast(id = nextChangeToastId++, text = text)
        toastDismissJob?.cancel()
        toastDismissJob = viewModelScope.launch {
            delay(TOAST_AUTO_DISMISS_MS)
            dismissChangeToast()
        }
    }

    fun onToastSwipedDismissed(toastId: Long) {
        if (_changeToast.value?.id == toastId) {
            dismissChangeToast()
        }
    }

    private fun dismissChangeToast() {
        toastDismissJob?.cancel()
        toastDismissJob = null
        _changeToast.value = null
        val next = pendingToastTexts.removeFirstOrNull()
        if (next != null) {
            displayToast(next)
        }
    }

    fun refresh() {
        if (connectivity.value == BackendConnectivity.UNREACHABLE) {
            return
        }
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
        val clampedDropIndex = dropIndex.coerceIn(0, orderedKeys.size)
        viewModelScope.launch {
            try {
                val newKey = dropKeyForIndex(orderedKeys = orderedKeys, dropIndex = clampedDropIndex)
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
        if (!undoInFlightGuard.compareAndSet(false, true)) return
        localState.update { it.copy(isUndoInFlight = true) }
        viewModelScope.launch {
            try {
                undoMutex.withLock {
                    try {
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
                    } finally {
                        undoInFlightGuard.set(false)
                        localState.update { it.copy(isUndoInFlight = false) }
                    }
                }
            } catch (e: CancellationException) {
                undoInFlightGuard.set(false)
                localState.update { it.copy(isUndoInFlight = false) }
                throw e
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

    fun onAddItemQuantityChanged(quantity: Int) {
        localState.update { state ->
            state.copy(addItemModal = state.addItemModal?.copy(quantity = quantity))
        }
    }

    fun onAddItemTemporaryToggled() {
        localState.update { state ->
            state.copy(addItemModal = state.addItemModal?.let { modal ->
                modal.copy(isTemporary = !modal.isTemporary)
            })
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
                        visibleIn = modal.selectedViewIds.filter { it != GLOBAL_FALLBACK_ID },
                        quantity = modal.quantity,
                        isTemporary = if (modal.isTemporary) true else null,
                    ),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                isCreateInFlight = false
                localState.update { it.copy(errorMessage = "Couldn't save the item") }
                return@launch
            }
            isCreateInFlight = false
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

    fun onItemLongPressed(item: Item) {
        val shops = modalAvailableViews(shopsRepository.shops.value)
        localState.update { state ->
            state.copy(
                editItemModal = EditItemUiState(
                    itemId = item.id,
                    originalName = item.name,
                    name = item.name,
                    availableViews = shops,
                    selectedViewIds = item.visibleIn.toSet(),
                    originalVisibleIn = item.visibleIn.toSet(),
                    originalQuantity = item.quantity,
                    quantity = item.quantity,
                    originalIsTemporary = item.isTemporary,
                    isTemporary = item.isTemporary,
                    checkCount = item.checkCount,
                ),
            )
        }
    }

    fun onEditItemTitleChanged(name: String) {
        localState.update { state ->
            state.copy(editItemModal = state.editItemModal?.copy(name = name))
        }
    }

    fun onEditItemQuantityChanged(quantity: Int) {
        localState.update { state ->
            state.copy(editItemModal = state.editItemModal?.copy(quantity = quantity))
        }
    }

    fun onEditItemTemporaryToggled() {
        localState.update { state ->
            state.copy(editItemModal = state.editItemModal?.let { modal ->
                modal.copy(isTemporary = !modal.isTemporary)
            })
        }
    }

    fun onEditItemViewToggled(viewId: Long) {
        localState.update { state ->
            state.copy(editItemModal = state.editItemModal?.let { modal ->
                val selected = modal.selectedViewIds.toMutableSet()
                if (viewId in selected) selected.remove(viewId) else selected.add(viewId)
                modal.copy(selectedViewIds = selected)
            })
        }
    }

    fun onEditItemToggleAllTapped() {
        localState.update { state ->
            state.copy(editItemModal = state.editItemModal?.let { modal ->
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

    fun onEditItemDiscard() {
        localState.update { it.copy(editItemModal = null) }
    }

    fun onEditItemSave() {
        val modal = localState.value.editItemModal ?: return
        if (!modal.canSave) return
        if (isUpdateInFlight) return
        if (itemsRepository.items.value.none { it.id == modal.itemId }) return
        isUpdateInFlight = true
        viewModelScope.launch {
            try {
                itemsRepository.updateDetails(
                    itemId = modal.itemId,
                    request = UpdateItemRequest(
                        name = if (modal.name.trim() != modal.originalName.trim()) modal.name.trim() else null,
                        visibleIn = if (modal.selectedViewIds != modal.originalVisibleIn) {
                            modal.selectedViewIds.filter { it != GLOBAL_FALLBACK_ID }
                        } else {
                            null
                        },
                        quantity = if (modal.quantity != modal.originalQuantity) modal.quantity else null,
                        isTemporary = if (modal.isTemporary != modal.originalIsTemporary) modal.isTemporary else null,
                    ),
                )
                localState.update { it.copy(editItemModal = null, errorMessage = null) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                localState.update { state ->
                    state.copy(
                        editItemModal = null,
                        errorMessage = "Couldn't save the item",
                    )
                }
            } finally {
                isUpdateInFlight = false
            }
        }
    }

    fun onEditItemDeleteTapped() {
        val modal = localState.value.editItemModal ?: return
        val item = itemsRepository.items.value.firstOrNull { it.id == modal.itemId } ?: return
        localState.update { it.copy(editItemModal = null, deleteConfirmItem = item) }
    }

    fun onDeleteCancelled() {
        localState.update { it.copy(deleteConfirmItem = null) }
    }

    fun onDeleteConfirmed() {
        val item = localState.value.deleteConfirmItem ?: return
        val previousIndex = itemsRepository.items.value.indexOfFirst { it.id == item.id }
        if (previousIndex < 0) {
            localState.update { it.copy(deleteConfirmItem = null) }
            return
        }
        localState.update { it.copy(deleteConfirmItem = null) }
        viewModelScope.launch {
            itemsRepository.applyDeleted(item.id)
            try {
                itemsRepository.delete(item.id)
                localState.update { it.copy(errorMessage = null) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (e is HttpException && e.code() == 404) {
                    localState.update { it.copy(errorMessage = null) }
                } else {
                    itemsRepository.applyRestored(item)
                    localState.update {
                        it.copy(errorMessage = "Couldn't delete the item")
                    }
                }
            }
        }
    }

    fun consumePostSaveSignal() {
        localState.update { it.copy(postSaveSignal = null) }
    }

    fun onErrorDismissed() {
        localState.update { it.copy(errorMessage = null) }
    }

    fun onViewSelected(shopId: Long) {
        val activeShopId = if (shopId == GLOBAL_PLACEHOLDER_ID) null else shopId
        localState.update { it.copy(activeViewId = activeShopId, drawerOpen = false) }
        viewModelScope.launch {
            listPreferences.setActiveShopId(activeShopId)
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

    fun onQuickMenuExpandedChanged(expanded: Boolean) {
        localState.update { it.copy(isQuickMenuExpanded = expanded) }
        viewModelScope.launch {
            listPreferences.setQuickMenuExpanded(expanded)
        }
    }

    fun onQuickMenuControlTapped(control: QuickMenuControl) {
        when (control) {
            QuickMenuControl.VIEW_SWITCHER -> onDrawerToggled()
            QuickMenuControl.SETTINGS -> onSettingsDrawerToggled()
            QuickMenuControl.SORT_MODE -> onSortModeToggled()
            QuickMenuControl.CHECKED_ITEMS_TOGGLE -> onShowCheckedChanged(!localState.value.showChecked)
            QuickMenuControl.SEARCH -> onSearchExpandedChanged(!localState.value.searchExpanded)
            QuickMenuControl.UNDO -> onUndoTapped()
        }
    }

    fun onSettingsDrawerToggled() {
        localState.update { it.copy(settingsDrawerOpen = !it.settingsDrawerOpen) }
    }

    fun onSettingsDrawerDismissed() {
        localState.update { it.copy(settingsDrawerOpen = false) }
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
        val shops = modalAvailableViews(shopsRepository.shops.value)
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
        nowMillis: Long,
    ): ListUiState {
        val now = java.time.Instant.ofEpochMilli(nowMillis)
        val liveItems = items.filterNot { it.isExpiredAt(now) }
        val globalShop = shops.firstOrNull { it.name == GLOBAL_VIEW_NAME }
        val globalShopId = globalShop?.id
        val activeShop = shops.firstOrNull { it.id == local.activeViewId }
        val activeViewId = when {
            activeShop != null -> activeShop.id
            globalShopId != null -> globalShopId
            else -> null
        }
        val viewItems = if (activeViewId == null) {
            liveItems
        } else {
            liveItems.filter { activeViewId in it.visibleIn || it.visibleIn.isEmpty() }
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
            local.editItemModal == null &&
            local.shopModal == null &&
            local.searchQuery.isBlank()
        val activeName = when {
            activeShop != null -> activeShop.name
            globalShop != null -> globalShop.name
            else -> GLOBAL_VIEW_NAME
        }
        val editItemModal = local.editItemModal?.let { modal ->
            items.firstOrNull { it.id == modal.itemId }
                ?.takeIf { it.checkCount != modal.checkCount }
                ?.let { modal.copy(checkCount = it.checkCount) }
                ?: modal
        }
        val uncheckedCounts = deriveUncheckedCountByShop(liveItems, shops)
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
            isQuickMenuExpanded = local.isQuickMenuExpanded,
            sortModeEnabled = local.sortModeEnabled,
            sortOrder = local.sortOrder,
            dragEnabled = dragEnabled,
            uncheckedCountByShop = uncheckedCounts,
            drawerShops = drawerShops,
            drawerOpen = local.drawerOpen,
            settingsDrawerOpen = local.settingsDrawerOpen,
            searchQuery = local.searchQuery,
            searchExpanded = local.searchExpanded,
            showChecked = local.showChecked,
            hideChecked = hideChecked,
            filteredItems = filtered,
            showAddAffordance = query.isNotEmpty() && filtered.isEmpty(),
            canUndo = undoEntries.isNotEmpty(),
            isUndoInFlight = local.isUndoInFlight,
            addItemModal = local.addItemModal,
            editItemModal = editItemModal,
            deleteConfirmItem = local.deleteConfirmItem,
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
        val settingsDrawerOpen: Boolean = false,
        val isQuickMenuExpanded: Boolean = false,
        val postSaveSignal: PostSaveSignal? = null,
        val addItemModal: AddItemUiState? = null,
        val editItemModal: EditItemUiState? = null,
        val deleteConfirmItem: Item? = null,
        val shopModal: ShopModalUiState? = null,
        val errorMessage: String? = null,
        val isUndoInFlight: Boolean = false,
    )

    companion object {
        internal const val GLOBAL_PLACEHOLDER_ID = -1L
        internal const val TOAST_AUTO_DISMISS_MS = 3_500L
        internal const val MAX_PENDING_TOASTS = 5
    }
}

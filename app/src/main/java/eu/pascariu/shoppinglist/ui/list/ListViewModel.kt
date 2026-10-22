package eu.pascariu.shoppinglist.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import eu.pascariu.shoppinglist.data.model.CreateItemRequest
import eu.pascariu.shoppinglist.data.model.Item
import eu.pascariu.shoppinglist.data.model.Shop
import eu.pascariu.shoppinglist.data.realtime.RealtimeClient
import eu.pascariu.shoppinglist.data.repo.ItemsRepository
import eu.pascariu.shoppinglist.data.repo.ShopsRepository
import eu.pascariu.shoppinglist.data.undo.UndoConfig
import eu.pascariu.shoppinglist.data.undo.UndoEntry
import eu.pascariu.shoppinglist.data.undo.UndoHistoryStore
import eu.pascariu.shoppinglist.domain.search.fuzzyMatch
import eu.pascariu.shoppinglist.domain.search.orderViewsGlobalFirst
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
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
) : ViewModel() {

    private val localState = MutableStateFlow(LocalUiState())
    private val _uiState = MutableStateFlow(ListUiState())
    val uiState: StateFlow<ListUiState> = _uiState.asStateFlow()

    private val inFlightChecks = MutableStateFlow(0)
    private val undoMutex = Mutex()

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
    }

    fun refresh() {
        viewModelScope.launch {
            try {
                itemsRepository.refetch(listId = RealtimeClient.DEFAULT_LIST_ID)
                shopsRepository.refetch()
            } catch (_: Exception) {
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        localState.update { it.copy(searchQuery = query) }
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
        viewModelScope.launch {
            try {
                itemsRepository.create(
                    listId = RealtimeClient.DEFAULT_LIST_ID,
                    request = CreateItemRequest(
                        name = modal.title.trim(),
                        visibleIn = modal.selectedViewIds.toList(),
                    ),
                )
            } catch (_: Exception) {
                return@launch
            }
            localState.update {
                it.copy(searchQuery = "", postSaveSignal = PostSaveSignal(), addItemModal = null)
            }
        }
    }

    fun onAddItemDiscard() {
        localState.update { it.copy(addItemModal = null) }
    }

    fun consumePostSaveSignal() {
        localState.update { it.copy(postSaveSignal = null) }
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
        val activeViewId = globalShop?.id
        val viewItems = if (activeViewId == null) {
            items
        } else {
            items.filter { activeViewId in it.visibleIn || it.visibleIn.isEmpty() }
        }
        val sorted = sortManual(items = viewItems, viewId = activeViewId)
        val query = local.searchQuery.trim()
        val filtered = if (query.isEmpty()) sorted else sorted.filter { fuzzyMatch(query, it.name) }
        return ListUiState(
            items = sorted,
            watermarkName = globalShop?.name ?: GLOBAL_VIEW_NAME,
            searchQuery = local.searchQuery,
            filteredItems = filtered,
            showAddAffordance = query.isNotEmpty() && filtered.isEmpty(),
            showUndoBar = undoEntries.isNotEmpty(),
            addItemModal = local.addItemModal,
            postSaveSignal = local.postSaveSignal,
        )
    }

    private fun sortManual(items: List<Item>, viewId: Long?): List<Item> {
        if (viewId == null) return items
        return items.sortedWith(
            compareBy<Item> { it.orderMap[viewId.toString()] ?: "\uFFFF" }
                .thenBy { it.id },
        )
    }

    private suspend fun waitForInFlightChecks(): Boolean {
        val deadline = System.currentTimeMillis() + UndoConfig.INFLIGHT_WAIT.toMillis()
        while (inFlightChecks.value > 0) {
            if (System.currentTimeMillis() >= deadline) return false
            delay(UndoConfig.INFLIGHT_WAIT.toMillis().coerceAtMost(deadline - System.currentTimeMillis()).coerceAtLeast(1))
        }
        return true
    }

    private data class LocalUiState(
        val searchQuery: String = "",
        val postSaveSignal: PostSaveSignal? = null,
        val addItemModal: AddItemUiState? = null,
    )

    private companion object {
        const val GLOBAL_VIEW_NAME = "Global"
    }
}

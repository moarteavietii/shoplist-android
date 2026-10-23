package eu.pascariu.shoppinglist.data.repo

import eu.pascariu.shoppinglist.data.api.ItemsApi
import eu.pascariu.shoppinglist.data.model.CreateItemRequest
import eu.pascariu.shoppinglist.data.model.Item
import eu.pascariu.shoppinglist.data.model.UpdateItemRequest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.updateAndGet
import retrofit2.HttpException

@Singleton
class ItemsRepository @Inject constructor(
    private val itemsApi: ItemsApi,
) {

    private val cache = MutableStateFlow<List<Item>>(emptyList())
    private val cachedListId = MutableStateFlow<Long?>(null)
    val items: StateFlow<List<Item>> = cache.asStateFlow()
    val cachedListIdFlow: StateFlow<Long?> = cachedListId.asStateFlow()

    suspend fun refetch(listId: Long) {
        cachedListId.value = listId
        cache.value = itemsApi.listItems(listId)
    }

    suspend fun create(listId: Long, request: CreateItemRequest): Item {
        val created = itemsApi.createItem(listId, request)
        if (listId == cachedListId.value) {
            cache.update { it + created }
        }
        return created
    }

    suspend fun update(itemId: Long, request: UpdateItemRequest): Item {
        val updated = itemsApi.updateItem(itemId, request)
        if (updated.listId == cachedListId.value) {
            cache.update { current ->
                if (current.any { it.id == updated.id }) {
                    current.map { if (it.id == updated.id) updated else it }
                } else {
                    current + updated
                }
            }
        }
        return updated
    }

    suspend fun delete(itemId: Long) {
        val response = itemsApi.deleteItem(itemId)
        if (!response.isSuccessful) throw HttpException(response)
        cache.update { current -> current.filterNot { it.id == itemId } }
    }

    suspend fun setChecked(itemId: Long, isChecked: Boolean, isUndo: Boolean = false): Item {
        val previous = cache.value.firstOrNull { it.id == itemId }
            ?: throw IllegalArgumentException("item $itemId not cached")
        cache.update { current -> current.map { if (it.id == itemId) it.copy(isChecked = isChecked) else it } }
        try {
            val updated = itemsApi.updateItem(itemId, UpdateItemRequest(isChecked = isChecked, isUndo = if (isUndo) true else null))
            cache.update { current -> current.map { if (it.id == updated.id) updated else it } }
            return updated
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            cache.update { current -> current.map { if (it.id == itemId) it.copy(isChecked = previous.isChecked) else it } }
            throw error
        }
    }

    suspend fun reorder(itemId: Long, viewId: Long, newKey: String): Item {
        val viewKey = viewId.toString()
        var previousKey: String? = null
        val mergedItem = cache.updateAndGet { current ->
            current.map { item ->
                if (item.id == itemId) {
                    previousKey = item.orderMap[viewKey]
                    item.copy(orderMap = item.orderMap + (viewKey to newKey))
                } else {
                    item
                }
            }
        }.firstOrNull { it.id == itemId }
            ?: throw IllegalArgumentException("item $itemId not cached")
        try {
            val updated = itemsApi.updateItem(itemId, UpdateItemRequest(orderMap = mergedItem.orderMap))
            cache.update { current -> current.map { if (it.id == updated.id) updated else it } }
            return updated
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            cache.update { current ->
                current.map { item ->
                    if (item.id == itemId) {
                        if (item.orderMap[viewKey] == newKey) {
                            val restoredOrderMap = if (previousKey == null) item.orderMap - viewKey else item.orderMap + (viewKey to previousKey)
                            item.copy(orderMap = restoredOrderMap)
                        } else {
                            item
                        }
                    } else {
                        item
                    }
                }
            }
            throw error
        }
    }

    fun applyCreated(item: Item) {
        if (item.listId != cachedListId.value) return
        cache.update { current ->
            if (current.any { it.id == item.id }) current else current + item
        }
    }

    fun applyRestored(item: Item, previousIndex: Int) {
        if (item.listId != cachedListId.value) return
        cache.update { current ->
            if (current.any { it.id == item.id }) {
                current
            } else {
                val index = previousIndex.coerceIn(0, current.size)
                current.toMutableList().apply { add(index, item) }
            }
        }
    }

    fun applyUpdated(item: Item) {
        if (item.listId != cachedListId.value) return
        cache.update { current ->
            val existing = current.firstOrNull { it.id == item.id } ?: return@update current
            if (existing == item) current else current.map { if (it.id == item.id) item else it }
        }
    }

    fun applyDeleted(itemId: Long) {
        cache.update { current -> current.filterNot { it.id == itemId } }
    }
}

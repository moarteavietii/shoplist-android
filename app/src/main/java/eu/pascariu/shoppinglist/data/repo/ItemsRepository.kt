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
            cache.value = cache.value + created
        }
        return created
    }

    suspend fun update(itemId: Long, request: UpdateItemRequest): Item {
        val updated = itemsApi.updateItem(itemId, request)
        if (updated.listId == cachedListId.value) {
            cache.value = cache.value.map { if (it.id == updated.id) updated else it }
        }
        return updated
    }

    suspend fun delete(itemId: Long) {
        itemsApi.deleteItem(itemId)
        cache.value = cache.value.filterNot { it.id == itemId }
    }
}

package eu.pascariu.shoppinglist.data.repo

import eu.pascariu.shoppinglist.data.api.ItemsApi
import eu.pascariu.shoppinglist.data.config.FeatureConfig
import eu.pascariu.shoppinglist.data.local.ItemDao
import eu.pascariu.shoppinglist.data.local.ItemEntity
import eu.pascariu.shoppinglist.data.local.MutationPayload
import eu.pascariu.shoppinglist.data.local.MutationQueue
import eu.pascariu.shoppinglist.data.local.toEntity
import eu.pascariu.shoppinglist.data.local.toModel
import eu.pascariu.shoppinglist.data.model.CreateItemRequest
import eu.pascariu.shoppinglist.data.model.Item
import eu.pascariu.shoppinglist.data.model.UpdateItemRequest
import eu.pascariu.shoppinglist.di.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException
import java.io.IOException

@Singleton
class ItemsRepository @Inject constructor(
    private val itemsApi: ItemsApi,
    private val itemDao: ItemDao,
    private val mutationQueue: MutationQueue,
    @ApplicationScope externalScope: CoroutineScope,
) {

    private val _cachedListId = MutableStateFlow<Long?>(null)
    private val placeholderMutex = Mutex()
    private var nextPlaceholderCandidate = -1L
    val items: StateFlow<List<Item>> = itemDao.observeAll()
        .map { entities -> entities.map { it.toModel() } }
        .stateIn(externalScope, SharingStarted.Eagerly, emptyList())
    val cachedListIdFlow: StateFlow<Long?> = _cachedListId.asStateFlow()

    suspend fun refetch(listId: Long) {
        val fetched = itemsApi.listItems(listId).map { it.toEntity() }
        _cachedListId.value = listId
        itemDao.replaceAll(fetched)
    }

    suspend fun create(listId: Long, request: CreateItemRequest): Item {
        if (mutationQueue.isOffline) {
            return createOffline(listId, request)
        }
        try {
            val created = itemsApi.createItem(listId, request)
            if (listId == _cachedListId.value) {
                itemDao.upsert(created.toEntity())
            }
            return created
        } catch (error: CancellationException) {
            throw error
        } catch (error: IOException) {
            return createOffline(listId, request)
        }
    }

    private suspend fun createOffline(listId: Long, request: CreateItemRequest): Item {
        val placeholderId = nextPlaceholderId()
        val optimistic = Item(
            id = placeholderId,
            listId = listId,
            name = request.name,
            isChecked = false,
            orderMap = request.orderMap ?: emptyMap(),
            quantity = request.quantity ?: 1,
            isTemporary = request.isTemporary ?: false,
            checkCount = 0,
            expiresAt = null,
            createdAt = java.time.Instant.now().toString(),
            visibleIn = request.visibleIn ?: emptyList(),
        )
        itemDao.upsert(optimistic.toEntity())
        mutationQueue.enqueue(MutationPayload.ItemCreate(listId, placeholderId, request))
        return optimistic
    }

    suspend fun update(itemId: Long, request: UpdateItemRequest): Item {
        if (mutationQueue.isOffline) {
            return updateOffline(itemId, request)
        }
        try {
            val updated = itemsApi.updateItem(itemId, request)
            if (updated.listId == _cachedListId.value) {
                itemDao.upsert(updated.toEntity())
            }
            return updated
        } catch (error: CancellationException) {
            throw error
        } catch (error: IOException) {
            return updateOffline(itemId, request)
        }
    }

    private suspend fun updateOffline(itemId: Long, request: UpdateItemRequest): Item {
        val previous = itemDao.getById(itemId)?.toModel()
            ?: throw IllegalArgumentException("item $itemId not cached")
        val optimisticIsChecked = request.isChecked ?: previous.isChecked
        val optimistic = previous.copy(
            name = request.name ?: previous.name,
            isChecked = optimisticIsChecked,
            orderMap = request.orderMap ?: previous.orderMap,
            visibleIn = request.visibleIn ?: previous.visibleIn,
            quantity = request.quantity ?: previous.quantity,
            isTemporary = request.isTemporary ?: previous.isTemporary,
            expiresAt = when {
                request.isTemporary == false -> null
                request.isTemporary == true && optimisticIsChecked -> stampExpiry()
                else -> previous.expiresAt
            },
        )
        itemDao.upsert(optimistic.toEntity())
        mutationQueue.enqueue(MutationPayload.ItemUpdate(itemId, request))
        return optimistic
    }

    suspend fun updateDetails(itemId: Long, request: UpdateItemRequest): Item {
        val previous = itemDao.getById(itemId)?.toModel()
            ?: throw IllegalArgumentException("item $itemId not cached")
        val isTemporary = request.isTemporary ?: previous.isTemporary
        val optimisticExpiry = when {
            request.isTemporary == false -> null
            request.isTemporary == true && previous.isChecked -> stampExpiry()
            else -> previous.expiresAt
        }
        val optimistic = previous.copy(
            name = request.name ?: previous.name,
            visibleIn = request.visibleIn ?: previous.visibleIn,
            quantity = request.quantity ?: previous.quantity,
            isTemporary = isTemporary,
            expiresAt = optimisticExpiry,
        )
        itemDao.upsert(optimistic.toEntity())
        if (mutationQueue.isOffline) {
            mutationQueue.enqueue(MutationPayload.ItemUpdate(itemId, request))
            return optimistic
        }
        try {
            val updated = itemsApi.updateItem(itemId, request)
            itemDao.upsert(updated.toEntity())
            return updated
        } catch (error: CancellationException) {
            throw error
        } catch (error: IOException) {
            mutationQueue.enqueue(MutationPayload.ItemUpdate(itemId, request))
            return optimistic
        } catch (error: Exception) {
            rollbackIfUnchanged(itemId, optimistic.toEntity(), previous.toEntity())
            throw error
        }
    }

    suspend fun delete(itemId: Long) {
        if (mutationQueue.isOffline) {
            itemDao.deleteById(itemId)
            mutationQueue.enqueue(MutationPayload.ItemDelete(itemId))
            return
        }
        try {
            val response = itemsApi.deleteItem(itemId)
            if (!response.isSuccessful) throw HttpException(response)
            itemDao.deleteById(itemId)
        } catch (error: CancellationException) {
            throw error
        } catch (error: IOException) {
            itemDao.deleteById(itemId)
            mutationQueue.enqueue(MutationPayload.ItemDelete(itemId))
        }
    }

    suspend fun setChecked(itemId: Long, isChecked: Boolean, isUndo: Boolean = false): Item {
        val previous = itemDao.getById(itemId)?.toModel()
            ?: throw IllegalArgumentException("item $itemId not cached")
        val optimisticExpiry = when {
            isChecked && previous.isTemporary -> stampExpiry()
            else -> null
        }
        val optimistic = previous.copy(isChecked = isChecked, expiresAt = optimisticExpiry)
        itemDao.upsert(optimistic.toEntity())
        if (mutationQueue.isOffline) {
            mutationQueue.enqueueCheckCoalesced(
                MutationPayload.ItemCheck(itemId = itemId, isChecked = isChecked, isUndo = if (isUndo) true else null),
            )
            return optimistic
        }
        try {
            val updated = itemsApi.updateItem(itemId, UpdateItemRequest(isChecked = isChecked, isUndo = if (isUndo) true else null))
            itemDao.upsert(updated.toEntity())
            return updated
        } catch (error: CancellationException) {
            throw error
        } catch (error: IOException) {
            mutationQueue.enqueueCheckCoalesced(
                MutationPayload.ItemCheck(itemId = itemId, isChecked = isChecked, isUndo = if (isUndo) true else null),
            )
            return optimistic
        } catch (error: Exception) {
            itemDao.getById(itemId)?.let { current ->
                itemDao.upsert(
                    current.toModel()
                        .copy(isChecked = previous.isChecked, expiresAt = previous.expiresAt)
                        .toEntity(),
                )
            }
            throw error
        }
    }

    suspend fun reorder(itemId: Long, viewId: Long, newKey: String): Item {
        val viewKey = viewId.toString()
        val previous = itemDao.getById(itemId)?.toModel()
            ?: throw IllegalArgumentException("item $itemId not cached")
        val previousKey = previous.orderMap[viewKey]
        val mergedItem = previous.copy(orderMap = previous.orderMap + (viewKey to newKey))
        itemDao.upsert(mergedItem.toEntity())
        if (mutationQueue.isOffline) {
            mutationQueue.enqueue(MutationPayload.ItemUpdate(itemId, UpdateItemRequest(orderMap = mergedItem.orderMap)))
            return mergedItem
        }
        try {
            val updated = itemsApi.updateItem(itemId, UpdateItemRequest(orderMap = mergedItem.orderMap))
            itemDao.upsert(updated.toEntity())
            return updated
        } catch (error: CancellationException) {
            throw error
        } catch (error: IOException) {
            mutationQueue.enqueue(MutationPayload.ItemUpdate(itemId, UpdateItemRequest(orderMap = mergedItem.orderMap)))
            return mergedItem
        } catch (error: Exception) {
            itemDao.getById(itemId)?.let { current ->
                if (current.toModel().orderMap[viewKey] == newKey) {
                    val restored = if (previousKey == null) {
                        previous.orderMap - viewKey
                    } else {
                        previous.orderMap + (viewKey to previousKey)
                    }
                    itemDao.upsert(previous.copy(orderMap = restored).toEntity())
                }
            }
            throw error
        }
    }

    suspend fun applyCreated(item: Item) {
        if (item.listId != _cachedListId.value) return
        if (itemDao.getById(item.id) != null) return
        itemDao.upsert(item.toEntity())
    }

    suspend fun applyRestored(item: Item, previousIndex: Int) {
        if (item.listId != _cachedListId.value) return
        if (itemDao.getById(item.id) != null) return
        itemDao.upsert(item.toEntity())
    }

    suspend fun applyUpdated(item: Item) {
        if (item.listId != _cachedListId.value) return
        val existing = itemDao.getById(item.id)?.toModel() ?: return
        if (existing == item) return
        itemDao.upsert(item.toEntity())
    }

    suspend fun applyDeleted(itemId: Long) {
        itemDao.deleteById(itemId)
    }

    suspend fun remapPlaceholder(placeholderId: Long, realItem: Item) {
        itemDao.deleteById(placeholderId)
        itemDao.upsert(realItem.toEntity())
    }

    private suspend fun nextPlaceholderId(): Long = placeholderMutex.withLock {
        var candidate = nextPlaceholderCandidate
        while (itemDao.getById(candidate) != null) candidate -= 1
        nextPlaceholderCandidate = candidate - 1
        candidate
    }

    private suspend fun rollbackIfUnchanged(currentId: Long, optimistic: ItemEntity, previous: ItemEntity) {
        val current = itemDao.getById(currentId)
        if (current == optimistic) {
            itemDao.upsert(previous)
        }
    }

    private fun stampExpiry(): String =
        java.time.Instant.ofEpochMilli(System.currentTimeMillis())
            .plus(FeatureConfig.TEMPORARY_ITEM_TTL)
            .toString()
}

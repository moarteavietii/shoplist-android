package eu.pascariu.shoppinglist.data.repo

import eu.pascariu.shoppinglist.data.api.ShopsApi
import eu.pascariu.shoppinglist.data.local.MutationPayload
import eu.pascariu.shoppinglist.data.local.MutationQueue
import eu.pascariu.shoppinglist.data.local.ShopDao
import eu.pascariu.shoppinglist.data.local.toEntity
import eu.pascariu.shoppinglist.data.local.toModel
import eu.pascariu.shoppinglist.data.model.CreateShopRequest
import eu.pascariu.shoppinglist.data.model.RenameShopRequest
import eu.pascariu.shoppinglist.data.model.Shop
import eu.pascariu.shoppinglist.di.ApplicationScope
import eu.pascariu.shoppinglist.domain.search.orderViewsGlobalFirst
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException
import java.util.UUID

@Singleton
class ShopsRepository @Inject constructor(
    private val shopsApi: ShopsApi,
    private val shopDao: ShopDao,
    private val mutationQueue: MutationQueue,
    @ApplicationScope externalScope: CoroutineScope,
) {

    val shops: StateFlow<List<Shop>> = shopDao.observeAll()
        .map { entities -> orderViewsGlobalFirst(entities.map { it.toModel() }) }
        .stateIn(externalScope, SharingStarted.Eagerly, emptyList())
    private val placeholderMutex = Mutex()
    private var nextPlaceholderCandidate = -1L

    suspend fun refetch() {
        shopDao.replaceAll(shopsApi.listShops().map { it.toEntity() })
    }

    suspend fun create(name: String): Shop {
        val request = CreateShopRequest(name = name, clientId = UUID.randomUUID().toString())
        if (mutationQueue.isOffline) {
            val placeholderId = nextPlaceholderId()
            val optimistic = Shop(
                id = placeholderId,
                name = name,
                createdAt = java.time.Instant.now().toString(),
            )
            shopDao.upsert(optimistic.toEntity())
            mutationQueue.enqueue(MutationPayload.ShopCreate(placeholderId, request))
            return optimistic
        }
        val created = shopsApi.createShop(request)
        shopDao.upsert(created.toEntity())
        return created
    }

    suspend fun rename(shopId: Long, name: String): Shop {
        if (mutationQueue.isOffline) {
            val previous = shopDao.getById(shopId)?.toModel()
                ?: throw IllegalArgumentException("shop $shopId not cached")
            val optimistic = previous.copy(name = name)
            shopDao.upsert(optimistic.toEntity())
            mutationQueue.enqueue(MutationPayload.ShopRename(shopId, RenameShopRequest(name = name)))
            return optimistic
        }
        val renamed = shopsApi.renameShop(shopId, RenameShopRequest(name = name))
        shopDao.upsert(renamed.toEntity())
        return renamed
    }

    suspend fun delete(shopId: Long) {
        if (mutationQueue.isOffline) {
            shopDao.deleteById(shopId)
            mutationQueue.enqueue(MutationPayload.ShopDelete(shopId))
            return
        }
        val response = shopsApi.deleteShop(shopId)
        if (!response.isSuccessful) throw HttpException(response)
        shopDao.deleteById(shopId)
    }

    suspend fun applyCreated(shop: Shop) {
        if (shopDao.getById(shop.id) != null) return
        shopDao.upsert(shop.toEntity())
    }

    suspend fun applyUpdated(shop: Shop) {
        val existing = shopDao.getById(shop.id)?.toModel() ?: return
        if (existing == shop) return
        shopDao.upsert(shop.toEntity())
    }

    suspend fun applyDeleted(shopId: Long) {
        shopDao.deleteById(shopId)
    }

    suspend fun remapPlaceholder(placeholderId: Long, realShop: Shop) {
        shopDao.deleteById(placeholderId)
        shopDao.upsert(realShop.toEntity())
    }

    private suspend fun nextPlaceholderId(): Long = placeholderMutex.withLock {
        var candidate = nextPlaceholderCandidate
        while (shopDao.getById(candidate) != null) candidate -= 1
        nextPlaceholderCandidate = candidate - 1
        candidate
    }
}

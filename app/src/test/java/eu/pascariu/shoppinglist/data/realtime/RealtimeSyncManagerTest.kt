package eu.pascariu.shoppinglist.data.realtime

import eu.pascariu.shoppinglist.data.api.ItemsApi
import eu.pascariu.shoppinglist.data.api.ShopsApi
import eu.pascariu.shoppinglist.data.model.CreateItemRequest
import eu.pascariu.shoppinglist.data.model.CreateShopRequest
import eu.pascariu.shoppinglist.data.model.Item
import eu.pascariu.shoppinglist.data.model.ItemEvent
import eu.pascariu.shoppinglist.data.model.RenameShopRequest
import eu.pascariu.shoppinglist.data.model.Shop
import eu.pascariu.shoppinglist.data.model.ShopEvent
import eu.pascariu.shoppinglist.data.model.UpdateItemRequest
import eu.pascariu.shoppinglist.data.repo.ItemsRepository
import eu.pascariu.shoppinglist.data.repo.ShopsRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Response

class RealtimeSyncManagerTest {

    @Test
    fun `connect signal triggers full refetch of items and shops`() = runTest {
        val itemsApi = FakeItemsApi(itemsForFetch = listOf(sampleItem()))
        val shopsApi = FakeShopsApi(shopsForFetch = listOf(sampleShop()))
        val client = FakeRealtimeClient()
        val manager = RealtimeSyncManager(client, ItemsRepository(itemsApi), ShopsRepository(shopsApi))

        manager.start(this)
        advanceUntilIdle()
        client.connectSignals.emit(RealtimeClient.Stream.ITEMS)
        client.connectSignals.emit(RealtimeClient.Stream.SHOPS)
        advanceUntilIdle()

        assertEquals(2, itemsApi.listCalls)
        assertEquals(2, shopsApi.listCalls)
        manager.stop()
    }

    @Test
    fun `item events are merged into the items cache`() = runTest {
        val itemsApi = FakeItemsApi(itemsForFetch = emptyList())
        val client = FakeRealtimeClient()
        val repository = ItemsRepository(itemsApi)
        val manager = RealtimeSyncManager(client, repository, ShopsRepository(FakeShopsApi(emptyList())))

        manager.start(this)
        advanceUntilIdle()
        client.itemEvents.emit(ItemEvent(type = "item:created", listId = 1, item = sampleItem()))
        client.itemEvents.emit(ItemEvent(type = "item:updated", listId = 1, item = sampleItem().copy(isChecked = true)))
        client.itemEvents.emit(ItemEvent(type = "item:deleted", listId = 1, itemId = 42))
        advanceUntilIdle()

        assertEquals(emptyList<Item>(), repository.items.value)
        manager.stop()
    }

    @Test
    fun `created item event appends to the cache`() = runTest {
        val itemsApi = FakeItemsApi(itemsForFetch = emptyList())
        val client = FakeRealtimeClient()
        val repository = ItemsRepository(itemsApi)
        val manager = RealtimeSyncManager(client, repository, ShopsRepository(FakeShopsApi(emptyList())))

        manager.start(this)
        advanceUntilIdle()
        client.connectSignals.emit(RealtimeClient.Stream.ITEMS)
        advanceUntilIdle()
        client.itemEvents.emit(ItemEvent(type = "item:created", listId = 1, item = sampleItem()))
        advanceUntilIdle()

        assertEquals(listOf(sampleItem()), repository.items.value)
        manager.stop()
    }

    @Test
    fun `shop events are merged into the shops cache`() = runTest {
        val shopsApi = FakeShopsApi(shopsForFetch = emptyList())
        val client = FakeRealtimeClient()
        val repository = ShopsRepository(shopsApi)
        val manager = RealtimeSyncManager(client, ItemsRepository(FakeItemsApi(emptyList())), repository)

        manager.start(this)
        advanceUntilIdle()
        client.shopEvents.emit(ShopEvent(type = "shop:created", shop = sampleShop()))
        client.shopEvents.emit(ShopEvent(type = "shop:deleted", shopId = 4))
        advanceUntilIdle()

        assertEquals(emptyList<Shop>(), repository.shops.value)
        manager.stop()
    }

    @Test
    fun `created shop event appends to the cache`() = runTest {
        val shopsApi = FakeShopsApi(shopsForFetch = emptyList())
        val client = FakeRealtimeClient()
        val repository = ShopsRepository(shopsApi)
        val manager = RealtimeSyncManager(client, ItemsRepository(FakeItemsApi(emptyList())), repository)

        manager.start(this)
        advanceUntilIdle()
        client.shopEvents.emit(ShopEvent(type = "shop:created", shop = sampleShop()))
        advanceUntilIdle()

        assertEquals(listOf(sampleShop()), repository.shops.value)
        manager.stop()
    }

    @Test
    fun `stop closes the client and stops processing events`() = runTest {
        val itemsApi = FakeItemsApi(itemsForFetch = emptyList())
        val client = FakeRealtimeClient()
        val repository = ItemsRepository(itemsApi)
        val manager = RealtimeSyncManager(client, repository, ShopsRepository(FakeShopsApi(emptyList())))

        manager.start(this)
        advanceUntilIdle()
        manager.stop()

        assertEquals(1, client.startCalls)
        assertEquals(1, client.stopCalls)

        client.itemEvents.emit(ItemEvent(type = "item:created", listId = 1, item = sampleItem()))
        advanceUntilIdle()

        assertEquals(emptyList<Item>(), repository.items.value)
    }
}

private fun sampleItem() = Item(
    id = 42,
    listId = 1,
    name = "Milk",
    isChecked = false,
    orderMap = mapOf("2" to "a0"),
    quantity = 1,
    isTemporary = false,
    checkCount = 0,
    expiresAt = null,
    createdAt = "2026-10-22T10:00:00.000Z",
    visibleIn = listOf(1L),
)

private fun sampleShop() = Shop(id = 4, name = "Global", createdAt = "2026-10-22T10:00:00.000Z")

private class FakeRealtimeClient : RealtimeClientContract {
    override val connectSignals = MutableSharedFlow<RealtimeClient.Stream>(extraBufferCapacity = 64)
    override val itemEvents = MutableSharedFlow<ItemEvent>(extraBufferCapacity = 64)
    override val shopEvents = MutableSharedFlow<ShopEvent>(extraBufferCapacity = 64)
    var startCalls = 0
    var stopCalls = 0

    override fun start() {
        startCalls++
    }

    override fun stop() {
        stopCalls++
    }
}

private class FakeItemsApi(var itemsForFetch: List<Item>) : ItemsApi {
    var listCalls = 0

    override suspend fun listItems(listId: Long): List<Item> {
        listCalls++
        return itemsForFetch
    }

    override suspend fun createItem(listId: Long, body: CreateItemRequest): Item = TODO()
    override suspend fun updateItem(itemId: Long, body: UpdateItemRequest): Item = TODO()
    override suspend fun deleteItem(itemId: Long): Response<Unit> = TODO()
}

private class FakeShopsApi(var shopsForFetch: List<Shop>) : ShopsApi {
    var listCalls = 0

    override suspend fun listShops(): List<Shop> {
        listCalls++
        return shopsForFetch
    }

    override suspend fun createShop(body: CreateShopRequest): Shop = TODO()
    override suspend fun renameShop(shopId: Long, body: RenameShopRequest): Shop = TODO()
    override suspend fun deleteShop(shopId: Long): Response<Unit> = TODO()
}

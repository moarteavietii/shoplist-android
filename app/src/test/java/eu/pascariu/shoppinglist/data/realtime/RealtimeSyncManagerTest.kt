package eu.pascariu.shoppinglist.data.realtime

import eu.pascariu.shoppinglist.data.api.ItemsApi
import eu.pascariu.shoppinglist.data.api.ShopsApi
import eu.pascariu.shoppinglist.data.device.DeviceIdStore
import eu.pascariu.shoppinglist.data.connectivity.BackendConnectivity
import eu.pascariu.shoppinglist.data.local.FakeItemDao
import eu.pascariu.shoppinglist.data.local.FakePendingMutationDao
import eu.pascariu.shoppinglist.data.local.MutationQueue
import eu.pascariu.shoppinglist.data.local.FakeShopDao
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class RealtimeSyncManagerTest {

    private val testScope = CoroutineScope(Dispatchers.Unconfined)

    private fun offlineAwareQueue(): MutationQueue = MutationQueue(
        dao = FakePendingMutationDao(),
        connectivity = MutableStateFlow(BackendConnectivity.REACHABLE),
        json = Json { ignoreUnknownKeys = true },
    )

    @Test
    fun `connect signal triggers full refetch of items and shops`() = runTest {
        val itemsApi = FakeItemsApi(itemsForFetch = listOf(sampleItem()))
        val shopsApi = FakeShopsApi(shopsForFetch = listOf(sampleShop()))
        val client = FakeRealtimeClient()
        val manager = RealtimeSyncManager(client, ItemsRepository(itemsApi, FakeItemDao(), offlineAwareQueue(), testScope), ShopsRepository(shopsApi, FakeShopDao(), offlineAwareQueue(), testScope), FakeDeviceIdStore(), ioDispatcher = Dispatchers.Unconfined)

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
        val repository = ItemsRepository(itemsApi, FakeItemDao(), offlineAwareQueue(), testScope)
        val manager = RealtimeSyncManager(client, repository, ShopsRepository(FakeShopsApi(emptyList()), FakeShopDao(), offlineAwareQueue(), testScope), FakeDeviceIdStore(), ioDispatcher = Dispatchers.Unconfined)

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
        val repository = ItemsRepository(itemsApi, FakeItemDao(), offlineAwareQueue(), testScope)
        val manager = RealtimeSyncManager(client, repository, ShopsRepository(FakeShopsApi(emptyList()), FakeShopDao(), offlineAwareQueue(), testScope), FakeDeviceIdStore(), ioDispatcher = Dispatchers.Unconfined)

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
        val repository = ShopsRepository(shopsApi, FakeShopDao(), offlineAwareQueue(), testScope)
        val manager = RealtimeSyncManager(client, ItemsRepository(FakeItemsApi(emptyList()), FakeItemDao(), offlineAwareQueue(), testScope), repository, FakeDeviceIdStore(), ioDispatcher = Dispatchers.Unconfined)

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
        val repository = ShopsRepository(shopsApi, FakeShopDao(), offlineAwareQueue(), testScope)
        val manager = RealtimeSyncManager(client, ItemsRepository(FakeItemsApi(emptyList()), FakeItemDao(), offlineAwareQueue(), testScope), repository, FakeDeviceIdStore(), ioDispatcher = Dispatchers.Unconfined)

        manager.start(this)
        advanceUntilIdle()
        client.shopEvents.emit(ShopEvent(type = "shop:created", shop = sampleShop()))
        advanceUntilIdle()

        assertEquals(listOf(sampleShop()), repository.shops.value)
        manager.stop()
    }

    @Test
    fun `failed refetch does not crash the sync collectors`() = runTest {
        val itemsApi = FakeItemsApi(itemsForFetch = listOf(sampleItem())).apply {
            listError = IOException("network down")
        }
        val shopsApi = FakeShopsApi(shopsForFetch = listOf(sampleShop()))
        val client = FakeRealtimeClient()
        val manager = RealtimeSyncManager(client, ItemsRepository(itemsApi, FakeItemDao(), offlineAwareQueue(), testScope), ShopsRepository(shopsApi, FakeShopDao(), offlineAwareQueue(), testScope), FakeDeviceIdStore(), ioDispatcher = Dispatchers.Unconfined)

        manager.start(this)
        advanceUntilIdle()
        client.connectSignals.emit(RealtimeClient.Stream.ITEMS)
        advanceUntilIdle()

        assertEquals(1, itemsApi.listCalls)
        assertEquals(1, shopsApi.listCalls)

        itemsApi.listError = null
        client.connectSignals.emit(RealtimeClient.Stream.ITEMS)
        advanceUntilIdle()

        assertEquals(2, itemsApi.listCalls)
        assertEquals(2, shopsApi.listCalls)
        manager.stop()
    }

    @Test
    fun `stop closes the client and stops processing events`() = runTest {
        val itemsApi = FakeItemsApi(itemsForFetch = emptyList())
        val client = FakeRealtimeClient()
        val repository = ItemsRepository(itemsApi, FakeItemDao(), offlineAwareQueue(), testScope)
        val manager = RealtimeSyncManager(client, repository, ShopsRepository(FakeShopsApi(emptyList()), FakeShopDao(), offlineAwareQueue(), testScope), FakeDeviceIdStore(), ioDispatcher = Dispatchers.Unconfined)

        manager.start(this)
        advanceUntilIdle()
        manager.stop()

        assertEquals(1, client.startCalls)
        assertEquals(1, client.stopCalls)

        client.itemEvents.emit(ItemEvent(type = "item:created", listId = 1, item = sampleItem()))
        advanceUntilIdle()

        assertEquals(emptyList<Item>(), repository.items.value)
    }

    @Test
    fun `restart after stop reconnects the client`() = runTest {
        val itemsApi = FakeItemsApi(itemsForFetch = emptyList())
        val client = FakeRealtimeClient()
        val manager = RealtimeSyncManager(client, ItemsRepository(itemsApi, FakeItemDao(), offlineAwareQueue(), testScope), ShopsRepository(FakeShopsApi(emptyList()), FakeShopDao(), offlineAwareQueue(), testScope), FakeDeviceIdStore(), ioDispatcher = Dispatchers.Unconfined)

        manager.start(this)
        advanceUntilIdle()
        manager.stop()
        manager.start(this)
        advanceUntilIdle()

        assertEquals(2, client.startCalls)
        assertEquals(1, client.stopCalls)
        manager.stop()
    }

    @Test
    fun `remote item event emits change toast labels`() = runTest {
        val itemsApi = FakeItemsApi(itemsForFetch = emptyList())
        val client = FakeRealtimeClient()
        val repository = ItemsRepository(itemsApi, FakeItemDao(), offlineAwareQueue(), testScope)
        val manager = RealtimeSyncManager(client, repository, ShopsRepository(FakeShopsApi(emptyList()), FakeShopDao(), offlineAwareQueue(), testScope), FakeDeviceIdStore(), ioDispatcher = Dispatchers.Unconfined)

        manager.start(this)
        advanceUntilIdle()
        client.connectSignals.emit(RealtimeClient.Stream.ITEMS)
        advanceUntilIdle()

        val labels = mutableListOf<List<ChangeToastLabel>>()
        val collector = launch { manager.changeToastLabels.collect { labels += it } }
        advanceUntilIdle()

        client.itemEvents.emit(ItemEvent(type = "item:created", listId = 1, item = sampleItem(), originDeviceId = "other-device"))
        advanceUntilIdle()

        assertEquals(listOf(listOf("new item: Milk")), labels.map { batch -> batch.map { it.text } })
        collector.cancel()
        manager.stop()
    }

    @Test
    fun `own-device echo does not emit change toast labels`() = runTest {
        val itemsApi = FakeItemsApi(itemsForFetch = emptyList())
        val client = FakeRealtimeClient()
        val repository = ItemsRepository(itemsApi, FakeItemDao(), offlineAwareQueue(), testScope)
        val manager = RealtimeSyncManager(client, repository, ShopsRepository(FakeShopsApi(emptyList()), FakeShopDao(), offlineAwareQueue(), testScope), FakeDeviceIdStore("device-A"), ioDispatcher = Dispatchers.Unconfined)

        manager.start(this)
        advanceUntilIdle()
        client.connectSignals.emit(RealtimeClient.Stream.ITEMS)
        advanceUntilIdle()

        val labels = mutableListOf<List<ChangeToastLabel>>()
        val collector = launch { manager.changeToastLabels.collect { labels += it } }
        advanceUntilIdle()

        client.itemEvents.emit(ItemEvent(type = "item:created", listId = 1, item = sampleItem(), originDeviceId = "device-A"))
        advanceUntilIdle()

        assertEquals(0, labels.size)
        collector.cancel()
        manager.stop()
    }

    @Test
    fun `event without origin device emits change toast labels`() = runTest {
        val itemsApi = FakeItemsApi(itemsForFetch = emptyList())
        val client = FakeRealtimeClient()
        val repository = ItemsRepository(itemsApi, FakeItemDao(), offlineAwareQueue(), testScope)
        val manager = RealtimeSyncManager(client, repository, ShopsRepository(FakeShopsApi(emptyList()), FakeShopDao(), offlineAwareQueue(), testScope), FakeDeviceIdStore("device-A"), ioDispatcher = Dispatchers.Unconfined)

        manager.start(this)
        advanceUntilIdle()
        client.connectSignals.emit(RealtimeClient.Stream.ITEMS)
        advanceUntilIdle()

        val labels = mutableListOf<List<ChangeToastLabel>>()
        val collector = launch { manager.changeToastLabels.collect { labels += it } }
        advanceUntilIdle()

        client.itemEvents.emit(ItemEvent(type = "item:created", listId = 1, item = sampleItem(), originDeviceId = null))
        advanceUntilIdle()

        assertEquals(listOf(listOf("new item: Milk")), labels.map { batch -> batch.map { it.text } })
        collector.cancel()
        manager.stop()
    }

    @Test
    fun `event for another list does not emit change toast labels`() = runTest {
        val itemsApi = FakeItemsApi(itemsForFetch = emptyList())
        val client = FakeRealtimeClient()
        val repository = ItemsRepository(itemsApi, FakeItemDao(), offlineAwareQueue(), testScope)
        val manager = RealtimeSyncManager(client, repository, ShopsRepository(FakeShopsApi(emptyList()), FakeShopDao(), offlineAwareQueue(), testScope), FakeDeviceIdStore(), ioDispatcher = Dispatchers.Unconfined)

        manager.start(this)
        advanceUntilIdle()
        client.connectSignals.emit(RealtimeClient.Stream.ITEMS)
        advanceUntilIdle()

        val labels = mutableListOf<List<ChangeToastLabel>>()
        val collector = launch { manager.changeToastLabels.collect { labels += it } }
        advanceUntilIdle()

        client.itemEvents.emit(ItemEvent(type = "item:created", listId = 999, item = sampleItem(), originDeviceId = "other-device"))
        advanceUntilIdle()

        assertEquals(0, labels.size)
        collector.cancel()
        manager.stop()
    }

    @Test
    fun `no-op update emits no change toast labels`() = runTest {
        val itemsApi = FakeItemsApi(itemsForFetch = listOf(sampleItem()))
        val client = FakeRealtimeClient()
        val repository = ItemsRepository(itemsApi, FakeItemDao(), offlineAwareQueue(), testScope)
        val manager = RealtimeSyncManager(client, repository, ShopsRepository(FakeShopsApi(emptyList()), FakeShopDao(), offlineAwareQueue(), testScope), FakeDeviceIdStore(), ioDispatcher = Dispatchers.Unconfined)

        manager.start(this)
        advanceUntilIdle()
        client.connectSignals.emit(RealtimeClient.Stream.ITEMS)
        advanceUntilIdle()

        val labels = mutableListOf<List<ChangeToastLabel>>()
        val collector = launch { manager.changeToastLabels.collect { labels += it } }
        advanceUntilIdle()

        client.itemEvents.emit(ItemEvent(type = "item:updated", listId = 1, item = sampleItem(), originDeviceId = "other-device"))
        advanceUntilIdle()

        assertEquals(0, labels.size)
        collector.cancel()
        manager.stop()
    }

    @Test
    fun `remote delete emits label with the pre-delete name`() = runTest {
        val itemsApi = FakeItemsApi(itemsForFetch = listOf(sampleItem()))
        val client = FakeRealtimeClient()
        val repository = ItemsRepository(itemsApi, FakeItemDao(), offlineAwareQueue(), testScope)
        val manager = RealtimeSyncManager(client, repository, ShopsRepository(FakeShopsApi(emptyList()), FakeShopDao(), offlineAwareQueue(), testScope), FakeDeviceIdStore(), ioDispatcher = Dispatchers.Unconfined)

        manager.start(this)
        advanceUntilIdle()
        client.connectSignals.emit(RealtimeClient.Stream.ITEMS)
        advanceUntilIdle()

        val labels = mutableListOf<List<ChangeToastLabel>>()
        val collector = launch { manager.changeToastLabels.collect { labels += it } }
        advanceUntilIdle()

        client.itemEvents.emit(ItemEvent(type = "item:deleted", listId = 1, itemId = 42, originDeviceId = "other-device"))
        advanceUntilIdle()

        assertEquals(listOf(listOf("deleted: Milk")), labels.map { batch -> batch.map { it.text } })
        collector.cancel()
        manager.stop()
    }
}

private class FakeDeviceIdStore(private val deviceId: String = "device-under-test") : DeviceIdStore {
    override fun getOrCreate(): String = deviceId
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
    var listError: IOException? = null

    override suspend fun listItems(listId: Long): List<Item> {
        listCalls++
        listError?.let { throw it }
        return itemsForFetch
    }

    override suspend fun createItem(listId: Long, body: CreateItemRequest): Item = TODO()
    override suspend fun updateItem(itemId: Long, body: UpdateItemRequest): Item = TODO()
    override suspend fun deleteItem(itemId: Long): Response<Unit> = TODO()
}

private class FakeShopsApi(var shopsForFetch: List<Shop>) : ShopsApi {
    var listCalls = 0
    var listError: IOException? = null

    override suspend fun listShops(): List<Shop> {
        listCalls++
        listError?.let { throw it }
        return shopsForFetch
    }

    override suspend fun createShop(body: CreateShopRequest): Shop = TODO()
    override suspend fun renameShop(shopId: Long, body: RenameShopRequest): Shop = TODO()
    override suspend fun deleteShop(shopId: Long): Response<Unit> = TODO()
}

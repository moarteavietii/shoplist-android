package eu.pascariu.shoppinglist.data.realtime

import eu.pascariu.shoppinglist.data.api.ItemsApi
import eu.pascariu.shoppinglist.data.api.ShopsApi
import eu.pascariu.shoppinglist.data.connectivity.BackendConnectivity
import eu.pascariu.shoppinglist.data.device.DeviceIdStore
import eu.pascariu.shoppinglist.data.local.FakeItemDao
import eu.pascariu.shoppinglist.data.local.FakePendingMutationDao
import eu.pascariu.shoppinglist.data.local.FakeShopDao
import eu.pascariu.shoppinglist.data.local.MutationDrainer
import eu.pascariu.shoppinglist.data.local.MutationPayload
import eu.pascariu.shoppinglist.data.local.MutationQueue
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
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Response

class RealtimeLifecycleTest {

    private val testScope = CoroutineScope(Dispatchers.Unconfined)
    private val json = Json { ignoreUnknownKeys = true }

    private fun queue(connectivity: MutableStateFlow<BackendConnectivity>) = MutationQueue(
        dao = FakePendingMutationDao(),
        connectivity = connectivity,
        json = json,
    )

    private fun controller(
        scope: CoroutineScope,
        itemsApi: GatedItemsApi,
        mutationQueue: MutationQueue,
    ): ConnectivityDrainController {
        val syncManager = RealtimeSyncManager(
            realtimeClient = LifecycleFakeRealtimeClient(),
            itemsRepository = ItemsRepository(itemsApi, FakeItemDao(), mutationQueue, testScope),
            shopsRepository = ShopsRepository(NoopShopsApi(), FakeShopDao(), mutationQueue, testScope),
            deviceIdStore = LifecycleFakeDeviceIdStore(),
            ioDispatcher = Dispatchers.Unconfined,
        )
        val drainer = MutationDrainer(
            queue = mutationQueue,
            itemsApi = itemsApi,
            shopsApi = NoopShopsApi(),
            itemsRepository = ItemsRepository(itemsApi, FakeItemDao(), mutationQueue, testScope),
            shopsRepository = ShopsRepository(NoopShopsApi(), FakeShopDao(), mutationQueue, testScope),
            json = json,
            ioDispatcher = Dispatchers.Unconfined,
        )
        return ConnectivityDrainController(scope, drainer, syncManager)
    }

    @Test
    fun `unreachable cancels an in-flight drain and keeps mutations queued`() = runTest {
        val connectivity = MutableStateFlow(BackendConnectivity.REACHABLE)
        val itemsApi = GatedItemsApi()
        val mutationQueue = queue(connectivity)
        val controller = controller(this, itemsApi, mutationQueue)
        mutationQueue.enqueue(MutationPayload.ItemCheck(itemId = 1, isChecked = true))
        mutationQueue.enqueue(MutationPayload.ItemCheck(itemId = 2, isChecked = true))

        controller.relaunchDrain()
        advanceUntilIdle()
        assertEquals(1, itemsApi.updateCalls)

        controller.onConnectivity(BackendConnectivity.UNREACHABLE)
        itemsApi.gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(1, itemsApi.updateCalls)
        assertEquals(2, mutationQueue.listAll().size)
    }

    @Test
    fun `duplicate reachable relaunch cancels the previous drain`() = runTest {
        val connectivity = MutableStateFlow(BackendConnectivity.REACHABLE)
        val itemsApi = GatedItemsApi()
        val mutationQueue = queue(connectivity)
        val controller = controller(this, itemsApi, mutationQueue)
        mutationQueue.enqueue(MutationPayload.ItemCheck(itemId = 1, isChecked = true))
        mutationQueue.enqueue(MutationPayload.ItemCheck(itemId = 2, isChecked = true))

        controller.relaunchDrain()
        advanceUntilIdle()
        assertEquals(1, itemsApi.updateCalls)

        controller.onConnectivity(BackendConnectivity.REACHABLE)
        itemsApi.gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(0, mutationQueue.listAll().size)
        assertEquals(3, itemsApi.updateCalls)
    }

    @Test
    fun `unreachable with no drain running does nothing`() = runTest {
        val connectivity = MutableStateFlow(BackendConnectivity.REACHABLE)
        val itemsApi = GatedItemsApi()
        val mutationQueue = queue(connectivity)
        val controller = controller(this, itemsApi, mutationQueue)
        mutationQueue.enqueue(MutationPayload.ItemCheck(itemId = 1, isChecked = true))

        controller.onConnectivity(BackendConnectivity.UNREACHABLE)
        advanceUntilIdle()

        assertEquals(0, itemsApi.updateCalls)
        assertEquals(1, mutationQueue.listAll().size)
    }
}

private class LifecycleFakeDeviceIdStore : DeviceIdStore {
    override fun getOrCreate(): String = "device-under-test"
}

private class LifecycleFakeRealtimeClient : RealtimeClientContract {
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

private class NoopShopsApi : ShopsApi {
    override suspend fun listShops(): List<Shop> = emptyList()
    override suspend fun createShop(body: CreateShopRequest): Shop = throw AssertionError("not expected")
    override suspend fun renameShop(shopId: Long, body: RenameShopRequest): Shop = throw AssertionError("not expected")
    override suspend fun deleteShop(shopId: Long): Response<Unit> = Response.success(Unit)
}

private class GatedItemsApi : ItemsApi {
    val gate = CompletableDeferred<Unit>()
    var updateCalls = 0
    private var gatedCallDone = false

    override suspend fun listItems(listId: Long): List<Item> = emptyList()

    override suspend fun createItem(listId: Long, body: CreateItemRequest): Item = throw AssertionError("not expected")

    override suspend fun updateItem(itemId: Long, body: UpdateItemRequest): Item {
        updateCalls++
        if (!gatedCallDone) {
            gatedCallDone = true
            gate.await()
        }
        return sampleItem(id = itemId, isChecked = body.isChecked ?: false)
    }

    override suspend fun deleteItem(itemId: Long): Response<Unit> = Response.success(Unit)
}

private fun sampleItem(id: Long, isChecked: Boolean): Item = Item(
    id = id,
    listId = 1,
    name = "Milk",
    isChecked = isChecked,
    orderMap = emptyMap(),
    quantity = 1,
    isTemporary = false,
    checkCount = 0,
    expiresAt = null,
    createdAt = "2026-10-22T10:00:00.000Z",
    visibleIn = listOf(1L),
)

package eu.pascariu.shoppinglist.data.local

import eu.pascariu.shoppinglist.data.api.ItemsApi
import eu.pascariu.shoppinglist.data.api.ShopsApi
import eu.pascariu.shoppinglist.data.connectivity.BackendConnectivity
import eu.pascariu.shoppinglist.data.model.CreateItemRequest
import eu.pascariu.shoppinglist.data.model.CreateShopRequest
import eu.pascariu.shoppinglist.data.model.Item
import eu.pascariu.shoppinglist.data.model.RenameShopRequest
import eu.pascariu.shoppinglist.data.model.Shop
import eu.pascariu.shoppinglist.data.model.UpdateItemRequest
import eu.pascariu.shoppinglist.data.repo.ItemsRepository
import eu.pascariu.shoppinglist.data.repo.ShopsRepository
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class MutationDrainerTest {

    private val testScope = CoroutineScope(Dispatchers.Unconfined)
    private val json = Json { ignoreUnknownKeys = true }

    private fun queue(dao: FakePendingMutationDao = FakePendingMutationDao()): MutationQueue =
        MutationQueue(dao, MutableStateFlow(BackendConnectivity.REACHABLE), json)

    private fun drainer(
        itemsApi: ItemsApi,
        queue: MutationQueue,
        itemsRepository: ItemsRepository = ItemsRepository(itemsApi, FakeItemDao(), queue, testScope),
    ): MutationDrainer = MutationDrainer(
        queue = queue,
        itemsApi = itemsApi,
        shopsApi = RecordingShopsApi(),
        itemsRepository = itemsRepository,
        shopsRepository = ShopsRepository(RecordingShopsApi(), FakeShopDao(), queue, testScope),
        json = json,
        ioDispatcher = Dispatchers.Unconfined,
    )

    @Test
    fun `drain sends mutations in enqueue order and empties the queue`() = runTest {
        val itemsApi = RecordingItemsApi()
        val queue = queue()
        queue.enqueue(MutationPayload.ItemCheck(itemId = 1, isChecked = true))
        queue.enqueue(MutationPayload.ItemCheck(itemId = 2, isChecked = true))

        drainer(itemsApi = itemsApi, queue = queue).drainAll()

        assertEquals(listOf(1L, 2L), itemsApi.checkedItemIds)
        assertEquals(0, queue.listAll().size)
    }

    @Test
    fun `network error stops the drain and keeps remaining mutations`() = runTest {
        val itemsApi = RecordingItemsApi().apply { checkError = IOException("offline") }
        val queue = queue()
        queue.enqueue(MutationPayload.ItemCheck(itemId = 1, isChecked = true))
        queue.enqueue(MutationPayload.ItemCheck(itemId = 2, isChecked = true))

        drainer(itemsApi = itemsApi, queue = queue).drainAll()

        assertEquals(2, queue.listAll().size)
    }

    @Test
    fun `409 drops the mutation and continues draining`() = runTest {
        val itemsApi = RecordingItemsApi().apply { checkErrorCode = 409 }
        val queue = queue()
        queue.enqueue(MutationPayload.ItemCheck(itemId = 1, isChecked = true))
        queue.enqueue(MutationPayload.ItemCheck(itemId = 2, isChecked = true))

        val drainer = drainer(itemsApi = itemsApi, queue = queue)
        drainer.drainAll()

        assertEquals(0, queue.listAll().size)
        assertEquals(2, itemsApi.updateCalls)
    }

    @Test
    fun `403 auth failure stops the drain and keeps mutations queued`() = runTest {
        val itemsApi = RecordingItemsApi().apply { checkErrorCode = 403 }
        val queue = queue()
        queue.enqueue(MutationPayload.ItemCheck(itemId = 1, isChecked = true))
        queue.enqueue(MutationPayload.ItemCheck(itemId = 2, isChecked = true))

        drainer(itemsApi = itemsApi, queue = queue).drainAll()

        assertEquals(2, queue.listAll().size)
        assertEquals(1, itemsApi.updateCalls)
    }

    @Test
    fun `item delete 404 drops the mutation and continues draining`() = runTest {
        val itemsApi = RecordingItemsApi().apply { deleteErrorCode = 404 }
        val queue = queue()
        queue.enqueue(MutationPayload.ItemDelete(itemId = 1))
        queue.enqueue(MutationPayload.ItemCheck(itemId = 2, isChecked = true))

        drainer(itemsApi = itemsApi, queue = queue).drainAll()

        assertEquals(0, queue.listAll().size)
        assertEquals(1, itemsApi.deleteCalls)
    }

    @Test
    fun `item delete transient server error keeps the mutation queued`() = runTest {
        val itemsApi = RecordingItemsApi().apply { deleteErrorCode = 500 }
        val queue = queue()
        queue.enqueue(MutationPayload.ItemDelete(itemId = 1))
        queue.enqueue(MutationPayload.ItemCheck(itemId = 2, isChecked = true))

        drainer(itemsApi = itemsApi, queue = queue).drainAll()

        assertEquals(2, queue.listAll().size)
        assertEquals(1, itemsApi.deleteCalls)
    }

    @Test
    fun `shop delete transient server error keeps the mutation queued`() = runTest {
        val shopsApi = RecordingShopsApi().apply { deleteErrorCode = 502 }
        val queue = queue()
        queue.enqueue(MutationPayload.ShopDelete(shopId = 1))

        val drainer = MutationDrainer(
            queue = queue,
            itemsApi = RecordingItemsApi(),
            shopsApi = shopsApi,
            itemsRepository = ItemsRepository(RecordingItemsApi(), FakeItemDao(), queue, testScope),
            shopsRepository = ShopsRepository(shopsApi, FakeShopDao(), queue, testScope),
            json = json,
            ioDispatcher = Dispatchers.Unconfined,
        )
        drainer.drainAll()

        assertEquals(1, queue.listAll().size)
        assertEquals(1, shopsApi.deleteCalls)
    }

    @Test
    fun `successful item create remaps the placeholder row and follow-ups`() = runTest {
        val itemsApi = RecordingItemsApi().apply { createdItem = sampleItem(id = 77) }
        val itemDao = FakeItemDao()
        val repository = ItemsRepository(itemsApi, itemDao, queue(), testScope)
        itemDao.upsert(sampleItem(id = -1).toEntity())
        val queue = queue()
        queue.enqueue(MutationPayload.ItemCreate(listId = 1, placeholderId = -1, request = CreateItemRequest(name = "Milk")))
        queue.enqueue(MutationPayload.ItemCheck(itemId = -1, isChecked = true))

        drainer(itemsApi = itemsApi, queue = queue, itemsRepository = repository).drainAll()

        assertEquals(0, queue.listAll().size)
        assertEquals(77L, itemDao.getById(77L)?.id)
        assertNull(itemDao.getById(-1))
    }
}

private fun sampleItem(id: Long): Item = Item(
    id = id,
    listId = 1,
    name = "Milk",
    isChecked = false,
    orderMap = emptyMap(),
    quantity = 1,
    isTemporary = false,
    checkCount = 0,
    expiresAt = null,
    createdAt = "2026-10-22T10:00:00.000Z",
    visibleIn = listOf(1L),
)

private fun sampleItem(id: Long, isChecked: Boolean): Item = sampleItem(id).copy(isChecked = isChecked)

private class RecordingItemsApi : ItemsApi {
    val checkedItemIds = mutableListOf<Long>()
    var updateCalls = 0
    var deleteCalls = 0
    var checkError: IOException? = null
    var checkErrorCode: Int? = null
    var deleteErrorCode: Int? = null
    var createdItem: Item? = null

    override suspend fun listItems(listId: Long): List<Item> = emptyList()

    override suspend fun createItem(listId: Long, body: CreateItemRequest): Item {
        return requireNotNull(createdItem)
    }

    override suspend fun updateItem(itemId: Long, body: UpdateItemRequest): Item {
        updateCalls++
        if (body.isChecked != null) checkedItemIds.add(itemId)
        checkError?.let { throw it }
        checkErrorCode?.let { code ->
            throw HttpException(Response.error<Any>(code, okhttp3.ResponseBody.create(null, "")))
        }
        return sampleItem(id = itemId, isChecked = body.isChecked ?: false)
    }

    override suspend fun deleteItem(itemId: Long): Response<Unit> {
        deleteCalls++
        deleteErrorCode?.let { code -> return Response.error(code, okhttp3.ResponseBody.create(null, "")) }
        return Response.success(Unit)
    }
}

private class RecordingShopsApi : ShopsApi {
    var deleteCalls = 0
    var deleteErrorCode: Int? = null

    override suspend fun listShops(): List<Shop> = emptyList()
    override suspend fun createShop(body: CreateShopRequest): Shop {
        throw AssertionError("not expected in this test")
    }
    override suspend fun renameShop(shopId: Long, body: RenameShopRequest): Shop {
        throw AssertionError("not expected in this test")
    }
    override suspend fun deleteShop(shopId: Long): Response<Unit> {
        deleteCalls++
        deleteErrorCode?.let { code -> return Response.error(code, okhttp3.ResponseBody.create(null, "")) }
        return Response.success(Unit)
    }
}

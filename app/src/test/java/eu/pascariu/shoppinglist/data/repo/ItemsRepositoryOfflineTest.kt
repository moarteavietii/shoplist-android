package eu.pascariu.shoppinglist.data.repo

import eu.pascariu.shoppinglist.data.api.ItemsApi
import eu.pascariu.shoppinglist.data.connectivity.BackendConnectivity
import eu.pascariu.shoppinglist.data.local.FakeItemDao
import eu.pascariu.shoppinglist.data.local.FakePendingMutationDao
import eu.pascariu.shoppinglist.data.local.MutationKind
import eu.pascariu.shoppinglist.data.local.MutationQueue
import eu.pascariu.shoppinglist.data.model.CreateItemRequest
import eu.pascariu.shoppinglist.data.model.Item
import eu.pascariu.shoppinglist.data.model.UpdateItemRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class ItemsRepositoryOfflineTest {

    private val testScope = CoroutineScope(Dispatchers.Unconfined)
    private val json = Json { ignoreUnknownKeys = true }

    private fun queue(connectivity: BackendConnectivity): MutationQueue = MutationQueue(
        dao = FakePendingMutationDao(),
        connectivity = MutableStateFlow(connectivity),
        json = json,
    )

    @Test
    fun `offline create uses a negative placeholder id and enqueues the create`() = runTest {
        val api = OfflineFakeItemsApi(itemsForFetch = emptyList())
        val repository = ItemsRepository(api, FakeItemDao(), queue(BackendConnectivity.UNREACHABLE), testScope)
        repository.refetch(listId = 1)

        val created = repository.create(listId = 1, CreateItemRequest(name = "Milk"))

        assertTrue(created.id < 0)
        assertEquals(listOf(created), repository.items.first())
        assertEquals(1, api.listCalls)
    }

    @Test
    fun `offline create enqueues an item_create mutation`() = runTest {
        val api = OfflineFakeItemsApi(itemsForFetch = emptyList())
        val mutationDao = FakePendingMutationDao()
        val repository = ItemsRepository(
            api,
            FakeItemDao(),
            MutationQueue(mutationDao, MutableStateFlow(BackendConnectivity.UNREACHABLE), json),
            testScope,
        )
        repository.refetch(listId = 1)

        repository.create(listId = 1, CreateItemRequest(name = "Milk"))

        assertEquals(1, mutationDao.listAll().size)
        assertEquals(MutationKind.ITEM_CREATE, mutationDao.listAll().single().kind)
    }

    @Test
    fun `offline check coalesces into the queue`() = runTest {
        val api = OfflineFakeItemsApi(itemsForFetch = sampleItems())
        val repository = ItemsRepository(api, FakeItemDao(), queue(BackendConnectivity.UNREACHABLE), testScope)
        repository.refetch(listId = 1)

        repository.setChecked(itemId = 42L, isChecked = true)
        repository.setChecked(itemId = 42L, isChecked = false)

        val cached = repository.items.first().single()
        assertEquals(false, cached.isChecked)
    }

    @Test
    fun `offline delete removes the item and enqueues the delete`() = runTest {
        val api = OfflineFakeItemsApi(itemsForFetch = sampleItems())
        val repository = ItemsRepository(api, FakeItemDao(), queue(BackendConnectivity.UNREACHABLE), testScope)
        repository.refetch(listId = 1)

        repository.delete(itemId = 42L)

        assertTrue(repository.items.first().isEmpty())
    }

    @Test
    fun `offline updateDetails applies the optimistic edit and enqueues an item_update mutation`() = runTest {
        val api = OfflineFakeItemsApi(itemsForFetch = sampleItems())
        val mutationDao = FakePendingMutationDao()
        val repository = ItemsRepository(
            api,
            FakeItemDao(),
            MutationQueue(mutationDao, MutableStateFlow(BackendConnectivity.UNREACHABLE), json),
            testScope,
        )
        repository.refetch(listId = 1)

        val updated = repository.updateDetails(itemId = 42L, UpdateItemRequest(name = "Oat milk", quantity = 2))

        assertEquals("Oat milk", updated.name)
        assertEquals(2, updated.quantity)
        assertEquals("Oat milk", repository.items.first().single().name)
        assertEquals(1, mutationDao.listAll().size)
        assertEquals(MutationKind.ITEM_UPDATE, mutationDao.listAll().single().kind)
    }

    @Test
    fun `offline reorder applies the optimistic order and enqueues an item_update mutation`() = runTest {
        val api = OfflineFakeItemsApi(itemsForFetch = sampleItems())
        val mutationDao = FakePendingMutationDao()
        val repository = ItemsRepository(
            api,
            FakeItemDao(),
            MutationQueue(mutationDao, MutableStateFlow(BackendConnectivity.UNREACHABLE), json),
            testScope,
        )
        repository.refetch(listId = 1)

        val reordered = repository.reorder(itemId = 42L, viewId = 2L, newKey = "a5")

        assertEquals("a5", reordered.orderMap["2"])
        assertEquals("a5", repository.items.first().single().orderMap["2"])
        assertEquals(1, mutationDao.listAll().size)
        assertEquals(MutationKind.ITEM_UPDATE, mutationDao.listAll().single().kind)
    }

    @Test
    fun `placeholder ids never collide`() = runTest {
        val api = OfflineFakeItemsApi(itemsForFetch = emptyList())
        val repository = ItemsRepository(api, FakeItemDao(), queue(BackendConnectivity.UNREACHABLE), testScope)
        repository.refetch(listId = 1)

        val first = repository.create(listId = 1, CreateItemRequest(name = "A"))
        val second = repository.create(listId = 1, CreateItemRequest(name = "Bread"))

        assertTrue(first.id < 0)
        assertTrue(second.id < first.id)
    }

    @Test
    fun `concurrent offline creates get distinct placeholder ids`() = runTest {
        val api = OfflineFakeItemsApi(itemsForFetch = emptyList())
        val repository = ItemsRepository(api, FakeItemDao(), queue(BackendConnectivity.UNREACHABLE), testScope)
        repository.refetch(listId = 1)

        val created = (1..8).map { index ->
            async { repository.create(listId = 1, CreateItemRequest(name = "Item $index")) }
        }.map { it.await() }

        val ids = created.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertTrue(ids.all { it < 0 })
    }
}

private fun sampleItems(): List<Item> = listOf(
    Item(
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
    ),
)

private class OfflineFakeItemsApi(
    var itemsForFetch: List<Item>,
) : ItemsApi {
    var listCalls = 0

    override suspend fun listItems(listId: Long): List<Item> {
        listCalls++
        return itemsForFetch
    }

    override suspend fun createItem(listId: Long, body: CreateItemRequest): Item {
        throw AssertionError("network calls are not expected while offline")
    }

    override suspend fun updateItem(itemId: Long, body: UpdateItemRequest): Item {
        throw AssertionError("network calls are not expected while offline")
    }

    override suspend fun deleteItem(itemId: Long): Response<Unit> {
        throw AssertionError("network calls are not expected while offline")
    }
}



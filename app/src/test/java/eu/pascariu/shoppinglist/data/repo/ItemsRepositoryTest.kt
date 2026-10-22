package eu.pascariu.shoppinglist.data.repo

import eu.pascariu.shoppinglist.data.api.ItemsApi
import eu.pascariu.shoppinglist.data.model.CreateItemRequest
import eu.pascariu.shoppinglist.data.model.Item
import eu.pascariu.shoppinglist.data.model.UpdateItemRequest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class ItemsRepositoryTest {

    @Test
    fun `first fetch populates cache and later reads are served from it`() = runTest {
        val api = FakeItemsApi(itemsForFetch = sampleItems())
        val repository = ItemsRepository(api)

        repository.refetch(listId = 1)

        assertEquals(sampleItems(), repository.items.first())
        assertEquals(1, api.listCalls)
    }

    @Test
    fun `refetch replaces cache wholesale`() = runTest {
        val api = FakeItemsApi(itemsForFetch = sampleItems())
        val repository = ItemsRepository(api)
        repository.refetch(listId = 1)

        api.itemsForFetch = listOf(sampleItems().first().copy(name = "Refreshed"))
        repository.refetch(listId = 1)

        assertEquals(listOf("Refreshed"), repository.items.first().map { it.name })
        assertEquals(2, api.listCalls)
    }

    @Test
    fun `404 on fetch surfaces error and leaves cache unchanged`() = runTest {
        val api = FakeItemsApi(itemsForFetch = sampleItems())
        val repository = ItemsRepository(api)
        repository.refetch(listId = 1)
        api.listError = httpException(404)

        val thrown = runCatching { repository.refetch(listId = 1) }

        assertTrue(thrown.exceptionOrNull() is HttpException)
        assertEquals(sampleItems(), repository.items.first())
    }

    @Test
    fun `create adds server-returned item to cache`() = runTest {
        val api = FakeItemsApi(itemsForFetch = emptyList())
        val repository = ItemsRepository(api)
        repository.refetch(listId = 1)
        val created = sampleItems().first()

        api.createdItem = created
        val request = CreateItemRequest(name = "Milk")
        repository.create(listId = 1, request)

        assertEquals(1L, api.lastCreateListId)
        assertEquals(request, api.lastCreateRequest)
        assertEquals(listOf(created), repository.items.first())
    }

    @Test
    fun `update replaces cached entry with server state`() = runTest {
        val api = FakeItemsApi(itemsForFetch = sampleItems())
        val repository = ItemsRepository(api)
        repository.refetch(listId = 1)
        val updated = sampleItems().first().copy(isChecked = true)

        api.updatedItem = updated
        val request = UpdateItemRequest(isChecked = true)
        repository.update(updated.id, request)

        assertEquals(updated.id, api.lastUpdateItemId)
        assertEquals(request, api.lastUpdateRequest)
        assertEquals(updated, repository.items.first().single())
    }

    @Test
    fun `create for a different list leaves cache untouched`() = runTest {
        val api = FakeItemsApi(itemsForFetch = sampleItems())
        val repository = ItemsRepository(api)
        repository.refetch(listId = 1)

        api.createdItem = sampleItems().first().copy(id = 7, listId = 2)
        repository.create(listId = 2, CreateItemRequest(name = "Other"))

        assertEquals(sampleItems(), repository.items.first())
        assertEquals(1L, repository.cachedListIdFlow.first())
    }

    @Test
    fun `update of item from another list leaves cache untouched`() = runTest {
        val api = FakeItemsApi(itemsForFetch = sampleItems())
        val repository = ItemsRepository(api)
        repository.refetch(listId = 1)
        val otherListItem = sampleItems().first().copy(id = 7, listId = 2)

        api.updatedItem = otherListItem
        repository.update(itemId = 7, UpdateItemRequest(isChecked = true))

        assertEquals(sampleItems(), repository.items.first())
    }

    @Test
    fun `update of item absent from cache inserts it`() = runTest {
        val api = FakeItemsApi(itemsForFetch = emptyList())
        val repository = ItemsRepository(api)
        repository.refetch(listId = 1)
        val updated = sampleItems().first().copy(isChecked = true)

        api.updatedItem = updated
        repository.update(itemId = updated.id, UpdateItemRequest(isChecked = true))

        assertEquals(listOf(updated), repository.items.first())
    }

    @Test
    fun `refetching another list retargets the cache`() = runTest {
        val api = FakeItemsApi(itemsForFetch = sampleItems())
        val repository = ItemsRepository(api)
        repository.refetch(listId = 1)
        val otherListItems = listOf(sampleItems().first().copy(id = 9, listId = 2))

        api.itemsForFetch = otherListItems
        repository.refetch(listId = 2)

        assertEquals(otherListItems, repository.items.first())
        assertEquals(2L, repository.cachedListIdFlow.first())
    }

    @Test
    fun `delete removes cached entry`() = runTest {
        val api = FakeItemsApi(itemsForFetch = sampleItems())
        val repository = ItemsRepository(api)
        repository.refetch(listId = 1)

        repository.delete(itemId = 42L)

        assertTrue(repository.items.first().isEmpty())
        assertTrue(api.deleted)
        assertEquals(42L, api.lastDeleteItemId)
    }

    @Test
    fun `409 on create surfaces error and leaves cache unchanged`() = runTest {
        val api = FakeItemsApi(itemsForFetch = sampleItems())
        val repository = ItemsRepository(api)
        repository.refetch(listId = 1)
        api.createError = httpException(409)

        val thrown = runCatching { repository.create(listId = 1, CreateItemRequest(name = "Milk")) }

        assertTrue(thrown.exceptionOrNull() is HttpException)
        assertEquals(sampleItems(), repository.items.first())
    }

    @Test
    fun `422 on update surfaces error and leaves cache unchanged`() = runTest {
        val api = FakeItemsApi(itemsForFetch = sampleItems())
        val repository = ItemsRepository(api)
        repository.refetch(listId = 1)
        api.updateError = httpException(422)

        val thrown = runCatching { repository.update(itemId = 42L, UpdateItemRequest(isChecked = true)) }

        assertTrue(thrown.exceptionOrNull() is HttpException)
        assertEquals(sampleItems(), repository.items.first())
    }

    @Test
    fun `404 on delete surfaces error and leaves cache unchanged`() = runTest {
        val api = FakeItemsApi(itemsForFetch = sampleItems())
        val repository = ItemsRepository(api)
        repository.refetch(listId = 1)
        api.deleteResponse = Response.error(404, okhttp3.ResponseBody.create(null, ""))

        val thrown = runCatching { repository.delete(itemId = 42L) }

        assertTrue(thrown.exceptionOrNull() is HttpException)
        assertEquals(sampleItems(), repository.items.first())
    }

    @Test
    fun `applyCreated appends an item that is not cached`() = runTest {
        val api = FakeItemsApi(itemsForFetch = emptyList())
        val repository = ItemsRepository(api)
        repository.refetch(listId = 1)
        val created = sampleItems().first()

        repository.applyCreated(created)

        assertEquals(listOf(created), repository.items.first())
    }

    @Test
    fun `applyCreated for an already-cached id leaves the cache unchanged`() = runTest {
        val api = FakeItemsApi(itemsForFetch = sampleItems())
        val repository = ItemsRepository(api)
        repository.refetch(listId = 1)

        repository.applyCreated(sampleItems().first())

        assertEquals(sampleItems(), repository.items.first())
    }

    @Test
    fun `applyUpdated replaces a cached entry whose payload differs`() = runTest {
        val api = FakeItemsApi(itemsForFetch = sampleItems())
        val repository = ItemsRepository(api)
        repository.refetch(listId = 1)
        val updated = sampleItems().first().copy(isChecked = true)

        repository.applyUpdated(updated)

        assertEquals(listOf(updated), repository.items.first())
    }

    @Test
    fun `applyUpdated for an absent id leaves the cache unchanged`() = runTest {
        val api = FakeItemsApi(itemsForFetch = sampleItems())
        val repository = ItemsRepository(api)
        repository.refetch(listId = 1)

        repository.applyUpdated(sampleItems().first().copy(id = 99, isChecked = true))

        assertEquals(sampleItems(), repository.items.first())
    }

    @Test
    fun `applyUpdated with an identical payload leaves the cache unchanged`() = runTest {
        val api = FakeItemsApi(itemsForFetch = sampleItems())
        val repository = ItemsRepository(api)
        repository.refetch(listId = 1)

        repository.applyUpdated(sampleItems().first())

        assertEquals(sampleItems(), repository.items.first())
    }

    @Test
    fun `applyDeleted removes a cached id and ignores an absent one`() = runTest {
        val api = FakeItemsApi(itemsForFetch = sampleItems())
        val repository = ItemsRepository(api)
        repository.refetch(listId = 1)

        repository.applyDeleted(itemId = 999L)
        assertEquals(sampleItems(), repository.items.first())

        repository.applyDeleted(itemId = 42L)
        assertTrue(repository.items.first().isEmpty())
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

private class FakeItemsApi(
    var itemsForFetch: List<Item>,
) : ItemsApi {
    var listCalls = 0
    var listError: HttpException? = null
    var createError: HttpException? = null
    var updateError: HttpException? = null
    var createdItem: Item? = null
    var updatedItem: Item? = null
    var deleted = false
    var deleteResponse: Response<Unit> = Response.success(Unit)
    var lastCreateListId: Long? = null
    var lastCreateRequest: CreateItemRequest? = null
    var lastUpdateItemId: Long? = null
    var lastUpdateRequest: UpdateItemRequest? = null
    var lastDeleteItemId: Long? = null

    override suspend fun listItems(listId: Long): List<Item> {
        listCalls++
        listError?.let { throw it }
        return itemsForFetch
    }

    override suspend fun createItem(listId: Long, body: CreateItemRequest): Item {
        lastCreateListId = listId
        lastCreateRequest = body
        createError?.let { throw it }
        return requireNotNull(createdItem)
    }

    override suspend fun updateItem(itemId: Long, body: UpdateItemRequest): Item {
        lastUpdateItemId = itemId
        lastUpdateRequest = body
        updateError?.let { throw it }
        return requireNotNull(updatedItem)
    }

    override suspend fun deleteItem(itemId: Long): Response<Unit> {
        deleted = true
        lastDeleteItemId = itemId
        return deleteResponse
    }
}

private fun httpException(code: Int): HttpException =
    HttpException(Response.error<Any>(code, okhttp3.ResponseBody.create(null, "")))

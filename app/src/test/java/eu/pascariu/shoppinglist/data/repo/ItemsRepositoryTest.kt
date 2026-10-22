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
        repository.create(listId = 1, CreateItemRequest(name = "Milk"))

        assertEquals(listOf(created), repository.items.first())
    }

    @Test
    fun `update replaces cached entry with server state`() = runTest {
        val api = FakeItemsApi(itemsForFetch = sampleItems())
        val repository = ItemsRepository(api)
        repository.refetch(listId = 1)
        val updated = sampleItems().first().copy(isChecked = true)

        api.updatedItem = updated
        repository.update(updated.id, UpdateItemRequest(isChecked = true))

        assertEquals(updated, repository.items.first().single())
    }

    @Test
    fun `delete removes cached entry`() = runTest {
        val api = FakeItemsApi(itemsForFetch = sampleItems())
        val repository = ItemsRepository(api)
        repository.refetch(listId = 1)

        repository.delete(itemId = 42L)

        assertTrue(repository.items.first().isEmpty())
        assertTrue(api.deleted)
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
    var createdItem: Item? = null
    var updatedItem: Item? = null
    var deleted = false

    override suspend fun listItems(listId: Long): List<Item> {
        listCalls++
        listError?.let { throw it }
        return itemsForFetch
    }

    override suspend fun createItem(listId: Long, body: CreateItemRequest): Item =
        requireNotNull(createdItem)

    override suspend fun updateItem(itemId: Long, body: UpdateItemRequest): Item =
        requireNotNull(updatedItem)

    override suspend fun deleteItem(itemId: Long): Response<Unit> {
        deleted = true
        return Response.success(Unit)
    }
}

private fun httpException(code: Int): HttpException =
    HttpException(Response.error<Any>(code, okhttp3.ResponseBody.create(null, "")))

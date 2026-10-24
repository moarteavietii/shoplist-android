package eu.pascariu.shoppinglist.data.repo

import eu.pascariu.shoppinglist.data.api.ItemsApi
import eu.pascariu.shoppinglist.data.config.FeatureConfig
import eu.pascariu.shoppinglist.data.connectivity.BackendConnectivity
import eu.pascariu.shoppinglist.data.local.FakeItemDao
import eu.pascariu.shoppinglist.data.local.FakePendingMutationDao
import eu.pascariu.shoppinglist.data.local.MutationQueue
import eu.pascariu.shoppinglist.data.model.CreateItemRequest
import eu.pascariu.shoppinglist.data.model.Item
import eu.pascariu.shoppinglist.data.model.UpdateItemRequest
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class ItemsRepositoryTest {

    private val testScope = CoroutineScope(Dispatchers.Unconfined)

    private fun offlineAwareQueue(): MutationQueue = MutationQueue(
        dao = FakePendingMutationDao(),
        connectivity = MutableStateFlow(BackendConnectivity.REACHABLE),
        json = Json { ignoreUnknownKeys = true },
    )

    @Test
    fun `first fetch populates cache and later reads are served from it`() = runTest {
        val api = FakeItemsApi(itemsForFetch = sampleItems())
        val repository = ItemsRepository(api, FakeItemDao(), offlineAwareQueue(), testScope)

        repository.refetch(listId = 1)

        assertEquals(sampleItems(), repository.items.first())
        assertEquals(1, api.listCalls)
    }

    @Test
    fun `refetch replaces cache wholesale`() = runTest {
        val api = FakeItemsApi(itemsForFetch = sampleItems())
        val repository = ItemsRepository(api, FakeItemDao(), offlineAwareQueue(), testScope)
        repository.refetch(listId = 1)

        api.itemsForFetch = listOf(sampleItems().first().copy(name = "Refreshed"))
        repository.refetch(listId = 1)

        assertEquals(listOf("Refreshed"), repository.items.first().map { it.name })
        assertEquals(2, api.listCalls)
    }

    @Test
    fun `404 on fetch surfaces error and leaves cache unchanged`() = runTest {
        val api = FakeItemsApi(itemsForFetch = sampleItems())
        val repository = ItemsRepository(api, FakeItemDao(), offlineAwareQueue(), testScope)
        repository.refetch(listId = 1)
        api.listError = httpException(404)

        val thrown = runCatching { repository.refetch(listId = 1) }

        assertTrue(thrown.exceptionOrNull() is HttpException)
        assertEquals(sampleItems(), repository.items.first())
    }

    @Test
    fun `create adds server-returned item to cache`() = runTest {
        val api = FakeItemsApi(itemsForFetch = emptyList())
        val repository = ItemsRepository(api, FakeItemDao(), offlineAwareQueue(), testScope)
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
    fun `create is idempotent when SSE event already added the item`() = runTest {
        val api = FakeItemsApi(itemsForFetch = emptyList())
        val repository = ItemsRepository(api, FakeItemDao(), offlineAwareQueue(), testScope)
        repository.refetch(listId = 1)
        val created = sampleItems().first()

        api.createdItem = created
        repository.applyCreated(created)
        repository.create(listId = 1, CreateItemRequest(name = created.name))

        assertEquals(1, repository.items.first().count { it.id == created.id })
    }

    @Test
    fun `update replaces cached entry with server state`() = runTest {
        val api = FakeItemsApi(itemsForFetch = sampleItems())
        val repository = ItemsRepository(api, FakeItemDao(), offlineAwareQueue(), testScope)
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
    fun `updateDetails applies name and visibility to cache before the patch settles`() = runTest {
        val api = FakeItemsApi(itemsForFetch = sampleItems())
        val repository = ItemsRepository(api, FakeItemDao(), offlineAwareQueue(), testScope)
        repository.refetch(listId = 1)
        var cacheDuringPatch: List<Item>? = null
        api.onUpdate = { cacheDuringPatch = repository.items.value }
        val updated = sampleItems().first().copy(name = "Crackers", visibleIn = listOf(1L, 2L))

        api.updatedItem = updated
        repository.updateDetails(itemId = 42L, UpdateItemRequest(name = "Crackers", visibleIn = listOf(1L, 2L)))

        assertEquals("Crackers", cacheDuringPatch?.single()?.name)
        assertEquals(listOf(1L, 2L), cacheDuringPatch?.single()?.visibleIn)
        assertEquals(updated, repository.items.first().single())
    }

    @Test
    fun `updateDetails failure restores the previous cached item`() = runTest {
        val api = FakeItemsApi(itemsForFetch = sampleItems())
        val repository = ItemsRepository(api, FakeItemDao(), offlineAwareQueue(), testScope)
        repository.refetch(listId = 1)
        api.updateError = httpException(500)

        val thrown = runCatching { repository.updateDetails(itemId = 42L, UpdateItemRequest(name = "Crackers")) }

        assertTrue(thrown.exceptionOrNull() is HttpException)
        assertEquals(sampleItems(), repository.items.first())
    }

    @Test
    fun `updateDetails failure does not clobber a newer remote state that arrived mid-flight`() = runTest {
        val api = FakeItemsApi(itemsForFetch = sampleItems())
        val repository = ItemsRepository(api, FakeItemDao(), offlineAwareQueue(), testScope)
        repository.refetch(listId = 1)
        api.onUpdate = {
            kotlinx.coroutines.runBlocking {
                repository.applyUpdated(sampleItems().first().copy(isChecked = true))
            }
        }
        api.updateError = httpException(500)

        runCatching { repository.updateDetails(itemId = 42L, UpdateItemRequest(name = "Crackers")) }

        val cached = repository.items.first().single()
        assertTrue(cached.isChecked)
        assertEquals("Milk", cached.name)
    }

    @Test
    fun `updateDetails of an item absent from cache fails without a patch`() = runTest {
        val api = FakeItemsApi(itemsForFetch = emptyList())
        val repository = ItemsRepository(api, FakeItemDao(), offlineAwareQueue(), testScope)
        repository.refetch(listId = 1)

        val thrown = runCatching { repository.updateDetails(itemId = 42L, UpdateItemRequest(name = "Crackers")) }

        assertTrue(thrown.exceptionOrNull() is IllegalArgumentException)
        assertEquals(null, api.lastUpdateItemId)
    }

    @Test
    fun `checking a temporary item stamps an optimistic expiry`() = runTest {
        val temporaryItem = sampleItems().first().copy(isTemporary = true, expiresAt = null)
        val api = FakeItemsApi(itemsForFetch = listOf(temporaryItem))
        val repository = ItemsRepository(api, FakeItemDao(), offlineAwareQueue(), testScope)
        repository.refetch(listId = 1)
        var cacheDuringPatch: Item? = null
        api.onUpdate = { cacheDuringPatch = repository.items.value.single() }
        val checked = temporaryItem.copy(isChecked = true, expiresAt = "2026-10-23T10:00:00Z")
        api.updatedItem = checked

        repository.setChecked(itemId = 42L, isChecked = true)

        assertTrue(cacheDuringPatch?.isChecked == true)
        val stamped = Instant.parse(requireNotNull(cacheDuringPatch?.expiresAt))
        val expectedFloor = Instant.now().plus(FeatureConfig.TEMPORARY_ITEM_TTL).minusSeconds(5)
        assertTrue(stamped.isAfter(expectedFloor))
        assertEquals(checked, repository.items.first().single())
    }

    @Test
    fun `unchecking a temporary item clears the cached expiry optimistically`() = runTest {
        val temporaryItem = sampleItems().first().copy(
            isTemporary = true,
            isChecked = true,
            expiresAt = "2026-10-23T10:00:00Z",
        )
        val api = FakeItemsApi(itemsForFetch = listOf(temporaryItem))
        val repository = ItemsRepository(api, FakeItemDao(), offlineAwareQueue(), testScope)
        repository.refetch(listId = 1)
        var cacheDuringPatch: Item? = null
        api.onUpdate = { cacheDuringPatch = repository.items.value.single() }
        val unchecked = temporaryItem.copy(isChecked = false, expiresAt = null)
        api.updatedItem = unchecked

        repository.setChecked(itemId = 42L, isChecked = false)

        assertEquals(false, cacheDuringPatch?.isChecked)
        assertEquals(null, cacheDuringPatch?.expiresAt)
        assertEquals(unchecked, repository.items.first().single())
    }

    @Test
    fun `failed check of a temporary item restores the previous expiry`() = runTest {
        val temporaryItem = sampleItems().first().copy(
            isTemporary = true,
            isChecked = true,
            expiresAt = "2026-10-23T10:00:00Z",
        )
        val api = FakeItemsApi(itemsForFetch = listOf(temporaryItem))
        val repository = ItemsRepository(api, FakeItemDao(), offlineAwareQueue(), testScope)
        repository.refetch(listId = 1)
        api.updateError = httpException(500)

        val thrown = runCatching { repository.setChecked(itemId = 42L, isChecked = false) }

        assertTrue(thrown.exceptionOrNull() is HttpException)
        assertEquals(temporaryItem, repository.items.first().single())
    }

    @Test
    fun `checking a permanent item leaves expiry untouched`() = runTest {
        val api = FakeItemsApi(itemsForFetch = sampleItems())
        val repository = ItemsRepository(api, FakeItemDao(), offlineAwareQueue(), testScope)
        repository.refetch(listId = 1)
        var cacheDuringPatch: Item? = null
        api.onUpdate = { cacheDuringPatch = repository.items.value.single() }
        val checked = sampleItems().first().copy(isChecked = true)
        api.updatedItem = checked

        repository.setChecked(itemId = 42L, isChecked = true)

        assertEquals(null, cacheDuringPatch?.expiresAt)
        assertEquals(checked, repository.items.first().single())
    }

    @Test
    fun `updateDetails temporary off clears the cached expiry optimistically`() = runTest {
        val temporaryItem = sampleItems().first().copy(
            isTemporary = true,
            isChecked = true,
            expiresAt = "2026-10-23T10:00:00Z",
        )
        val api = FakeItemsApi(itemsForFetch = listOf(temporaryItem))
        val repository = ItemsRepository(api, FakeItemDao(), offlineAwareQueue(), testScope)
        repository.refetch(listId = 1)
        var cacheDuringPatch: Item? = null
        api.onUpdate = { cacheDuringPatch = repository.items.value.single() }
        val permanent = temporaryItem.copy(isTemporary = false, expiresAt = null)
        api.updatedItem = permanent

        repository.updateDetails(itemId = 42L, UpdateItemRequest(isTemporary = false))

        assertEquals(false, cacheDuringPatch?.isTemporary)
        assertEquals(null, cacheDuringPatch?.expiresAt)
        assertEquals(permanent, repository.items.first().single())
    }

    @Test
    fun `updateDetails temporary on while checked stamps an optimistic expiry`() = runTest {
        val checkedItem = sampleItems().first().copy(isChecked = true)
        val api = FakeItemsApi(itemsForFetch = listOf(checkedItem))
        val repository = ItemsRepository(api, FakeItemDao(), offlineAwareQueue(), testScope)
        repository.refetch(listId = 1)
        var cacheDuringPatch: Item? = null
        api.onUpdate = { cacheDuringPatch = repository.items.value.single() }
        val temporary = checkedItem.copy(
            isTemporary = true,
            expiresAt = "2026-10-24T10:00:00Z",
        )
        api.updatedItem = temporary

        repository.updateDetails(itemId = 42L, UpdateItemRequest(isTemporary = true))

        assertTrue(cacheDuringPatch?.isTemporary == true)
        val stamped = Instant.parse(requireNotNull(cacheDuringPatch?.expiresAt))
        val expectedFloor = Instant.now().plus(FeatureConfig.TEMPORARY_ITEM_TTL).minusSeconds(5)
        assertTrue(stamped.isAfter(expectedFloor))
        assertEquals(temporary, repository.items.first().single())
    }

    @Test
    fun `updateDetails temporary on while unchecked does not stamp an expiry`() = runTest {
        val api = FakeItemsApi(itemsForFetch = sampleItems())
        val repository = ItemsRepository(api, FakeItemDao(), offlineAwareQueue(), testScope)
        repository.refetch(listId = 1)
        var cacheDuringPatch: Item? = null
        api.onUpdate = { cacheDuringPatch = repository.items.value.single() }
        val temporary = sampleItems().first().copy(isTemporary = true)
        api.updatedItem = temporary

        repository.updateDetails(itemId = 42L, UpdateItemRequest(isTemporary = true))

        assertEquals(null, cacheDuringPatch?.expiresAt)
        assertEquals(temporary, repository.items.first().single())
    }

    @Test
    fun `updateDetails folds quantity into the optimistic copy`() = runTest {
        val api = FakeItemsApi(itemsForFetch = sampleItems())
        val repository = ItemsRepository(api, FakeItemDao(), offlineAwareQueue(), testScope)
        repository.refetch(listId = 1)
        var cacheDuringPatch: Item? = null
        api.onUpdate = { cacheDuringPatch = repository.items.value.single() }
        val updated = sampleItems().first().copy(quantity = 3)
        api.updatedItem = updated

        repository.updateDetails(itemId = 42L, UpdateItemRequest(quantity = 3))

        assertEquals(3, cacheDuringPatch?.quantity)
        assertEquals(updated, repository.items.first().single())
    }

    @Test
    fun `updateDetails failure restores quantity and expiry`() = runTest {
        val temporaryItem = sampleItems().first().copy(
            isTemporary = true,
            isChecked = true,
            quantity = 2,
            expiresAt = "2026-10-23T10:00:00Z",
        )
        val api = FakeItemsApi(itemsForFetch = listOf(temporaryItem))
        val repository = ItemsRepository(api, FakeItemDao(), offlineAwareQueue(), testScope)
        repository.refetch(listId = 1)
        api.updateError = httpException(500)

        val thrown = runCatching {
            repository.updateDetails(
                itemId = 42L,
                UpdateItemRequest(name = "Crackers", quantity = 5, isTemporary = false),
            )
        }

        assertTrue(thrown.exceptionOrNull() is HttpException)
        assertEquals(temporaryItem, repository.items.first().single())
    }

    @Test
    fun `create for a different list leaves cache untouched`() = runTest {
        val api = FakeItemsApi(itemsForFetch = sampleItems())
        val repository = ItemsRepository(api, FakeItemDao(), offlineAwareQueue(), testScope)
        repository.refetch(listId = 1)

        api.createdItem = sampleItems().first().copy(id = 7, listId = 2)
        repository.create(listId = 2, CreateItemRequest(name = "Other"))

        assertEquals(sampleItems(), repository.items.first())
        assertEquals(1L, repository.cachedListIdFlow.first())
    }

    @Test
    fun `update of item from another list leaves cache untouched`() = runTest {
        val api = FakeItemsApi(itemsForFetch = sampleItems())
        val repository = ItemsRepository(api, FakeItemDao(), offlineAwareQueue(), testScope)
        repository.refetch(listId = 1)
        val otherListItem = sampleItems().first().copy(id = 7, listId = 2)

        api.updatedItem = otherListItem
        repository.update(itemId = 7, UpdateItemRequest(isChecked = true))

        assertEquals(sampleItems(), repository.items.first())
    }

    @Test
    fun `update of item absent from cache inserts it`() = runTest {
        val api = FakeItemsApi(itemsForFetch = emptyList())
        val repository = ItemsRepository(api, FakeItemDao(), offlineAwareQueue(), testScope)
        repository.refetch(listId = 1)
        val updated = sampleItems().first().copy(isChecked = true)

        api.updatedItem = updated
        repository.update(itemId = updated.id, UpdateItemRequest(isChecked = true))

        assertEquals(listOf(updated), repository.items.first())
    }

    @Test
    fun `refetching another list retargets the cache`() = runTest {
        val api = FakeItemsApi(itemsForFetch = sampleItems())
        val repository = ItemsRepository(api, FakeItemDao(), offlineAwareQueue(), testScope)
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
        val repository = ItemsRepository(api, FakeItemDao(), offlineAwareQueue(), testScope)
        repository.refetch(listId = 1)

        repository.delete(itemId = 42L)

        assertTrue(repository.items.first().isEmpty())
        assertTrue(api.deleted)
        assertEquals(42L, api.lastDeleteItemId)
    }

    @Test
    fun `409 on create surfaces error and leaves cache unchanged`() = runTest {
        val api = FakeItemsApi(itemsForFetch = sampleItems())
        val repository = ItemsRepository(api, FakeItemDao(), offlineAwareQueue(), testScope)
        repository.refetch(listId = 1)
        api.createError = httpException(409)

        val thrown = runCatching { repository.create(listId = 1, CreateItemRequest(name = "Milk")) }

        assertTrue(thrown.exceptionOrNull() is HttpException)
        assertEquals(sampleItems(), repository.items.first())
    }

    @Test
    fun `422 on update surfaces error and leaves cache unchanged`() = runTest {
        val api = FakeItemsApi(itemsForFetch = sampleItems())
        val repository = ItemsRepository(api, FakeItemDao(), offlineAwareQueue(), testScope)
        repository.refetch(listId = 1)
        api.updateError = httpException(422)

        val thrown = runCatching { repository.update(itemId = 42L, UpdateItemRequest(isChecked = true)) }

        assertTrue(thrown.exceptionOrNull() is HttpException)
        assertEquals(sampleItems(), repository.items.first())
    }

    @Test
    fun `404 on delete surfaces error and leaves cache unchanged`() = runTest {
        val api = FakeItemsApi(itemsForFetch = sampleItems())
        val repository = ItemsRepository(api, FakeItemDao(), offlineAwareQueue(), testScope)
        repository.refetch(listId = 1)
        api.deleteResponse = Response.error(404, okhttp3.ResponseBody.create(null, ""))

        val thrown = runCatching { repository.delete(itemId = 42L) }

        assertTrue(thrown.exceptionOrNull() is HttpException)
        assertEquals(sampleItems(), repository.items.first())
    }

    @Test
    fun `applyCreated appends an item that is not cached`() = runTest {
        val api = FakeItemsApi(itemsForFetch = emptyList())
        val repository = ItemsRepository(api, FakeItemDao(), offlineAwareQueue(), testScope)
        repository.refetch(listId = 1)
        val created = sampleItems().first()

        repository.applyCreated(created)

        assertEquals(listOf(created), repository.items.first())
    }

    @Test
    fun `applyCreated for an already-cached id leaves the cache unchanged`() = runTest {
        val api = FakeItemsApi(itemsForFetch = sampleItems())
        val repository = ItemsRepository(api, FakeItemDao(), offlineAwareQueue(), testScope)
        repository.refetch(listId = 1)

        repository.applyCreated(sampleItems().first())

        assertEquals(sampleItems(), repository.items.first())
    }

    @Test
    fun `applyUpdated replaces a cached entry whose payload differs`() = runTest {
        val api = FakeItemsApi(itemsForFetch = sampleItems())
        val repository = ItemsRepository(api, FakeItemDao(), offlineAwareQueue(), testScope)
        repository.refetch(listId = 1)
        val updated = sampleItems().first().copy(isChecked = true)

        repository.applyUpdated(updated)

        assertEquals(listOf(updated), repository.items.first())
    }

    @Test
    fun `applyUpdated for an absent id leaves the cache unchanged`() = runTest {
        val api = FakeItemsApi(itemsForFetch = sampleItems())
        val repository = ItemsRepository(api, FakeItemDao(), offlineAwareQueue(), testScope)
        repository.refetch(listId = 1)

        repository.applyUpdated(sampleItems().first().copy(id = 99, isChecked = true))

        assertEquals(sampleItems(), repository.items.first())
    }

    @Test
    fun `applyUpdated with an identical payload leaves the cache unchanged`() = runTest {
        val api = FakeItemsApi(itemsForFetch = sampleItems())
        val repository = ItemsRepository(api, FakeItemDao(), offlineAwareQueue(), testScope)
        repository.refetch(listId = 1)

        repository.applyUpdated(sampleItems().first())

        assertEquals(sampleItems(), repository.items.first())
    }

    @Test
    fun `applyDeleted removes a cached id and ignores an absent one`() = runTest {
        val api = FakeItemsApi(itemsForFetch = sampleItems())
        val repository = ItemsRepository(api, FakeItemDao(), offlineAwareQueue(), testScope)
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
    var onUpdate: (() -> Unit)? = null

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
        onUpdate?.invoke()
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

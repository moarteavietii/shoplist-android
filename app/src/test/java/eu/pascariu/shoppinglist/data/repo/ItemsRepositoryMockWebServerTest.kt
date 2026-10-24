package eu.pascariu.shoppinglist.data.repo

import eu.pascariu.shoppinglist.data.api.ItemsApi
import eu.pascariu.shoppinglist.data.connectivity.BackendConnectivity
import eu.pascariu.shoppinglist.data.local.FakeItemDao
import eu.pascariu.shoppinglist.data.local.FakePendingMutationDao
import eu.pascariu.shoppinglist.data.local.MutationQueue
import eu.pascariu.shoppinglist.data.model.CreateItemRequest
import eu.pascariu.shoppinglist.data.model.Item
import eu.pascariu.shoppinglist.data.model.UpdateItemRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class ItemsRepositoryMockWebServerTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: ItemsRepository

    private fun offlineAwareQueue(): MutationQueue = MutationQueue(
        dao = FakePendingMutationDao(),
        connectivity = MutableStateFlow(BackendConnectivity.REACHABLE),
        json = Json { ignoreUnknownKeys = true },
    )

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
        val api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ItemsApi::class.java)
        repository = ItemsRepository(api, FakeItemDao(), offlineAwareQueue(), CoroutineScope(Dispatchers.Unconfined))
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun enqueueItemJson(
        id: Long = 10,
        listId: Long = 1,
        name: String = "Milk",
        isChecked: Boolean = false,
        checkCount: Int = 0,
        orderMapJson: String = """{"a0": "a1"}""",
    ): String =
        """
        {
          "id": $id,
          "listId": $listId,
          "name": "$name",
          "isChecked": $isChecked,
          "orderMap": $orderMapJson,
          "quantity": 2,
          "isTemporary": false,
          "checkCount": $checkCount,
          "expiresAt": null,
          "createdAt": "2026-10-22T09:00:00Z",
          "visibleIn": [1]
        }
        """.trimIndent()

    private fun enqueueItemListJson(vararg itemJsons: String): MockResponse =
        MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody("[${itemJsons.joinToString(",")}]")

    private fun enqueueItemResponse(itemJson: String, code: Int = 200): MockResponse =
        MockResponse()
            .setResponseCode(code)
            .setHeader("Content-Type", "application/json")
            .setBody(itemJson)

    @Test
    fun `listItems deserializes real JSON and populates cache`() = runTest {
        server.enqueue(enqueueItemListJson(enqueueItemJson(id = 10), enqueueItemJson(id = 11, name = "Bread")))

        repository.refetch(listId = 1)

        val recorded = server.takeRequest()
        assertEquals("GET", recorded.method)
        assertEquals("/api/v1/lists/1/items", recorded.path)
        assertEquals(listOf(10L, 11L), repository.items.first().map { it.id })
        assertEquals(listOf("Milk", "Bread"), repository.items.first().map { it.name })
        assertEquals(listOf(false, false), repository.items.first().map { it.isTemporary })
    }

    @Test
    fun `create posts JSON body to list-scoped path and caches server item`() = runTest {
        server.enqueue(enqueueItemListJson())
        repository.refetch(listId = 1)
        server.takeRequest()
        server.enqueue(enqueueItemResponse(enqueueItemJson(id = 12, name = "Eggs")))

        val request = CreateItemRequest(name = "Eggs", quantity = 2)
        val created = repository.create(listId = 1, request)

        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/api/v1/lists/1/items", recorded.path)
        assertTrue(recorded.body.readUtf8().contains("\"name\":\"Eggs\""))
        assertTrue(recorded.getHeader("Content-Type")!!.startsWith("application/json"))
        assertEquals(12L, created.id)
        assertEquals(listOf(12L), repository.items.first().map { it.id })
    }

    @Test
    fun `update patches JSON body to item path and replaces cached entry`() = runTest {
        server.enqueue(enqueueItemListJson(enqueueItemJson(id = 10)))
        repository.refetch(listId = 1)
        server.takeRequest()
        server.enqueue(enqueueItemResponse(enqueueItemJson(id = 10, isChecked = true)))

        val request = UpdateItemRequest(isChecked = true)
        val updated = repository.update(itemId = 10, request)

        val recorded = server.takeRequest()
        assertEquals("PATCH", recorded.method)
        assertEquals("/api/v1/items/10", recorded.path)
        assertTrue(recorded.body.readUtf8().contains("\"isChecked\":true"))
        assertTrue(updated.isChecked)
        assertTrue(repository.items.first().single().isChecked)
    }

    @Test
    fun `delete sends DELETE to item path and removes from cache`() = runTest {
        server.enqueue(enqueueItemListJson(enqueueItemJson(id = 10)))
        repository.refetch(listId = 1)
        server.takeRequest()
        server.enqueue(MockResponse().setResponseCode(204))

        repository.delete(itemId = 10)

        val recorded = server.takeRequest()
        assertEquals("DELETE", recorded.method)
        assertEquals("/api/v1/items/10", recorded.path)
        assertTrue(repository.items.first().isEmpty())
    }

    @Test
    fun `500 on create surfaces HttpException and leaves cache unchanged`() = runTest {
        server.enqueue(enqueueItemListJson(enqueueItemJson(id = 10)))
        repository.refetch(listId = 1)
        server.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"error\":\"boom\"}")
        )

        val thrown = runCatching { repository.create(listId = 1, CreateItemRequest(name = "Milk")) }

        assertTrue(thrown.exceptionOrNull() is HttpException)
        assertEquals(500, (thrown.exceptionOrNull() as HttpException).code())
        assertEquals(listOf(10L), repository.items.first().map { it.id })
    }

    @Test
    fun `404 on delete surfaces HttpException and leaves cache unchanged`() = runTest {
        server.enqueue(enqueueItemListJson(enqueueItemJson(id = 10)))
        repository.refetch(listId = 1)
        server.enqueue(MockResponse().setResponseCode(404))

        val thrown = runCatching { repository.delete(itemId = 10) }

        assertTrue(thrown.exceptionOrNull() is HttpException)
        assertEquals(404, (thrown.exceptionOrNull() as HttpException).code())
        assertEquals(listOf(10L), repository.items.first().map { it.id })
    }

    @Test
    fun `404 on fetch surfaces HttpException and leaves cache unchanged`() = runTest {
        server.enqueue(enqueueItemListJson(enqueueItemJson(id = 10)))
        repository.refetch(listId = 1)
        server.enqueue(MockResponse().setResponseCode(404))

        val thrown = runCatching { repository.refetch(listId = 1) }

        assertTrue(thrown.exceptionOrNull() is HttpException)
        assertEquals(listOf(10L), repository.items.first().map { it.id })
    }

    @Test
    fun `setChecked optimistically flips cache before PATCH settles`() = runTest {
        server.enqueue(enqueueItemListJson(enqueueItemJson(id = 10, isChecked = false)))
        repository.refetch(listId = 1)
        server.takeRequest()
        server.enqueue(enqueueItemResponse(enqueueItemJson(id = 10, isChecked = true)))

        val deferred = async { repository.setChecked(itemId = 10, isChecked = true) }

        runCurrent()
        assertTrue(repository.items.first().single().isChecked)

        deferred.join()
        assertEquals(10L, deferred.await().id)
    }

    @Test
    fun `setChecked sends PATCH with isChecked and swaps in server item`() = runTest {
        server.enqueue(enqueueItemListJson(enqueueItemJson(id = 10, isChecked = false)))
        repository.refetch(listId = 1)
        server.takeRequest()
        server.enqueue(enqueueItemResponse(enqueueItemJson(id = 10, isChecked = true, checkCount = 1)))

        val updated = repository.setChecked(itemId = 10, isChecked = true)

        val recorded = server.takeRequest()
        assertEquals("PATCH", recorded.method)
        assertEquals("/api/v1/items/10", recorded.path)
        val body = recorded.body.readUtf8()
        assertTrue(body.contains("\"isChecked\":true"))
        assertTrue(!body.contains("isUndo"))
        assertTrue(updated.isChecked)
        assertEquals(1, repository.items.first().single().checkCount)
    }

    @Test
    fun `setChecked with isUndo sends undo flag in PATCH body`() = runTest {
        server.enqueue(enqueueItemListJson(enqueueItemJson(id = 10, isChecked = true)))
        repository.refetch(listId = 1)
        server.takeRequest()
        server.enqueue(enqueueItemResponse(enqueueItemJson(id = 10, isChecked = false)))

        repository.setChecked(itemId = 10, isChecked = false, isUndo = true)

        val recorded = server.takeRequest()
        assertTrue(recorded.body.readUtf8().contains("\"isUndo\":true"))
    }

    @Test
    fun `manual uncheck does not send undo flag`() = runTest {
        server.enqueue(enqueueItemListJson(enqueueItemJson(id = 10, isChecked = true)))
        repository.refetch(listId = 1)
        server.takeRequest()
        server.enqueue(enqueueItemResponse(enqueueItemJson(id = 10, isChecked = false)))

        repository.setChecked(itemId = 10, isChecked = false)

        val recorded = server.takeRequest()
        assertTrue(!recorded.body.readUtf8().contains("isUndo"))
    }

    @Test
    fun `failed setChecked rolls cache back and rethrows`() = runTest {
        server.enqueue(enqueueItemListJson(enqueueItemJson(id = 10, isChecked = false)))
        repository.refetch(listId = 1)
        server.takeRequest()
        server.enqueue(MockResponse().setResponseCode(500).setHeader("Content-Type", "application/json").setBody("{\"error\":\"boom\"}"))

        val thrown = runCatching { repository.setChecked(itemId = 10, isChecked = true) }

        assertTrue(thrown.exceptionOrNull() is HttpException)
        assertEquals(false, repository.items.first().single().isChecked)
    }

    @Test
    fun `failed setChecked rollback only reverts isChecked keeping remote fields`() = runTest {
        server.enqueue(enqueueItemListJson(enqueueItemJson(id = 10, isChecked = false)))
        repository.refetch(listId = 1)
        server.takeRequest()
        server.enqueue(MockResponse().setResponseCode(500).setHeader("Content-Type", "application/json").setBody("{\"error\":\"boom\"}"))

        val deferred = async { runCatching { repository.setChecked(itemId = 10, isChecked = true) } }
        runCurrent()

        repository.applyUpdated(
            Json.decodeFromString(enqueueItemJson(id = 10, name = "Organic Milk", isChecked = true)),
        )

        deferred.join()
        assertTrue(deferred.await().exceptionOrNull() is HttpException)

        val rolledBack = repository.items.first().single()
        assertEquals(false, rolledBack.isChecked)
        assertEquals("Organic Milk", rolledBack.name)
    }

    @Test
    fun `reorder optimistically merges orderMap before PATCH settles`() = runTest {
        val patchSettled = CountDownLatch(1)
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse =
                if (request.method == "PATCH") {
                    patchSettled.await(5, TimeUnit.SECONDS)
                    enqueueItemResponse(enqueueItemJson(id = 10))
                } else {
                    enqueueItemListJson(enqueueItemJson(id = 10))
                }
        }

        repository.refetch(listId = 1)
        server.takeRequest()

        val deferred = async { repository.reorder(itemId = 10, viewId = 1, newKey = "a5") }

        runCurrent()
        assertEquals("a5", repository.items.first().single().orderMap["1"])

        patchSettled.countDown()
        deferred.join()
        assertEquals(10L, deferred.await().id)
    }

    @Test
    fun `reorder sends PATCH with merged orderMap`() = runTest {
        server.enqueue(enqueueItemListJson(enqueueItemJson(id = 10)))
        repository.refetch(listId = 1)
        server.takeRequest()
        server.enqueue(enqueueItemResponse(enqueueItemJson(id = 10)))

        repository.reorder(itemId = 10, viewId = 1, newKey = "a5")

        val recorded = server.takeRequest()
        assertEquals("PATCH", recorded.method)
        assertEquals("/api/v1/items/10", recorded.path)
        val body = recorded.body.readUtf8()
        assertTrue(body.contains("\"orderMap\""))
        assertTrue(body.contains("\"1\":\"a5\""))
    }

    @Test
    fun `failed reorder rolls cache back and rethrows`() = runTest {
        server.enqueue(enqueueItemListJson(enqueueItemJson(id = 10)))
        repository.refetch(listId = 1)
        server.takeRequest()
        server.enqueue(MockResponse().setResponseCode(500).setHeader("Content-Type", "application/json").setBody("{\"error\":\"boom\"}"))

        val thrown = runCatching { repository.reorder(itemId = 10, viewId = 1, newKey = "a5") }

        assertTrue(thrown.exceptionOrNull() is HttpException)
        val rolledBack = repository.items.first().single()
        assertNull(rolledBack.orderMap["1"])
        assertEquals("a1", rolledBack.orderMap["a0"])
    }

    @Test
    fun `failed reorder rollback only reverts the reordered view key keeping remote fields`() = runTest {
        server.enqueue(enqueueItemListJson(enqueueItemJson(id = 10)))
        repository.refetch(listId = 1)
        server.takeRequest()
        server.enqueue(MockResponse().setResponseCode(500).setHeader("Content-Type", "application/json").setBody("{\"error\":\"boom\"}"))

        val deferred = async { runCatching { repository.reorder(itemId = 10, viewId = 1, newKey = "a5") } }
        runCurrent()

        repository.applyUpdated(
            Json.decodeFromString<Item>(enqueueItemJson(id = 10, name = "Organic Milk")),
        )

        deferred.join()
        assertTrue(deferred.await().exceptionOrNull() is HttpException)

        val rolledBack = repository.items.first().single()
        assertEquals("Organic Milk", rolledBack.name)
        assertNull(rolledBack.orderMap["1"])
        assertEquals("a1", rolledBack.orderMap["a0"])
    }

    @Test
    fun `failed reorder rollback keeps concurrent orderMap change for reordered view`() = runTest {
        server.enqueue(enqueueItemListJson(enqueueItemJson(id = 10)))
        repository.refetch(listId = 1)
        server.takeRequest()
        server.enqueue(MockResponse().setResponseCode(500).setHeader("Content-Type", "application/json").setBody("{\"error\":\"boom\"}"))

        val deferred = async { runCatching { repository.reorder(itemId = 10, viewId = 1, newKey = "a5") } }
        runCurrent()

        repository.applyUpdated(
            Json.decodeFromString<Item>(enqueueItemJson(id = 10, orderMapJson = """{"a0": "a1", "1": "b9"}""")),
        )

        deferred.join()
        assertTrue(deferred.await().exceptionOrNull() is HttpException)

        val rolledBack = repository.items.first().single()
        assertEquals("b9", rolledBack.orderMap["1"])
        assertEquals("a1", rolledBack.orderMap["a0"])
    }

    @Test
    fun `failed reorder rollback keeps concurrent orderMap change from another view`() = runTest {
        server.enqueue(enqueueItemListJson(enqueueItemJson(id = 10)))
        repository.refetch(listId = 1)
        server.takeRequest()
        server.enqueue(MockResponse().setResponseCode(500).setHeader("Content-Type", "application/json").setBody("{\"error\":\"boom\"}"))

        val deferred = async { runCatching { repository.reorder(itemId = 10, viewId = 1, newKey = "a5") } }
        runCurrent()

        repository.applyUpdated(
            Json.decodeFromString<Item>(enqueueItemJson(id = 10, orderMapJson = """{"a0": "a1", "2": "b0"}""")),
        )

        deferred.join()
        assertTrue(deferred.await().exceptionOrNull() is HttpException)

        val rolledBack = repository.items.first().single()
        assertNull(rolledBack.orderMap["1"])
        assertEquals("b0", rolledBack.orderMap["2"])
        assertEquals("a1", rolledBack.orderMap["a0"])
    }
}

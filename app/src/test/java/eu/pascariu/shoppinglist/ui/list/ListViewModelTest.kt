package eu.pascariu.shoppinglist.ui.list

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import eu.pascariu.shoppinglist.data.api.ItemsApi
import eu.pascariu.shoppinglist.data.api.ShopsApi
import eu.pascariu.shoppinglist.data.model.Item
import eu.pascariu.shoppinglist.data.repo.ItemsRepository
import eu.pascariu.shoppinglist.data.repo.ShopsRepository
import eu.pascariu.shoppinglist.data.undo.UndoHistoryStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
class ListViewModelTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var server: MockWebServer
    private lateinit var dataStoreFile: java.io.File
    private lateinit var storeScope: CoroutineScope
    private lateinit var itemsRepository: ItemsRepository
    private lateinit var shopsRepository: ShopsRepository
    private lateinit var undoHistoryStore: UndoHistoryStore
    private lateinit var viewModel: ListViewModel

    private val json = Json

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        server = MockWebServer()
        server.start()
        dataStoreFile = java.io.File(tmpFolder.root, "undo_history_test.preferences_pb")
        storeScope = CoroutineScope(testDispatcher + Job())
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        itemsRepository = ItemsRepository(retrofit.create(ItemsApi::class.java))
        shopsRepository = ShopsRepository(retrofit.create(ShopsApi::class.java))
        undoHistoryStore = UndoHistoryStore(
            dataStore = PreferenceDataStoreFactory.create(scope = storeScope) {
                dataStoreFile
            },
            json = json,
            storeScope = storeScope,
            ioDispatcher = Dispatchers.IO,
            pruneInterval = java.time.Duration.ofHours(1),
            clock = System::currentTimeMillis,
        )
        viewModel = ListViewModel(itemsRepository, shopsRepository, undoHistoryStore)
    }

    @After
    fun tearDown() {
        storeScope.cancel()
        server.shutdown()
        Dispatchers.resetMain()
    }

    private fun enqueueItemList(vararg itemJsons: String) {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("[${itemJsons.joinToString(",")}]"),
        )
    }

    private fun enqueueShopList(vararg shopJsons: String) {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("[${shopJsons.joinToString(",")}]"),
        )
    }

    private fun itemJson(
        id: Long,
        name: String,
        isChecked: Boolean = false,
        orderKey: String = "a0",
        visibleIn: String = "[1]",
    ): String =
        """
        {
          "id": $id,
          "listId": 1,
          "name": "$name",
          "isChecked": $isChecked,
          "orderMap": {"1": "$orderKey"},
          "quantity": 1,
          "isTemporary": false,
          "checkCount": 0,
          "expiresAt": null,
          "createdAt": "2026-10-22T09:00:00Z",
          "visibleIn": $visibleIn
        }
        """.trimIndent()

    private fun shopJson(id: Long, name: String): String =
        """
        {"id": $id, "name": "$name", "createdAt": "2026-10-22T09:00:00Z"}
        """.trimIndent()

    private fun TestScope.loadFixtures() {
        enqueueItemList(itemJson(10, "Milk", orderKey = "a0"), itemJson(11, "Bread", orderKey = "a1"))
        enqueueShopList(shopJson(1, "Global"), shopJson(2, "Auchan"))
        viewModel.refresh()
        awaitUntil { shopsRepository.shops.value.isNotEmpty() && itemsRepository.items.value.isNotEmpty() }
        server.takeRequest(5, TimeUnit.SECONDS)
        server.takeRequest(5, TimeUnit.SECONDS)
    }

    // Deliberate hybrid: real-time deadline + virtual-time advanceUntilIdle() so MockWebServer
    // responses land while virtual time still burns through ViewModel delays deterministically.
    private fun TestScope.awaitUntil(timeoutMs: Long = 5_000, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (!condition()) {
            if (System.currentTimeMillis() >= deadline) {
                throw AssertionError(
                    "condition not met within ${timeoutMs}ms " +
                        "(requests=${server.requestCount}, shops=${shopsRepository.shops.value.size}, items=${itemsRepository.items.value.size})",
                )
            }
            advanceUntilIdle()
            Thread.sleep(10)
        }
    }

    @Test
    fun `refresh loads items and shops and builds ui state`() = runTest(testDispatcher) {
        loadFixtures()

        val state = viewModel.uiState.value
        assertEquals(listOf("Milk", "Bread"), state.items.map { it.name })
        assertEquals("Global", state.watermarkName)
        assertFalse(state.showUndoBar)
    }

    @Test
    fun `search query filters items with fuzzy match`() = runTest(testDispatcher) {
        loadFixtures()

        viewModel.onSearchQueryChanged("mlk")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf("Milk"), state.filteredItems.map { it.name })
        assertFalse(state.showAddAffordance)
    }

    @Test
    fun `non-matching query shows add affordance`() = runTest(testDispatcher) {
        loadFixtures()

        viewModel.onSearchQueryChanged("xyz")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.filteredItems.isEmpty())
        assertTrue(state.showAddAffordance)
    }

    @Test
    fun `successful check records undo entry and shows undo bar`() = runTest(testDispatcher) {
        loadFixtures()
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(itemJson(id = 10, name = "Milk", isChecked = true)),
        )

        viewModel.onCheckToggled(viewModel.uiState.value.items.first { it.id == 10L })
        awaitUntil { viewModel.uiState.value.showUndoBar }

        assertEquals(1, undoHistoryStore.entries.value.size)
        assertTrue(viewModel.uiState.value.items.first { it.id == 10L }.isChecked)
    }

    @Test
    fun `failed check does not record history`() = runTest(testDispatcher) {
        loadFixtures()
        server.enqueue(MockResponse().setResponseCode(500).setHeader("Content-Type", "application/json").setBody("{\"error\":\"boom\"}"))

        viewModel.onCheckToggled(viewModel.uiState.value.items.first { it.id == 10L })
        awaitUntil { server.requestCount == 3 }
        awaitUntil { !viewModel.uiState.value.items.first { it.id == 10L }.isChecked }

        assertFalse(viewModel.uiState.value.showUndoBar)
        assertTrue(undoHistoryStore.entries.value.isEmpty())
        assertFalse(viewModel.uiState.value.items.first { it.id == 10L }.isChecked)
    }

    @Test
    fun `undo reverts most recent check with isUndo flag`() = runTest(testDispatcher) {
        loadFixtures()
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(itemJson(id = 10, name = "Milk", isChecked = true)),
        )
        viewModel.onCheckToggled(viewModel.uiState.value.items.first { it.id == 10L })
        awaitUntil { viewModel.uiState.value.showUndoBar }

        server.enqueue(
            MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(itemJson(id = 10, name = "Milk", isChecked = false)),
        )
        viewModel.onUndoTapped()
        awaitUntil { !viewModel.uiState.value.items.first { it.id == 10L }.isChecked }

        val checkRequest = server.takeRequest(5, TimeUnit.SECONDS)
        assertNotNull(checkRequest)
        assertEquals("PATCH", checkRequest!!.method)
        assertEquals("/api/items/10", checkRequest.path)
        val checkBody = checkRequest.body.readUtf8()
        assertTrue(checkBody.contains("\"isChecked\":true"))
        assertFalse(checkBody.contains("isUndo"))
        val undoRequest = server.takeRequest(5, TimeUnit.SECONDS)
        assertNotNull(undoRequest)
        assertEquals("PATCH", undoRequest!!.method)
        assertEquals("/api/items/10", undoRequest.path)
        assertTrue(undoRequest.body.readUtf8().contains("\"isUndo\":true"))
        awaitUntil { !viewModel.uiState.value.showUndoBar }
    }

    @Test
    fun `undo proceeds immediately when no checks are in flight`() = runTest(testDispatcher) {
        loadFixtures()
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(itemJson(id = 10, name = "Milk", isChecked = true)),
        )
        viewModel.onCheckToggled(viewModel.uiState.value.items.first { it.id == 10L })
        awaitUntil { viewModel.uiState.value.showUndoBar }

        server.enqueue(
            MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(itemJson(id = 10, name = "Milk", isChecked = false)),
        )
        viewModel.onUndoTapped()
        runCurrent()

        val undoRequest = server.takeRequest(5, TimeUnit.SECONDS)
        assertNotNull("undo PATCH should fire without waiting out the inflight window", undoRequest)
        assertEquals("PATCH", undoRequest!!.method)
    }

    @Test
    fun `undo skips stale entry whose to no longer matches`() = runTest(testDispatcher) {
        loadFixtures()
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(itemJson(id = 10, name = "Milk", isChecked = true)),
        )
        viewModel.onCheckToggled(viewModel.uiState.value.items.first { it.id == 10L })
        awaitUntil { viewModel.uiState.value.showUndoBar }

        server.enqueue(
            MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(itemJson(id = 10, name = "Milk", isChecked = false)),
        )
        itemsRepository.setChecked(itemId = 10L, isChecked = false)
        awaitUntil { !viewModel.uiState.value.items.first { it.id == 10L }.isChecked }

        viewModel.onUndoTapped()
        awaitUntil { !viewModel.uiState.value.showUndoBar }

        assertEquals(4, server.requestCount)
    }

    @Test
    fun `undo of item missing from cache drops entry without PATCH`() = runTest(testDispatcher) {
        loadFixtures()
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(itemJson(id = 10, name = "Milk", isChecked = true)),
        )
        viewModel.onCheckToggled(viewModel.uiState.value.items.first { it.id == 10L })
        awaitUntil { viewModel.uiState.value.showUndoBar }

        itemsRepository.applyDeleted(10L)
        awaitUntil { itemsRepository.items.value.none { it.id == 10L } }

        viewModel.onUndoTapped()
        awaitUntil { !viewModel.uiState.value.showUndoBar }

        assertEquals(3, server.requestCount)
    }

    @Test
    fun `failed undo restores entry`() = runTest(testDispatcher) {
        loadFixtures()
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(itemJson(id = 10, name = "Milk", isChecked = true)),
        )
        viewModel.onCheckToggled(viewModel.uiState.value.items.first { it.id == 10L })
        awaitUntil { viewModel.uiState.value.showUndoBar }

        server.enqueue(MockResponse().setResponseCode(500).setHeader("Content-Type", "application/json").setBody("{\"error\":\"boom\"}"))
        viewModel.onUndoTapped()
        awaitUntil { server.requestCount == 4 }
        awaitUntil { viewModel.uiState.value.items.first { it.id == 10L }.isChecked }

        assertTrue(viewModel.uiState.value.showUndoBar)
        assertEquals(1, undoHistoryStore.entries.value.size)
    }

    @Test
    fun `add affordance opens modal with query prefilled and all views selected`() = runTest(testDispatcher) {
        loadFixtures()

        viewModel.onAddAffordanceTapped()
        awaitUntil { viewModel.uiState.value.addItemModal != null }

        val modal = viewModel.uiState.value.addItemModal
        assertNotNull(modal)
        assertEquals("", modal?.title)
        assertEquals(listOf("Global", "Auchan"), modal?.availableViews?.map { it.name })
        assertEquals(setOf(1L, 2L), modal?.selectedViewIds)
    }

    @Test
    fun `toggle all deselects everything except Global when all selected`() = runTest(testDispatcher) {
        loadFixtures()
        viewModel.onAddAffordanceTapped()
        awaitUntil { viewModel.uiState.value.addItemModal != null }

        viewModel.onAddItemToggleAllTapped()
        awaitUntil { viewModel.uiState.value.addItemModal?.selectedViewIds == setOf(1L) }

        assertEquals(setOf(1L), viewModel.uiState.value.addItemModal?.selectedViewIds)
    }

    @Test
    fun `toggle all selects everything when not all selected`() = runTest(testDispatcher) {
        loadFixtures()
        viewModel.onAddAffordanceTapped()
        awaitUntil { viewModel.uiState.value.addItemModal != null }
        viewModel.onAddItemViewToggled(2L)
        awaitUntil { viewModel.uiState.value.addItemModal?.selectedViewIds == setOf(1L) }

        viewModel.onAddItemToggleAllTapped()
        awaitUntil { viewModel.uiState.value.addItemModal?.selectedViewIds == setOf(1L, 2L) }

        assertEquals(setOf(1L, 2L), viewModel.uiState.value.addItemModal?.selectedViewIds)
    }

    @Test
    fun `save creates item with selected views and clears query`() = runTest(testDispatcher) {
        loadFixtures()
        viewModel.onSearchQueryChanged("Eggs")
        advanceUntilIdle()
        viewModel.onAddAffordanceTapped()
        advanceUntilIdle()
        server.enqueue(
            MockResponse().setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody(itemJson(id = 12, name = "Eggs")),
        )

        viewModel.onAddItemSave()
        awaitUntil { server.requestCount == 3 }

        val recorded = server.takeRequest(5, TimeUnit.SECONDS)
        assertNotNull(recorded)
        assertEquals("POST", recorded!!.method)
        assertEquals("/api/lists/1/items", recorded.path)
        val body = recorded.body.readUtf8()
        assertTrue(body.contains("\"name\":\"Eggs\""))
        assertTrue(body.contains("\"visibleIn\":[1,2]"))
        awaitUntil { viewModel.uiState.value.addItemModal == null }
        assertEquals("", viewModel.uiState.value.searchQuery)
    }

    @Test
    fun `double tap on save creates only one item`() = runTest(testDispatcher) {
        loadFixtures()
        viewModel.onSearchQueryChanged("Eggs")
        advanceUntilIdle()
        viewModel.onAddAffordanceTapped()
        advanceUntilIdle()
        server.enqueue(
            MockResponse().setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody(itemJson(id = 12, name = "Eggs")),
        )

        viewModel.onAddItemSave()
        viewModel.onAddItemSave()
        awaitUntil { viewModel.uiState.value.addItemModal == null }
        advanceUntilIdle()

        assertEquals(3, server.requestCount)
    }

    @Test
    fun `failed save surfaces error and keeps modal open`() = runTest(testDispatcher) {
        loadFixtures()
        viewModel.onSearchQueryChanged("Eggs")
        advanceUntilIdle()
        viewModel.onAddAffordanceTapped()
        advanceUntilIdle()
        server.enqueue(MockResponse().setResponseCode(500))

        viewModel.onAddItemSave()
        awaitUntil { server.requestCount == 3 }
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.errorMessage)
        assertNotNull(viewModel.uiState.value.addItemModal)
    }

    @Test
    fun `successful retry clears save error`() = runTest(testDispatcher) {
        loadFixtures()
        viewModel.onSearchQueryChanged("Eggs")
        advanceUntilIdle()
        viewModel.onAddAffordanceTapped()
        advanceUntilIdle()
        server.enqueue(MockResponse().setResponseCode(500))
        server.enqueue(
            MockResponse().setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody(itemJson(id = 12, name = "Eggs")),
        )

        viewModel.onAddItemSave()
        awaitUntil { server.requestCount == 3 }
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.errorMessage)

        viewModel.onAddItemSave()
        awaitUntil { viewModel.uiState.value.addItemModal == null }

        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `error can be dismissed`() = runTest(testDispatcher) {
        loadFixtures()
        viewModel.onSearchQueryChanged("Eggs")
        advanceUntilIdle()
        viewModel.onAddAffordanceTapped()
        advanceUntilIdle()
        server.enqueue(MockResponse().setResponseCode(500))

        viewModel.onAddItemSave()
        awaitUntil { server.requestCount == 3 }
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.errorMessage)

        viewModel.onErrorDismissed()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `failed refresh surfaces error`() = runTest(testDispatcher) {
        server.enqueue(MockResponse().setResponseCode(500))
        server.enqueue(MockResponse().setResponseCode(500))

        viewModel.refresh()
        awaitUntil { viewModel.uiState.value.errorMessage != null }

        assertNotNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `save can be retried after a failed create`() = runTest(testDispatcher) {
        loadFixtures()
        viewModel.onSearchQueryChanged("Eggs")
        advanceUntilIdle()
        viewModel.onAddAffordanceTapped()
        advanceUntilIdle()
        server.enqueue(MockResponse().setResponseCode(500))
        server.enqueue(
            MockResponse().setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody(itemJson(id = 12, name = "Eggs")),
        )

        viewModel.onAddItemSave()
        awaitUntil { server.requestCount == 3 }
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.addItemModal)

        viewModel.onAddItemSave()
        awaitUntil { server.requestCount == 4 }
        awaitUntil { viewModel.uiState.value.addItemModal == null }
    }

    @Test
    fun `save with blank title does not create item`() = runTest(testDispatcher) {
        loadFixtures()
        viewModel.onAddAffordanceTapped()
        awaitUntil { viewModel.uiState.value.addItemModal != null }
        viewModel.onAddItemTitleChanged("  ")
        awaitUntil { viewModel.uiState.value.addItemModal?.title == "  " }

        viewModel.onAddItemSave()
        advanceUntilIdle()

        assertEquals(2, server.requestCount)
        assertNotNull(viewModel.uiState.value.addItemModal)
    }

    @Test
    fun `discard closes modal without creating item`() = runTest(testDispatcher) {
        loadFixtures()
        viewModel.onAddAffordanceTapped()
        awaitUntil { viewModel.uiState.value.addItemModal != null }

        viewModel.onAddItemDiscard()
        awaitUntil { viewModel.uiState.value.addItemModal == null }

        assertEquals(2, server.requestCount)
    }

    @Test
    fun `remote item updated event merges while optimistic check in flight`() = runTest(testDispatcher) {
        loadFixtures()
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(itemJson(id = 10, name = "Milk", isChecked = true)),
        )

        viewModel.onCheckToggled(viewModel.uiState.value.items.first { it.id == 10L })
        awaitUntil { server.requestCount == 3 }

        itemsRepository.applyUpdated(
            eu.pascariu.shoppinglist.data.model.Item(
                id = 11L,
                listId = 1L,
                name = "Bread renamed",
                isChecked = false,
                orderMap = mapOf("1" to "a1"),
                quantity = 1,
                isTemporary = false,
                checkCount = 0,
                expiresAt = null,
                createdAt = "2026-10-22T09:00:00Z",
                visibleIn = listOf(1L),
            ),
        )
        awaitUntil { viewModel.uiState.value.items.first { it.id == 11L }.name == "Bread renamed" }

        assertTrue(viewModel.uiState.value.items.first { it.id == 10L }.isChecked)
        assertEquals(1, undoHistoryStore.entries.value.size)
    }

    @Test
    fun `remote item deleted event drops row while undo bar visible`() = runTest(testDispatcher) {
        loadFixtures()
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(itemJson(id = 10, name = "Milk", isChecked = true)),
        )
        viewModel.onCheckToggled(viewModel.uiState.value.items.first { it.id == 10L })
        awaitUntil { viewModel.uiState.value.showUndoBar }

        itemsRepository.applyDeleted(11L)
        awaitUntil { viewModel.uiState.value.items.none { it.id == 11L } }

        assertEquals(listOf(10L), viewModel.uiState.value.items.map { it.id })
        assertTrue(viewModel.uiState.value.showUndoBar)
    }
}

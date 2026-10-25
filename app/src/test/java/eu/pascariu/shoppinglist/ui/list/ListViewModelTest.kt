package eu.pascariu.shoppinglist.ui.list

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import eu.pascariu.shoppinglist.data.api.ItemsApi
import eu.pascariu.shoppinglist.data.api.ShopsApi
import eu.pascariu.shoppinglist.data.connectivity.BackendConnectivity
import eu.pascariu.shoppinglist.data.local.FakeItemDao
import eu.pascariu.shoppinglist.data.local.FakePendingMutationDao
import eu.pascariu.shoppinglist.data.local.MutationQueue
import eu.pascariu.shoppinglist.data.local.FakeShopDao
import eu.pascariu.shoppinglist.data.model.Item
import eu.pascariu.shoppinglist.data.prefs.ListPreferencesDataSource
import eu.pascariu.shoppinglist.data.prefs.ThemeRepository
import eu.pascariu.shoppinglist.data.prefs.SortOrder
import eu.pascariu.shoppinglist.data.realtime.ChangeToastLabel
import eu.pascariu.shoppinglist.data.realtime.RealtimeSyncManagerContract
import eu.pascariu.shoppinglist.data.repo.ItemsRepository
import eu.pascariu.shoppinglist.data.repo.ShopsRepository
import eu.pascariu.shoppinglist.data.undo.UndoHistoryStore
import eu.pascariu.shoppinglist.domain.search.GLOBAL_FALLBACK_ID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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
import java.time.Instant
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
    private lateinit var listPreferences: ListPreferencesDataSource
    private lateinit var listPreferencesDataStore: DataStore<Preferences>
    private lateinit var themeRepository: ThemeRepository
    private lateinit var expiryTick: MutableStateFlow<Long>
    private lateinit var fakeSyncManager: FakeRealtimeSyncManager
    private lateinit var connectivityFlow: MutableStateFlow<BackendConnectivity>
    private lateinit var viewModel: ListViewModel

    private val json = Json

    private fun offlineAwareQueue(): MutationQueue = MutationQueue(
        dao = FakePendingMutationDao(),
        connectivity = MutableStateFlow(BackendConnectivity.REACHABLE),
        json = Json { ignoreUnknownKeys = true },
    )

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
        itemsRepository = ItemsRepository(
            retrofit.create(ItemsApi::class.java),
            FakeItemDao(),
            offlineAwareQueue(),
            CoroutineScope(testDispatcher + Job()),
        )
        shopsRepository = ShopsRepository(
            retrofit.create(ShopsApi::class.java),
            FakeShopDao(),
            offlineAwareQueue(),
            CoroutineScope(testDispatcher + Job()),
        )
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
        listPreferencesDataStore = PreferenceDataStoreFactory.create(scope = storeScope) {
            java.io.File(tmpFolder.root, "list_prefs_viewmodel_test.preferences_pb")
        }
        listPreferences = ListPreferencesDataSource(
            dataStore = listPreferencesDataStore,
            storeScope = storeScope,
            ioDispatcher = testDispatcher,
        )
        themeRepository = ThemeRepository(
            dataStore = listPreferencesDataStore,
            storeScope = storeScope,
            ioDispatcher = testDispatcher,
        )
        expiryTick = MutableStateFlow(System.currentTimeMillis())
        fakeSyncManager = FakeRealtimeSyncManager()
        connectivityFlow = MutableStateFlow(BackendConnectivity.REACHABLE)
        viewModel = ListViewModel(
            itemsRepository = itemsRepository,
            shopsRepository = shopsRepository,
            connectivity = connectivityFlow,
            undoHistoryStore = undoHistoryStore,
            listPreferences = listPreferences,
            themeRepository = themeRepository,
            realtimeSyncManager = fakeSyncManager,
            expiryTicker = expiryTick,
        )
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
        orderKey: String? = "a0",
        visibleIn: String = "[1]",
        checkCount: Int = 0,
        quantity: Int = 1,
        isTemporary: Boolean = false,
        expiresAt: String? = null,
    ): String =
        """
        {
          "id": $id,
          "listId": 1,
          "name": "$name",
          "isChecked": $isChecked,
          "orderMap": ${if (orderKey == null) "{}" else "{\"1\": \"$orderKey\"}"},
          "quantity": $quantity,
          "isTemporary": $isTemporary,
          "checkCount": $checkCount,
          "expiresAt": ${if (expiresAt == null) "null" else "\"$expiresAt\""},
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
        assertFalse(state.canUndo)
    }

    @Test
    fun `refresh while offline does not hit the network or set an error`() = runTest(testDispatcher) {
        connectivityFlow.value = BackendConnectivity.UNREACHABLE

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(0, server.requestCount)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `failed refresh retries on reconnect and clears error on success`() = runTest(testDispatcher) {
        connectivityFlow.value = BackendConnectivity.UNREACHABLE
        advanceUntilIdle()

        server.enqueue(MockResponse().setResponseCode(500))
        connectivityFlow.value = BackendConnectivity.REACHABLE
        awaitUntil { viewModel.uiState.value.errorMessage != null }
        assertEquals("Couldn't refresh the list", viewModel.uiState.value.errorMessage)

        enqueueItemList(itemJson(10, "Milk", orderKey = "a0"))
        enqueueShopList(shopJson(1, "Global"))
        connectivityFlow.value = BackendConnectivity.UNREACHABLE
        advanceUntilIdle()
        connectivityFlow.value = BackendConnectivity.REACHABLE
        awaitUntil { viewModel.uiState.value.errorMessage == null }
        assertEquals(3, server.requestCount)
    }

    @Test
    fun `reconnect without prior failure does not duplicate the initial refresh`() = runTest(testDispatcher) {
        enqueueItemList(itemJson(10, "Milk", orderKey = "a0"))
        enqueueShopList(shopJson(1, "Global"))
        viewModel.refresh()
        awaitUntil { shopsRepository.shops.value.isNotEmpty() && itemsRepository.items.value.isNotEmpty() }
        assertEquals(2, server.requestCount)

        connectivityFlow.value = BackendConnectivity.UNREACHABLE
        advanceUntilIdle()
        connectivityFlow.value = BackendConnectivity.REACHABLE
        advanceUntilIdle()

        assertEquals(2, server.requestCount)
        assertNull(viewModel.uiState.value.errorMessage)
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
        awaitUntil { viewModel.uiState.value.canUndo }

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

        assertFalse(viewModel.uiState.value.canUndo)
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
        awaitUntil { viewModel.uiState.value.canUndo }

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
        assertEquals("/api/v1/items/10", checkRequest.path)
        val checkBody = checkRequest.body.readUtf8()
        assertTrue(checkBody.contains("\"isChecked\":true"))
        assertFalse(checkBody.contains("isUndo"))
        val undoRequest = server.takeRequest(5, TimeUnit.SECONDS)
        assertNotNull(undoRequest)
        assertEquals("PATCH", undoRequest!!.method)
        assertEquals("/api/v1/items/10", undoRequest.path)
        assertTrue(undoRequest.body.readUtf8().contains("\"isUndo\":true"))
        awaitUntil { !viewModel.uiState.value.canUndo }
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
        awaitUntil { viewModel.uiState.value.canUndo }

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
        awaitUntil { viewModel.uiState.value.canUndo }

        server.enqueue(
            MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(itemJson(id = 10, name = "Milk", isChecked = false)),
        )
        itemsRepository.setChecked(itemId = 10L, isChecked = false)
        awaitUntil { !viewModel.uiState.value.items.first { it.id == 10L }.isChecked }

        viewModel.onUndoTapped()
        awaitUntil { !viewModel.uiState.value.canUndo }

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
        awaitUntil { viewModel.uiState.value.canUndo }

        itemsRepository.applyDeleted(10L)
        awaitUntil { itemsRepository.items.value.none { it.id == 10L } }

        viewModel.onUndoTapped()
        awaitUntil { !viewModel.uiState.value.canUndo }

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
        awaitUntil { viewModel.uiState.value.canUndo }

        server.enqueue(MockResponse().setResponseCode(500).setHeader("Content-Type", "application/json").setBody("{\"error\":\"boom\"}"))
        viewModel.onUndoTapped()
        awaitUntil { server.requestCount == 4 }
        awaitUntil { viewModel.uiState.value.items.first { it.id == 10L }.isChecked }

        assertTrue(viewModel.uiState.value.canUndo)
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
        assertEquals("/api/v1/lists/1/items", recorded.path)
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
    fun `second add after successful save is not blocked by stale guard`() = runTest(testDispatcher) {
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
        awaitUntil { viewModel.uiState.value.addItemModal == null }

        viewModel.onAddAffordanceTapped()
        awaitUntil { viewModel.uiState.value.addItemModal != null }
        viewModel.onAddItemTitleChanged("Butter")
        awaitUntil { viewModel.uiState.value.addItemModal?.title == "Butter" }
        server.enqueue(
            MockResponse().setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody(itemJson(id = 13, name = "Butter")),
        )

        viewModel.onAddItemSave()
        awaitUntil { viewModel.uiState.value.addItemModal == null }

        assertEquals(4, server.requestCount)
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
    fun `long press opens edit modal prefilled with item name and visibility`() = runTest(testDispatcher) {
        loadFixtures()

        viewModel.onItemLongPressed(viewModel.uiState.value.items.first { it.id == 11L })
        awaitUntil { viewModel.uiState.value.editItemModal != null }

        val modal = viewModel.uiState.value.editItemModal
        assertNotNull(modal)
        assertEquals(11L, modal?.itemId)
        assertEquals("Bread", modal?.name)
        assertEquals("Bread", modal?.originalName)
        assertEquals(listOf("Global", "Auchan"), modal?.availableViews?.map { it.name })
        assertEquals(setOf(1L), modal?.selectedViewIds)
    }

    @Test
    fun `edit modal canSave is false for blank or unchanged name and true for changed name`() {
        val base = EditItemUiState(
            itemId = 11L,
            originalName = "Bread",
            name = "Bread",
            availableViews = emptyList(),
            selectedViewIds = setOf(1L),
        )
        assertFalse(base.canSave)
        assertFalse(base.copy(name = "  ").canSave)
        assertTrue(base.copy(name = "Toast").canSave)
        assertTrue(base.isUnchanged)
        assertTrue(base.copy(name = " Bread ").isUnchanged)
        assertFalse(base.copy(name = " Bread ").canSave)
        assertFalse(base.copy(name = "Toast").isUnchanged)
        assertTrue(base.allViewsSelected)
        assertTrue(base.copy(selectedViewIds = emptySet()).allViewsSelected)
        assertTrue(base.copy(selectedViewIds = setOf(1L, 2L)).canSave)
        assertFalse(base.copy(selectedViewIds = setOf(1L, 2L)).isUnchanged)
        assertFalse(base.copy(selectedViewIds = setOf(1L, 2L), originalVisibleIn = setOf(1L, 2L)).canSave)
    }

    @Test
    fun `edit modal check count refreshes while modal is open`() = runTest(testDispatcher) {
        loadFixtures()

        viewModel.onItemLongPressed(viewModel.uiState.value.items.first { it.id == 11L })
        awaitUntil { viewModel.uiState.value.editItemModal != null }
        assertEquals(0, viewModel.uiState.value.editItemModal?.checkCount)

        server.enqueue(
            MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(itemJson(id = 11, name = "Bread", isChecked = true, checkCount = 7)),
        )

        viewModel.onCheckToggled(viewModel.uiState.value.items.first { it.id == 11L })
        awaitUntil { viewModel.uiState.value.editItemModal?.checkCount == 7 }
    }

    @Test
    fun `edit modal title change and view toggle update state`() = runTest(testDispatcher) {
        loadFixtures()
        viewModel.onItemLongPressed(viewModel.uiState.value.items.first { it.id == 11L })
        awaitUntil { viewModel.uiState.value.editItemModal != null }

        viewModel.onEditItemTitleChanged("Crackers")
        awaitUntil { viewModel.uiState.value.editItemModal?.name == "Crackers" }

        viewModel.onEditItemViewToggled(2L)
        awaitUntil { viewModel.uiState.value.editItemModal?.selectedViewIds == setOf(1L, 2L) }

        viewModel.onEditItemToggleAllTapped()
        awaitUntil { viewModel.uiState.value.editItemModal?.selectedViewIds == setOf(1L) }

        viewModel.onEditItemViewToggled(1L)
        awaitUntil { viewModel.uiState.value.editItemModal?.selectedViewIds == emptySet<Long>() }

        viewModel.onEditItemToggleAllTapped()
        awaitUntil { viewModel.uiState.value.editItemModal?.selectedViewIds == setOf(1L, 2L) }

        assertEquals("Crackers", viewModel.uiState.value.editItemModal?.name)
        assertEquals(setOf(1L, 2L), viewModel.uiState.value.editItemModal?.selectedViewIds)
    }

    @Test
    fun `edit modal discard closes without request`() = runTest(testDispatcher) {
        loadFixtures()
        viewModel.onItemLongPressed(viewModel.uiState.value.items.first { it.id == 11L })
        awaitUntil { viewModel.uiState.value.editItemModal != null }

        viewModel.onEditItemTitleChanged("Crackers")
        awaitUntil { viewModel.uiState.value.editItemModal?.name == "Crackers" }

        viewModel.onEditItemDiscard()
        awaitUntil { viewModel.uiState.value.editItemModal == null }

        assertEquals(2, server.requestCount)
        assertEquals("Bread", viewModel.uiState.value.items.first { it.id == 11L }.name)
    }

    @Test
    fun `edit modal save patches name and closes`() = runTest(testDispatcher) {
        loadFixtures()
        viewModel.onItemLongPressed(viewModel.uiState.value.items.first { it.id == 11L })
        awaitUntil { viewModel.uiState.value.editItemModal != null }
        viewModel.onEditItemTitleChanged("Crackers")
        awaitUntil { viewModel.uiState.value.editItemModal?.name == "Crackers" }
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(itemJson(id = 11, name = "Crackers")),
        )

        viewModel.onEditItemSave()
        awaitUntil { server.requestCount == 3 }

        val recorded = server.takeRequest(5, TimeUnit.SECONDS)
        assertNotNull(recorded)
        assertEquals("PATCH", recorded!!.method)
        assertEquals("/api/v1/items/11", recorded.path)
        assertTrue(recorded.body.readUtf8().contains("\"name\":\"Crackers\""))
        awaitUntil { viewModel.uiState.value.editItemModal == null }
        assertEquals("Crackers", viewModel.uiState.value.items.first { it.id == 11L }.name)
    }

    @Test
    fun `edit modal save with unchanged name does not patch`() = runTest(testDispatcher) {
        loadFixtures()
        viewModel.onItemLongPressed(viewModel.uiState.value.items.first { it.id == 11L })
        awaitUntil { viewModel.uiState.value.editItemModal != null }

        viewModel.onEditItemSave()
        advanceUntilIdle()

        assertEquals(2, server.requestCount)
        assertNotNull(viewModel.uiState.value.editItemModal)
    }

    @Test
    fun `edit modal save with only visibility change patches visibleIn and closes`() = runTest(testDispatcher) {
        loadFixtures()
        viewModel.onItemLongPressed(viewModel.uiState.value.items.first { it.id == 11L })
        awaitUntil { viewModel.uiState.value.editItemModal != null }
        viewModel.onEditItemViewToggled(2L)
        awaitUntil { viewModel.uiState.value.editItemModal?.selectedViewIds == setOf(1L, 2L) }
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(itemJson(id = 11, name = "Bread", visibleIn = "[1, 2]")),
        )

        viewModel.onEditItemSave()
        awaitUntil { server.requestCount == 3 }

        val recorded = server.takeRequest(5, TimeUnit.SECONDS)
        assertNotNull(recorded)
        assertEquals("PATCH", recorded!!.method)
        assertEquals("/api/v1/items/11", recorded.path)
        val body = recorded.body.readUtf8()
        assertFalse(body.contains("\"name\""))
        assertTrue(body.contains("\"visibleIn\":[1,2]"))
        awaitUntil { viewModel.uiState.value.editItemModal == null }
        assertEquals(listOf(1L, 2L), viewModel.uiState.value.items.first { it.id == 11L }.visibleIn)
    }

    @Test
    fun `edit modal toggle all selects everything and deselects all but Global`() = runTest(testDispatcher) {
        loadFixtures()
        viewModel.onItemLongPressed(viewModel.uiState.value.items.first { it.id == 11L })
        awaitUntil { viewModel.uiState.value.editItemModal != null }
        assertEquals(setOf(1L), viewModel.uiState.value.editItemModal?.selectedViewIds)

        viewModel.onEditItemToggleAllTapped()
        awaitUntil { viewModel.uiState.value.editItemModal?.selectedViewIds == setOf(1L, 2L) }

        viewModel.onEditItemToggleAllTapped()
        awaitUntil { viewModel.uiState.value.editItemModal?.selectedViewIds == setOf(1L) }

        assertEquals(setOf(1L), viewModel.uiState.value.editItemModal?.selectedViewIds)
    }

    @Test
    fun `edit modal save strips fallback global sentinel from visibleIn`() = runTest(testDispatcher) {
        enqueueItemList(itemJson(11, "Bread", orderKey = "a1"))
        enqueueShopList(shopJson(2, "Auchan"))
        viewModel.refresh()
        awaitUntil { shopsRepository.shops.value.isNotEmpty() && itemsRepository.items.value.isNotEmpty() }
        server.takeRequest(5, TimeUnit.SECONDS)
        server.takeRequest(5, TimeUnit.SECONDS)

        viewModel.onItemLongPressed(viewModel.uiState.value.items.first { it.id == 11L })
        awaitUntil { viewModel.uiState.value.editItemModal != null }
        assertEquals(
            listOf("Global", "Auchan"),
            viewModel.uiState.value.editItemModal?.availableViews?.map { it.name },
        )

        viewModel.onEditItemToggleAllTapped()
        awaitUntil { viewModel.uiState.value.editItemModal?.selectedViewIds == setOf(GLOBAL_FALLBACK_ID, 2L) }

        viewModel.onEditItemToggleAllTapped()
        awaitUntil { viewModel.uiState.value.editItemModal?.selectedViewIds == setOf(GLOBAL_FALLBACK_ID) }
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(itemJson(id = 11, name = "Bread", visibleIn = "[]")),
        )

        viewModel.onEditItemSave()
        awaitUntil { server.requestCount == 3 }

        val recorded = server.takeRequest(5, TimeUnit.SECONDS)
        assertNotNull(recorded)
        val body = recorded!!.body.readUtf8()
        assertTrue(body.contains("\"visibleIn\":[]"))
        assertFalse(body.contains("-2"))
        awaitUntil { viewModel.uiState.value.editItemModal == null }
    }

    @Test
    fun `edit modal save failure restores name and shows error`() = runTest(testDispatcher) {
        loadFixtures()
        viewModel.onItemLongPressed(viewModel.uiState.value.items.first { it.id == 11L })
        awaitUntil { viewModel.uiState.value.editItemModal != null }
        viewModel.onEditItemTitleChanged("Crackers")
        awaitUntil { viewModel.uiState.value.editItemModal?.name == "Crackers" }
        server.enqueue(MockResponse().setResponseCode(500))

        viewModel.onEditItemSave()
        awaitUntil { server.requestCount == 3 }
        advanceUntilIdle()

        assertEquals("Bread", viewModel.uiState.value.items.first { it.id == 11L }.name)
        assertNotNull(viewModel.uiState.value.errorMessage)
        assertNull(viewModel.uiState.value.editItemModal)
    }

    @Test
    fun `edit modal save ignores duplicate taps while patch in flight`() = runTest(testDispatcher) {
        loadFixtures()
        viewModel.onItemLongPressed(viewModel.uiState.value.items.first { it.id == 11L })
        awaitUntil { viewModel.uiState.value.editItemModal != null }
        viewModel.onEditItemTitleChanged("Crackers")
        awaitUntil { viewModel.uiState.value.editItemModal?.name == "Crackers" }

        viewModel.onEditItemSave()
        viewModel.onEditItemSave()
        awaitUntil { server.requestCount == 3 }

        server.enqueue(
            MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(itemJson(id = 11, name = "Crackers")),
        )
        awaitUntil { viewModel.uiState.value.editItemModal == null }

        assertEquals(3, server.requestCount)
    }

    @Test
    fun `drag disabled while edit modal open and restored after close`() = runTest(testDispatcher) {
        loadFixtures()
        awaitUntil { viewModel.uiState.value.dragEnabled }

        viewModel.onItemLongPressed(viewModel.uiState.value.items.first { it.id == 11L })
        awaitUntil { viewModel.uiState.value.editItemModal != null }
        assertFalse(viewModel.uiState.value.dragEnabled)

        viewModel.onEditItemDiscard()
        awaitUntil { viewModel.uiState.value.editItemModal == null }
        assertTrue(viewModel.uiState.value.dragEnabled)
    }

    @Test
    fun `delete tap closes edit modal and opens confirmation`() = runTest(testDispatcher) {
        loadFixtures()
        viewModel.onItemLongPressed(viewModel.uiState.value.items.first { it.id == 11L })
        awaitUntil { viewModel.uiState.value.editItemModal != null }

        viewModel.onEditItemDeleteTapped()
        awaitUntil { viewModel.uiState.value.editItemModal == null }
        awaitUntil { viewModel.uiState.value.deleteConfirmItem != null }

        assertEquals(11L, viewModel.uiState.value.deleteConfirmItem?.id)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `delete confirm removes item optimistically`() = runTest(testDispatcher) {
        loadFixtures()
        viewModel.onItemLongPressed(viewModel.uiState.value.items.first { it.id == 11L })
        awaitUntil { viewModel.uiState.value.editItemModal != null }
        viewModel.onEditItemDeleteTapped()
        awaitUntil { viewModel.uiState.value.deleteConfirmItem != null }

        server.enqueue(MockResponse().setResponseCode(204))
        viewModel.onDeleteConfirmed()
        awaitUntil { viewModel.uiState.value.deleteConfirmItem == null }
        awaitUntil { server.requestCount == 3 }

        val recorded = server.takeRequest(5, TimeUnit.SECONDS)
        assertNotNull(recorded)
        assertEquals("DELETE", recorded!!.method)
        assertEquals("/api/v1/items/11", recorded.path)
        awaitUntil { viewModel.uiState.value.items.none { it.id == 11L } }
        assertNull(viewModel.uiState.value.deleteConfirmItem)
    }

    @Test
    fun `delete cancel closes confirmation without request`() = runTest(testDispatcher) {
        loadFixtures()
        viewModel.onItemLongPressed(viewModel.uiState.value.items.first { it.id == 11L })
        awaitUntil { viewModel.uiState.value.editItemModal != null }
        viewModel.onEditItemDeleteTapped()
        awaitUntil { viewModel.uiState.value.deleteConfirmItem != null }

        viewModel.onDeleteCancelled()
        awaitUntil { viewModel.uiState.value.deleteConfirmItem == null }

        assertEquals(2, server.requestCount)
        assertEquals(listOf("Milk", "Bread"), viewModel.uiState.value.items.map { it.name })
    }

    @Test
    fun `delete confirm closes dialog synchronously before request settles`() = runTest(testDispatcher) {
        loadFixtures()
        viewModel.onItemLongPressed(viewModel.uiState.value.items.first { it.id == 11L })
        awaitUntil { viewModel.uiState.value.editItemModal != null }
        viewModel.onEditItemDeleteTapped()
        awaitUntil { viewModel.uiState.value.deleteConfirmItem != null }
        server.enqueue(MockResponse().setResponseCode(204).setBodyDelay(2, TimeUnit.SECONDS))

        viewModel.onDeleteConfirmed()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.deleteConfirmItem)
        awaitUntil { server.requestCount == 3 }
        awaitUntil { viewModel.uiState.value.items.none { it.id == 11L } }
        assertNull(viewModel.uiState.value.deleteConfirmItem)
    }

    @Test
    fun `delete failure restores item at its previous position and shows error`() = runTest(testDispatcher) {
        loadFixtures()
        viewModel.onItemLongPressed(viewModel.uiState.value.items.first { it.id == 10L })
        awaitUntil { viewModel.uiState.value.editItemModal != null }
        viewModel.onEditItemDeleteTapped()
        awaitUntil { viewModel.uiState.value.deleteConfirmItem != null }
        server.enqueue(MockResponse().setResponseCode(500))

        viewModel.onDeleteConfirmed()
        awaitUntil { server.requestCount == 3 }
        advanceUntilIdle()

        assertEquals(listOf("Milk", "Bread"), viewModel.uiState.value.items.map { it.name })
        assertEquals(listOf(10L, 11L), itemsRepository.items.value.map { it.id })
        assertNotNull(viewModel.uiState.value.errorMessage)
        assertNull(viewModel.uiState.value.deleteConfirmItem)
    }

    @Test
    fun `delete failure restores item at its previous cache position`() = runTest(testDispatcher) {
        loadFixtures()
        viewModel.onItemLongPressed(viewModel.uiState.value.items.first { it.id == 10L })
        awaitUntil { viewModel.uiState.value.editItemModal != null }
        viewModel.onEditItemDeleteTapped()
        awaitUntil { viewModel.uiState.value.deleteConfirmItem != null }
        server.enqueue(MockResponse().setResponseCode(500))

        viewModel.onDeleteConfirmed()
        awaitUntil { server.requestCount == 3 }
        advanceUntilIdle()

        assertEquals(listOf(10L, 11L), itemsRepository.items.value.map { it.id })
    }

    @Test
    fun `delete 404 keeps item deleted without error`() = runTest(testDispatcher) {
        loadFixtures()
        viewModel.onItemLongPressed(viewModel.uiState.value.items.first { it.id == 11L })
        awaitUntil { viewModel.uiState.value.editItemModal != null }
        viewModel.onEditItemDeleteTapped()
        awaitUntil { viewModel.uiState.value.deleteConfirmItem != null }
        server.enqueue(MockResponse().setResponseCode(404))

        viewModel.onDeleteConfirmed()
        awaitUntil { server.requestCount == 3 }
        advanceUntilIdle()

        awaitUntil { viewModel.uiState.value.items.none { it.id == 11L } }
        assertNull(viewModel.uiState.value.errorMessage)
        assertNull(viewModel.uiState.value.deleteConfirmItem)
    }

    @Test
    fun `edit modal save with item removed from cache does not patch`() = runTest(testDispatcher) {
        loadFixtures()
        viewModel.onItemLongPressed(viewModel.uiState.value.items.first { it.id == 11L })
        awaitUntil { viewModel.uiState.value.editItemModal != null }
        viewModel.onEditItemTitleChanged("Crackers")
        awaitUntil { viewModel.uiState.value.editItemModal?.name == "Crackers" }

        itemsRepository.applyDeleted(11L)
        awaitUntil { viewModel.uiState.value.items.none { it.id == 11L } }
        viewModel.onEditItemSave()
        advanceUntilIdle()

        assertEquals(2, server.requestCount)
        assertNotNull(viewModel.uiState.value.editItemModal)
    }

    @Test
    fun `delete tap with item removed from cache does not open confirmation`() = runTest(testDispatcher) {
        loadFixtures()
        viewModel.onItemLongPressed(viewModel.uiState.value.items.first { it.id == 11L })
        awaitUntil { viewModel.uiState.value.editItemModal != null }

        itemsRepository.applyDeleted(11L)
        awaitUntil { viewModel.uiState.value.items.none { it.id == 11L } }
        viewModel.onEditItemDeleteTapped()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.editItemModal)
        assertNull(viewModel.uiState.value.deleteConfirmItem)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `delete confirm with item vanished closes confirmation without request`() = runTest(testDispatcher) {
        loadFixtures()
        viewModel.onItemLongPressed(viewModel.uiState.value.items.first { it.id == 11L })
        awaitUntil { viewModel.uiState.value.editItemModal != null }
        viewModel.onEditItemDeleteTapped()
        awaitUntil { viewModel.uiState.value.deleteConfirmItem != null }

        itemsRepository.applyDeleted(11L)
        viewModel.onDeleteConfirmed()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.deleteConfirmItem)
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
    fun `active view defaults to Global`() = runTest(testDispatcher) {
        loadFixtures()

        val state = viewModel.uiState.value
        assertEquals(1L, state.activeViewId)
        assertEquals("Global", state.watermarkName)
    }

    @Test
    fun `remote item updated with changed visibleIn re-filters active view`() = runTest(testDispatcher) {
        enqueueItemList(
            itemJson(10, "Milk", orderKey = "a0", visibleIn = "[1]"),
            itemJson(11, "Bread", orderKey = "a1", visibleIn = "[1,2]"),
        )
        enqueueShopList(shopJson(1, "Global"), shopJson(2, "Auchan"))
        viewModel.refresh()
        awaitUntil { shopsRepository.shops.value.isNotEmpty() && itemsRepository.items.value.isNotEmpty() }
        server.takeRequest(5, TimeUnit.SECONDS)
        server.takeRequest(5, TimeUnit.SECONDS)

        viewModel.onViewSelected(2L)
        awaitUntil { viewModel.uiState.value.activeViewId == 2L }
        assertEquals(listOf("Bread"), viewModel.uiState.value.filteredItems.map { it.name })

        itemsRepository.applyUpdated(
            eu.pascariu.shoppinglist.data.model.Item(
                id = 11L,
                listId = 1L,
                name = "Bread",
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
        awaitUntil { viewModel.uiState.value.filteredItems.isEmpty() }
    }

    @Test
    fun `selecting a view filters and sorts by that view`() = runTest(testDispatcher) {
        enqueueItemList(
            itemJson(10, "Milk", orderKey = "a0", visibleIn = "[1]"),
            itemJson(11, "Bread", orderKey = "a1", visibleIn = "[1,2]"),
            itemJson(12, "Chips", orderKey = "a0", visibleIn = "[2]"),
        )
        enqueueShopList(shopJson(1, "Global"), shopJson(2, "Auchan"))
        viewModel.refresh()
        awaitUntil { shopsRepository.shops.value.isNotEmpty() && itemsRepository.items.value.isNotEmpty() }
        server.takeRequest(5, TimeUnit.SECONDS)
        server.takeRequest(5, TimeUnit.SECONDS)

        viewModel.onViewSelected(2L)
        awaitUntil { viewModel.uiState.value.activeViewId == 2L }

        val state = viewModel.uiState.value
        assertEquals(2L, state.activeViewId)
        assertEquals("Auchan", state.watermarkName)
        assertEquals(listOf("Bread", "Chips"), state.items.map { it.name })
    }

    @Test
    fun `fallback to Global when active shop is deleted`() = runTest(testDispatcher) {
        loadFixtures()
        viewModel.onViewSelected(2L)
        awaitUntil { viewModel.uiState.value.activeViewId == 2L }

        shopsRepository.applyDeleted(2L)
        awaitUntil { viewModel.uiState.value.activeViewId == 1L }

        assertEquals(1L, viewModel.uiState.value.activeViewId)
        assertEquals("Global", viewModel.uiState.value.watermarkName)
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
        awaitUntil { viewModel.uiState.value.canUndo }

        itemsRepository.applyDeleted(11L)
        awaitUntil { viewModel.uiState.value.items.none { it.id == 11L } }

        assertEquals(listOf(10L), viewModel.uiState.value.items.map { it.id })
        assertTrue(viewModel.uiState.value.canUndo)
    }

    @Test
    fun `add shop opens create modal with empty name`() = runTest(testDispatcher) {
        loadFixtures()

        viewModel.onAddShopTapped()
        awaitUntil { viewModel.uiState.value.shopModal != null }

        val modal = viewModel.uiState.value.shopModal
        assertNotNull(modal)
        assertEquals(ShopModalMode.CREATE, modal?.mode)
        assertEquals("", modal?.name)
        assertFalse(modal?.canSave ?: true)
    }

    @Test
    fun `edit shop opens modal pre-filled and save disabled while unchanged`() = runTest(testDispatcher) {
        loadFixtures()

        viewModel.onShopEditTapped(2L)
        awaitUntil { viewModel.uiState.value.shopModal != null }

        val modal = viewModel.uiState.value.shopModal
        assertNotNull(modal)
        assertEquals(ShopModalMode.EDIT, modal?.mode)
        assertEquals(2L, modal?.shopId)
        assertEquals("Auchan", modal?.name)
        assertTrue(modal?.isUnchanged == true)
        assertFalse(modal?.canSave ?: true)
        assertEquals("Close", modal?.secondaryLabel)
    }

    @Test
    fun `shop name change enables save and trims on save`() = runTest(testDispatcher) {
        loadFixtures()
        viewModel.onShopEditTapped(2L)

        viewModel.onShopNameChanged("  Auchan Nord  ")
        awaitUntil { viewModel.uiState.value.shopModal?.name == "  Auchan Nord  " }
        val modal = viewModel.uiState.value.shopModal
        assertNotNull(modal)
        assertFalse(modal?.isUnchanged ?: true)
        assertTrue(modal?.canSave ?: false)
        assertEquals("Discard", modal?.secondaryLabel)
        assertEquals("  Auchan Nord  ", modal?.name)
    }

    @Test
    fun `whitespace-only name cannot be saved`() = runTest(testDispatcher) {
        loadFixtures()

        viewModel.onAddShopTapped()
        awaitUntil { viewModel.uiState.value.shopModal != null }
        viewModel.onShopNameChanged("   ")
        awaitUntil { viewModel.uiState.value.shopModal?.name == "   " }

        val modal = viewModel.uiState.value.shopModal
        assertNotNull(modal)
        assertFalse(modal?.canSave ?: true)
    }

    @Test
    fun `saving create mode creates shop and closes modal`() = runTest(testDispatcher) {
        loadFixtures()
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(shopJson(3, "Lidl")),
        )

        viewModel.onAddShopTapped()
        viewModel.onShopNameChanged("Lidl")
        viewModel.onShopSave()
        awaitUntil { viewModel.uiState.value.shopModal == null }

        awaitUntil { shopsRepository.shops.value.any { it.id == 3L } }
        assertEquals(listOf("Global", "Auchan", "Lidl"), shopsRepository.shops.value.map { it.name })
        val drawer = viewModel.uiState.value.drawerShops
        assertEquals(listOf("Global", "Auchan", "Lidl"), drawer.map { it.name })
    }

    @Test
    fun `saving rename updates shop everywhere`() = runTest(testDispatcher) {
        loadFixtures()
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(shopJson(2, "Auchan Express")),
        )

        viewModel.onShopEditTapped(2L)
        viewModel.onShopNameChanged("Auchan Express")
        viewModel.onShopSave()
        awaitUntil { viewModel.uiState.value.shopModal == null }

        awaitUntil { viewModel.uiState.value.drawerShops.any { it.name == "Auchan Express" } }
        assertEquals(listOf("Global", "Auchan Express"), viewModel.uiState.value.drawerShops.map { it.name })
    }

    @Test
    fun `failed create keeps modal open and surfaces error`() = runTest(testDispatcher) {
        loadFixtures()
        server.enqueue(
            MockResponse().setResponseCode(409)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"error": "duplicate"}"""),
        )

        viewModel.onAddShopTapped()
        viewModel.onShopNameChanged("Auchan")
        viewModel.onShopSave()
        awaitUntil { viewModel.uiState.value.shopModal?.error != null }

        val modal = viewModel.uiState.value.shopModal
        assertNotNull(modal)
        assertFalse(modal?.saving ?: true)
        assertNotNull(modal?.error)
    }

    @Test
    fun `discard closes shop modal`() = runTest(testDispatcher) {
        loadFixtures()

        viewModel.onAddShopTapped()
        viewModel.onShopDiscard()

        assertNull(viewModel.uiState.value.shopModal)
    }

    @Test
    fun `delete confirmation flow deletes shop and closes dialogs`() = runTest(testDispatcher) {
        loadFixtures()
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"status": "ok"}"""),
        )

        viewModel.onShopEditTapped(2L)
        awaitUntil { viewModel.uiState.value.shopModal != null }
        viewModel.onShopDeleteTapped()
        awaitUntil { viewModel.uiState.value.shopModal?.deleteConfirmOpen == true }
        val modal = viewModel.uiState.value.shopModal
        assertNotNull(modal)
        assertTrue(modal?.deleteConfirmOpen == true)

        viewModel.onShopDeleteConfirmed()
        awaitUntil { viewModel.uiState.value.shopModal == null }
        awaitUntil { shopsRepository.shops.value.none { it.id == 2L } }

        assertNull(viewModel.uiState.value.shopModal)
        assertTrue(viewModel.uiState.value.drawerShops.none { it.id == 2L })
    }

    @Test
    fun `failed shop delete shows delete-specific error`() = runTest(testDispatcher) {
        loadFixtures()
        server.enqueue(
            MockResponse().setResponseCode(403)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"error": "protected"}"""),
        )

        viewModel.onShopEditTapped(2L)
        awaitUntil { viewModel.uiState.value.shopModal != null }
        viewModel.onShopDeleteTapped()
        awaitUntil { viewModel.uiState.value.shopModal?.deleteConfirmOpen == true }
        viewModel.onShopDeleteConfirmed()
        awaitUntil { viewModel.uiState.value.shopModal?.error != null }

        val modal = viewModel.uiState.value.shopModal
        assertNotNull(modal)
        assertFalse(modal?.saving ?: true)
        assertEquals("The Global shop cannot be deleted", modal?.error)
    }

    @Test
    fun `cancelling delete returns to edit modal`() = runTest(testDispatcher) {
        loadFixtures()

        viewModel.onShopEditTapped(2L)
        awaitUntil { viewModel.uiState.value.shopModal != null }
        viewModel.onShopDeleteTapped()
        awaitUntil { viewModel.uiState.value.shopModal?.deleteConfirmOpen == true }
        viewModel.onShopDeleteCancelled()
        awaitUntil { viewModel.uiState.value.shopModal?.deleteConfirmOpen == false }

        val modal = viewModel.uiState.value.shopModal
        assertNotNull(modal)
        assertFalse(modal?.deleteConfirmOpen ?: true)
    }

    private fun newViewModel(): ListViewModel {
        val instance = ListViewModel(
            itemsRepository = itemsRepository,
            shopsRepository = shopsRepository,
            connectivity = connectivityFlow,
            undoHistoryStore = undoHistoryStore,
            listPreferences = listPreferences,
            themeRepository = themeRepository,
            realtimeSyncManager = fakeSyncManager,
            expiryTicker = expiryTick,
        )
        viewModel = instance
        return instance
    }

    @Test
    fun `valid persisted shop id is restored once shops load`() = runTest(testDispatcher) {
        listPreferences.setActiveShopId(2L)
        advanceUntilIdle()
        enqueueItemList(itemJson(10, "Milk", orderKey = "a0"))
        enqueueShopList(shopJson(1, "Global"), shopJson(2, "Auchan"))

        newViewModel().refresh()
        advanceUntilIdle()
        awaitUntil { shopsRepository.shops.value.isNotEmpty() }

        assertEquals(2L, viewModel.uiState.value.activeViewId)
        assertEquals("Auchan", viewModel.uiState.value.watermarkName)
    }

    @Test
    fun `view selected while shops load is not overwritten by restore`() = runTest(testDispatcher) {
        listPreferences.setActiveShopId(2L)
        advanceUntilIdle()

        newViewModel()
        viewModel.onViewSelected(1L)
        advanceUntilIdle()

        enqueueItemList(itemJson(10, "Milk", orderKey = "a0"))
        enqueueShopList(shopJson(1, "Global"), shopJson(2, "Auchan"))
        viewModel.refresh()
        awaitUntil { shopsRepository.shops.value.isNotEmpty() }
        advanceUntilIdle()

        assertEquals(1L, viewModel.uiState.value.activeViewId)
        assertEquals("Global", viewModel.uiState.value.watermarkName)
    }

    @Test
    fun `selecting placeholder global row clears active shop instead of persisting sentinel`() = runTest(testDispatcher) {
        loadFixtures()
        viewModel.onViewSelected(2L)
        awaitUntil { viewModel.uiState.value.activeViewId == 2L }

        viewModel.onViewSelected(ListViewModel.GLOBAL_PLACEHOLDER_ID)
        advanceUntilIdle()

        assertEquals(1L, viewModel.uiState.value.activeViewId)
        assertEquals("Global", viewModel.uiState.value.watermarkName)
        assertNull(listPreferences.activeShopId.value)
    }

    @Test
    fun `invalid persisted shop id falls back to Global`() = runTest(testDispatcher) {
        listPreferences.setActiveShopId(999L)
        advanceUntilIdle()
        enqueueItemList(itemJson(10, "Milk", orderKey = "a0"))
        enqueueShopList(shopJson(1, "Global"), shopJson(2, "Auchan"))

        newViewModel().refresh()
        advanceUntilIdle()
        awaitUntil { shopsRepository.shops.value.isNotEmpty() }

        assertEquals(1L, viewModel.uiState.value.activeViewId)
        assertEquals("Global", viewModel.uiState.value.watermarkName)
        assertNull(listPreferences.activeShopId.value)
    }

    @Test
    fun `nothing persisted defaults to Global`() = runTest(testDispatcher) {
        enqueueItemList(itemJson(10, "Milk", orderKey = "a0"))
        enqueueShopList(shopJson(1, "Global"), shopJson(2, "Auchan"))

        newViewModel().refresh()
        advanceUntilIdle()
        awaitUntil { shopsRepository.shops.value.isNotEmpty() }

        assertEquals(1L, viewModel.uiState.value.activeViewId)
        assertEquals("Global", viewModel.uiState.value.watermarkName)
    }

    @Test
    fun `persisted id is not applied before shops load`() = runTest(testDispatcher) {
        listPreferences.setActiveShopId(2L)
        advanceUntilIdle()

        newViewModel()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.activeViewId)
        assertEquals("Global", viewModel.uiState.value.watermarkName)
    }

    @Test
    fun `view selection writes the persisted id immediately`() = runTest(testDispatcher) {
        loadFixtures()

        viewModel.onViewSelected(2L)
        advanceUntilIdle()

        assertEquals(2L, listPreferences.activeShopId.value)
    }

    @Test
    fun `quick menu expand state persists and is restored on a new view model`() = runTest(testDispatcher) {
        loadFixtures()

        viewModel.onQuickMenuExpandedChanged(true)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isQuickMenuExpanded)

        val restarted = newViewModel()
        advanceUntilIdle()

        assertTrue(restarted.uiState.value.isQuickMenuExpanded)
    }

    @Test
    fun `quick menu collapse persists`() = runTest(testDispatcher) {
        loadFixtures()

        viewModel.onQuickMenuExpandedChanged(true)
        advanceUntilIdle()
        viewModel.onQuickMenuExpandedChanged(false)
        advanceUntilIdle()

        val restarted = newViewModel()
        advanceUntilIdle()

        assertFalse(restarted.uiState.value.isQuickMenuExpanded)
    }

    @Test
    fun `uncheckedCountByShop counts per-shop and Global counts all`() = runTest(testDispatcher) {
        enqueueItemList(
            itemJson(10, "Milk", isChecked = false, visibleIn = "[1,2]"),
            itemJson(11, "Bread", isChecked = false, visibleIn = "[2]"),
            itemJson(12, "Chips", isChecked = true, visibleIn = "[2]"),
            itemJson(13, "Tea", isChecked = false, visibleIn = "[]"),
        )
        enqueueShopList(shopJson(1, "Global"), shopJson(2, "Auchan"))
        viewModel.refresh()
        awaitUntil { shopsRepository.shops.value.isNotEmpty() && itemsRepository.items.value.isNotEmpty() }
        server.takeRequest(5, TimeUnit.SECONDS)
        server.takeRequest(5, TimeUnit.SECONDS)

        val counts = viewModel.uiState.value.uncheckedCountByShop
        assertEquals(2, counts[1L])
        assertEquals(2, counts[2L])
    }

    @Test
    fun `fully checked shop has zero count`() = runTest(testDispatcher) {
        enqueueItemList(
            itemJson(11, "Bread", isChecked = true, visibleIn = "[2]"),
        )
        enqueueShopList(shopJson(1, "Global"), shopJson(2, "Auchan"))
        viewModel.refresh()
        awaitUntil { shopsRepository.shops.value.isNotEmpty() && itemsRepository.items.value.isNotEmpty() }
        server.takeRequest(5, TimeUnit.SECONDS)
        server.takeRequest(5, TimeUnit.SECONDS)

        val counts = viewModel.uiState.value.uncheckedCountByShop
        assertEquals(0, counts[1L])
        assertEquals(0, counts[2L])
    }

    @Test
    fun `checking an item updates drawer badges and switcher count at once`() = runTest(testDispatcher) {
        loadFixtures()
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(itemJson(id = 10, name = "Milk", isChecked = true)),
        )

        viewModel.onCheckToggled(viewModel.uiState.value.items.first { it.id == 10L })
        awaitUntil { viewModel.uiState.value.uncheckedCountByShop[1L] == 1 }

        val counts = viewModel.uiState.value.uncheckedCountByShop
        assertEquals(1, counts[1L])
        assertEquals(0, counts[2L])
        assertEquals(1, viewModel.uiState.value.drawerShops.first { it.isGlobal }.uncheckedCount)
        assertEquals(0, viewModel.uiState.value.drawerShops.first { !it.isGlobal }.uncheckedCount)
    }

    @Test
    fun `search expanded defaults to collapsed and persists`() = runTest(testDispatcher) {
        loadFixtures()

        assertFalse(viewModel.uiState.value.searchExpanded)

        viewModel.onSearchExpandedChanged(true)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.searchExpanded)
        assertTrue(listPreferences.searchExpanded.value)

        val restarted = newViewModel()
        advanceUntilIdle()

        assertTrue(restarted.uiState.value.searchExpanded)
    }

    @Test
    fun `collapsing search clears the query`() = runTest(testDispatcher) {
        loadFixtures()

        viewModel.onSearchExpandedChanged(true)
        viewModel.onSearchQueryChanged("mlk")
        advanceUntilIdle()
        assertEquals(listOf("Milk"), viewModel.uiState.value.filteredItems.map { it.name })

        viewModel.onSearchExpandedChanged(false)
        advanceUntilIdle()

        assertEquals("", viewModel.uiState.value.searchQuery)
        assertTrue(viewModel.uiState.value.filteredItems.map { it.name }.containsAll(listOf("Milk", "Bread")))
    }

    @Test
    fun `query is never persisted across restart`() = runTest(testDispatcher) {
        loadFixtures()

        viewModel.onSearchExpandedChanged(true)
        viewModel.onSearchQueryChanged("mlk")
        advanceUntilIdle()

        val restarted = newViewModel()
        advanceUntilIdle()

        assertTrue(restarted.uiState.value.searchExpanded)
        assertEquals("", restarted.uiState.value.searchQuery)
    }

    @Test
    fun `showChecked defaults to true and persists`() = runTest(testDispatcher) {
        loadFixtures()

        assertTrue(viewModel.uiState.value.showChecked)

        viewModel.onShowCheckedChanged(false)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.showChecked)
        assertFalse(listPreferences.showChecked.value)

        val restarted = newViewModel()
        advanceUntilIdle()

        assertFalse(restarted.uiState.value.showChecked)
    }

    @Test
    fun `hiding excludes checked items but search override keeps matches visible`() = runTest(testDispatcher) {
        enqueueItemList(
            itemJson(10, "Milk", isChecked = false, visibleIn = "[1]"),
            itemJson(11, "Bread", isChecked = true, visibleIn = "[1]"),
        )
        enqueueShopList(shopJson(1, "Global"), shopJson(2, "Auchan"))
        viewModel.refresh()
        awaitUntil { shopsRepository.shops.value.isNotEmpty() && itemsRepository.items.value.isNotEmpty() }
        server.takeRequest(5, TimeUnit.SECONDS)
        server.takeRequest(5, TimeUnit.SECONDS)

        viewModel.onSortOrderChanged(SortOrder.ALPHABETICAL)
        viewModel.onShowCheckedChanged(false)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.hideChecked)
        assertEquals(listOf("Bread", "Milk"), viewModel.uiState.value.filteredItems.map { it.name })

        viewModel.onSearchQueryChanged("bre")
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.hideChecked)
        assertEquals(listOf("Bread"), viewModel.uiState.value.filteredItems.map { it.name })

        viewModel.onSearchQueryChanged("")
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.hideChecked)
        assertEquals(listOf("Bread", "Milk"), viewModel.uiState.value.filteredItems.map { it.name })
    }

    @Test
    fun `manual sort engagement reveals all items regardless of hide toggle`() = runTest(testDispatcher) {
        enqueueItemList(
            itemJson(10, "Milk", isChecked = false, visibleIn = "[1]"),
            itemJson(11, "Bread", isChecked = true, visibleIn = "[1]"),
        )
        enqueueShopList(shopJson(1, "Global"), shopJson(2, "Auchan"))
        viewModel.refresh()
        awaitUntil { shopsRepository.shops.value.isNotEmpty() && itemsRepository.items.value.isNotEmpty() }
        server.takeRequest(5, TimeUnit.SECONDS)
        server.takeRequest(5, TimeUnit.SECONDS)

        viewModel.onShowCheckedChanged(false)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.hideChecked)
        assertEquals(listOf("Milk", "Bread"), viewModel.uiState.value.filteredItems.map { it.name })

        viewModel.onSortOrderChanged(SortOrder.ALPHABETICAL)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.hideChecked)
        assertEquals(listOf("Bread", "Milk"), viewModel.uiState.value.filteredItems.map { it.name })

        viewModel.onSortOrderChanged(SortOrder.MANUAL)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.hideChecked)
        assertEquals(listOf("Milk", "Bread"), viewModel.uiState.value.filteredItems.map { it.name })
    }

    @Test
    fun `alphabetical order re-orders items case-insensitively`() = runTest(testDispatcher) {
        loadFixtures()

        viewModel.onSortOrderChanged(SortOrder.ALPHABETICAL)
        advanceUntilIdle()

        assertEquals(listOf("Bread", "Milk"), viewModel.uiState.value.items.map { it.name })
    }

    @Test
    fun `frequency order re-orders live when a check count changes`() = runTest(testDispatcher) {
        enqueueItemList(
            itemJson(10, "Milk", orderKey = "a0", checkCount = 5),
            itemJson(11, "Bread", orderKey = "a1", checkCount = 2),
        )
        enqueueShopList(shopJson(1, "Global"), shopJson(2, "Auchan"))
        viewModel.refresh()
        awaitUntil { shopsRepository.shops.value.isNotEmpty() && itemsRepository.items.value.isNotEmpty() }
        server.takeRequest(5, TimeUnit.SECONDS)
        server.takeRequest(5, TimeUnit.SECONDS)

        viewModel.onSortOrderChanged(SortOrder.FREQUENCY)
        advanceUntilIdle()
        assertEquals(listOf("Milk", "Bread"), viewModel.uiState.value.items.map { it.name })

        server.enqueue(
            MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(itemJson(id = 11, name = "Bread", isChecked = true, checkCount = 7)),
        )

        viewModel.onCheckToggled(viewModel.uiState.value.items.first { it.id == 11L })
        awaitUntil { viewModel.uiState.value.items.first { it.id == 11L }.checkCount == 7 }

        assertEquals(listOf("Bread", "Milk"), viewModel.uiState.value.items.map { it.name })
    }

    @Test
    fun `switching back to manual restores the exact stored order`() = runTest(testDispatcher) {
        enqueueItemList(
            itemJson(10, "Milk", orderKey = "a0", checkCount = 5),
            itemJson(11, "Bread", orderKey = "a1", checkCount = 2),
            itemJson(12, "Eggs", orderKey = "a2", checkCount = 9),
        )
        enqueueShopList(shopJson(1, "Global"), shopJson(2, "Auchan"))
        viewModel.refresh()
        awaitUntil { shopsRepository.shops.value.isNotEmpty() && itemsRepository.items.value.isNotEmpty() }
        server.takeRequest(5, TimeUnit.SECONDS)
        server.takeRequest(5, TimeUnit.SECONDS)

        viewModel.onSortOrderChanged(SortOrder.FREQUENCY)
        advanceUntilIdle()
        assertEquals(listOf("Eggs", "Milk", "Bread"), viewModel.uiState.value.items.map { it.name })

        viewModel.onSortOrderChanged(SortOrder.MANUAL)
        advanceUntilIdle()
        assertEquals(listOf("Milk", "Bread", "Eggs"), viewModel.uiState.value.items.map { it.name })
    }

    @Test
    fun `sort mode and order persist across restart`() = runTest(testDispatcher) {
        loadFixtures()

        viewModel.onSortModeToggled()
        viewModel.onSortOrderChanged(SortOrder.FREQUENCY)
        advanceUntilIdle()

        assertFalse(listPreferences.sortModeEnabled.value)
        assertEquals(SortOrder.FREQUENCY, listPreferences.sortOrder.value)

        val restarted = newViewModel()
        advanceUntilIdle()

        assertFalse(restarted.uiState.value.sortModeEnabled)
        assertEquals(SortOrder.FREQUENCY, restarted.uiState.value.sortOrder)
    }

    @Test
    fun `drag gating matrix - enabled only in manual mode with sort on and no overlays`() =
        runTest(testDispatcher) {
            loadFixtures()

            assertTrue(viewModel.isDragEnabled())

            viewModel.onSortModeToggled()
            advanceUntilIdle()
            assertFalse(viewModel.isDragEnabled())

            viewModel.onSortModeToggled()
            advanceUntilIdle()
            assertTrue(viewModel.isDragEnabled())

            viewModel.onSortOrderChanged(SortOrder.ALPHABETICAL)
            advanceUntilIdle()
            assertFalse(viewModel.isDragEnabled())

            viewModel.onSortOrderChanged(SortOrder.MANUAL)
            advanceUntilIdle()
            assertTrue(viewModel.isDragEnabled())

            viewModel.onAddAffordanceTapped()
            advanceUntilIdle()
            assertFalse(viewModel.isDragEnabled())

            viewModel.onAddItemDiscard()
            advanceUntilIdle()
            assertTrue(viewModel.isDragEnabled())

            viewModel.onAddShopTapped()
            advanceUntilIdle()
            assertFalse(viewModel.isDragEnabled())

            viewModel.onShopDiscard()
            advanceUntilIdle()
            assertTrue(viewModel.isDragEnabled())
        }

    @Test
    fun `drag disabled while search query is active`() = runTest(testDispatcher) {
        loadFixtures()

        viewModel.onSearchQueryChanged("mlk")
        advanceUntilIdle()

        assertFalse(viewModel.isDragEnabled())

        viewModel.onSearchQueryChanged("")
        advanceUntilIdle()

        assertTrue(viewModel.isDragEnabled())
    }

    @Test
    fun `drop with active search query does not reorder`() = runTest(testDispatcher) {
        loadFixtures()

        viewModel.onSearchQueryChanged("mlk")
        advanceUntilIdle()

        viewModel.onItemDropped(viewModel.uiState.value.items.first { it.id == 10L }, dropIndex = 0)
        advanceUntilIdle()

        assertEquals(2, server.requestCount)
    }

    @Test
    fun `drop in non-manual order does not reorder`() = runTest(testDispatcher) {
        loadFixtures()
        viewModel.onSortOrderChanged(SortOrder.ALPHABETICAL)
        advanceUntilIdle()

        viewModel.onItemDropped(viewModel.uiState.value.items.first { it.id == 10L }, dropIndex = 1)
        advanceUntilIdle()

        assertEquals(2, server.requestCount)
    }

    @Test
    fun `valid manual drop sends reorder with fractional key and updates cache`() = runTest(testDispatcher) {
        loadFixtures()
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(itemJson(id = 10, name = "Milk", orderKey = "a2")),
        )

        viewModel.onItemDropped(viewModel.uiState.value.items.first { it.id == 10L }, dropIndex = 1)
        awaitUntil { server.requestCount == 3 }

        val recorded = server.takeRequest(5, TimeUnit.SECONDS)!!
        assertEquals("PATCH", recorded.method)
        assertEquals("/api/v1/items/10", recorded.path)
        val body = recorded.body.readUtf8()
        assertTrue(body.contains("\"orderMap\""))
        assertTrue(body.contains("\"1\":\"a2\""))
        awaitUntil { viewModel.uiState.value.items.first { it.id == 10L }.orderMap["1"] == "a2" }
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `drop past keyed tail clamps to end of keyed list`() = runTest(testDispatcher) {
        enqueueItemList(
            itemJson(10, "Milk", orderKey = "a0"),
            itemJson(11, "Bread", orderKey = "a1"),
            itemJson(12, "Eggs", orderKey = null),
        )
        enqueueShopList(shopJson(1, "Global"), shopJson(2, "Auchan"))
        viewModel.refresh()
        awaitUntil { shopsRepository.shops.value.isNotEmpty() && itemsRepository.items.value.isNotEmpty() }
        server.takeRequest(5, TimeUnit.SECONDS)
        server.takeRequest(5, TimeUnit.SECONDS)

        server.enqueue(
            MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(itemJson(id = 10, name = "Milk", orderKey = "a2")),
        )

        viewModel.onItemDropped(viewModel.uiState.value.items.first { it.id == 10L }, dropIndex = 2)
        awaitUntil { server.requestCount == 3 }

        val recorded = server.takeRequest(5, TimeUnit.SECONDS)!!
        val body = recorded.body.readUtf8()
        assertTrue(body.contains("\"1\":\"a2\""))
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `failed reorder shows error message`() = runTest(testDispatcher) {
        loadFixtures()
        server.enqueue(MockResponse().setResponseCode(500).setHeader("Content-Type", "application/json").setBody("{\"error\":\"boom\"}"))

        viewModel.onItemDropped(viewModel.uiState.value.items.first { it.id == 10L }, dropIndex = 1)
        awaitUntil { viewModel.uiState.value.errorMessage != null }

        assertEquals("Couldn't move the item", viewModel.uiState.value.errorMessage)
        assertEquals("a0", viewModel.uiState.value.items.first { it.id == 10L }.orderMap["1"])
    }

    @Test
    fun `add modal quantity and temporary intents update state`() = runTest(testDispatcher) {
        loadFixtures()
        viewModel.onAddAffordanceTapped()
        awaitUntil { viewModel.uiState.value.addItemModal != null }

        viewModel.onAddItemQuantityChanged(3)
        awaitUntil { viewModel.uiState.value.addItemModal?.quantity == 3 }

        viewModel.onAddItemTemporaryToggled()
        awaitUntil { viewModel.uiState.value.addItemModal?.isTemporary == true }

        viewModel.onAddItemQuantityChanged(1)
        viewModel.onAddItemTemporaryToggled()
        awaitUntil { viewModel.uiState.value.addItemModal?.quantity == 1 }
        assertFalse(viewModel.uiState.value.addItemModal?.isTemporary ?: true)
    }

    @Test
    fun `add save sends quantity and temporary fields`() = runTest(testDispatcher) {
        loadFixtures()
        viewModel.onSearchQueryChanged("Eggs")
        advanceUntilIdle()
        viewModel.onAddAffordanceTapped()
        advanceUntilIdle()
        viewModel.onAddItemQuantityChanged(3)
        awaitUntil { viewModel.uiState.value.addItemModal?.quantity == 3 }
        viewModel.onAddItemTemporaryToggled()
        awaitUntil { viewModel.uiState.value.addItemModal?.isTemporary == true }
        server.enqueue(
            MockResponse().setResponseCode(201)
                .setHeader("Content-Type", "application/json")
                .setBody(itemJson(id = 12, name = "Eggs")),
        )

        viewModel.onAddItemSave()
        awaitUntil { server.requestCount == 3 }

        val recorded = server.takeRequest(5, TimeUnit.SECONDS)
        val body = recorded!!.body.readUtf8()
        assertTrue(body.contains("\"quantity\":3"))
        assertTrue(body.contains("\"isTemporary\":true"))
    }

    @Test
    fun `edit modal quantity and temporary intents update state`() = runTest(testDispatcher) {
        loadFixtures()
        viewModel.onItemLongPressed(viewModel.uiState.value.items.first { it.id == 11L })
        awaitUntil { viewModel.uiState.value.editItemModal != null }

        viewModel.onEditItemQuantityChanged(4)
        awaitUntil { viewModel.uiState.value.editItemModal?.quantity == 4 }

        viewModel.onEditItemTemporaryToggled()
        awaitUntil { viewModel.uiState.value.editItemModal?.isTemporary == true }

        assertTrue(viewModel.uiState.value.editItemModal?.canSave ?: false)
    }

    @Test
    fun `edit save patches quantity and temporary only when changed`() = runTest(testDispatcher) {
        loadFixtures()
        viewModel.onItemLongPressed(viewModel.uiState.value.items.first { it.id == 11L })
        awaitUntil { viewModel.uiState.value.editItemModal != null }
        viewModel.onEditItemQuantityChanged(4)
        awaitUntil { viewModel.uiState.value.editItemModal?.quantity == 4 }
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(itemJson(id = 11, name = "Bread")),
        )

        viewModel.onEditItemSave()
        awaitUntil { server.requestCount == 3 }

        val recorded = server.takeRequest(5, TimeUnit.SECONDS)
        val body = recorded!!.body.readUtf8()
        assertTrue(body.contains("\"quantity\":4"))
        assertFalse(body.contains("\"isTemporary\""))
        awaitUntil { viewModel.uiState.value.editItemModal == null }
    }

    @Test
    fun `expired temporary item is hidden from the list`() = runTest(testDispatcher) {
        val past = Instant.now().minusSeconds(60).toString()
        enqueueItemList(
            itemJson(10, "Milk", orderKey = "a0"),
            itemJson(11, "Yogurt", orderKey = "a1", isTemporary = true, isChecked = true, expiresAt = past),
        )
        enqueueShopList(shopJson(1, "Global"), shopJson(2, "Auchan"))
        viewModel.refresh()
        awaitUntil { itemsRepository.items.value.isNotEmpty() }
        advanceUntilIdle()

        val names = viewModel.uiState.value.items.map { it.name }
        assertEquals(listOf("Milk"), names)
    }

    @Test
    fun `unexpired temporary item stays visible`() = runTest(testDispatcher) {
        val future = Instant.now().plusSeconds(3600).toString()
        enqueueItemList(
            itemJson(10, "Milk", orderKey = "a0"),
            itemJson(11, "Yogurt", orderKey = "a1", isTemporary = true, isChecked = true, expiresAt = future),
        )
        enqueueShopList(shopJson(1, "Global"), shopJson(2, "Auchan"))
        viewModel.refresh()
        awaitUntil { itemsRepository.items.value.isNotEmpty() }
        advanceUntilIdle()

        assertEquals(listOf("Milk", "Yogurt"), viewModel.uiState.value.items.map { it.name })
    }

    @Test
    fun `permanent item with stale expiry stays visible`() = runTest(testDispatcher) {
        val past = Instant.now().minusSeconds(60).toString()
        enqueueItemList(
            itemJson(10, "Milk", orderKey = "a0"),
            itemJson(11, "Yogurt", orderKey = "a1", isTemporary = false, expiresAt = past),
        )
        enqueueShopList(shopJson(1, "Global"), shopJson(2, "Auchan"))
        viewModel.refresh()
        awaitUntil { itemsRepository.items.value.isNotEmpty() }
        advanceUntilIdle()

        assertEquals(listOf("Milk", "Yogurt"), viewModel.uiState.value.items.map { it.name })
    }

    @Test
    fun `expiry tick hides an item whose expiry passes mid-session`() = runTest(testDispatcher) {
        val soon = Instant.now().plusSeconds(60).toString()
        enqueueItemList(
            itemJson(10, "Milk", orderKey = "a0"),
            itemJson(11, "Yogurt", orderKey = "a1", isTemporary = true, isChecked = true, expiresAt = soon),
        )
        enqueueShopList(shopJson(1, "Global"), shopJson(2, "Auchan"))
        viewModel.refresh()
        awaitUntil { itemsRepository.items.value.isNotEmpty() }
        advanceUntilIdle()
        assertEquals(listOf("Milk", "Yogurt"), viewModel.uiState.value.items.map { it.name })

        expiryTick.value = Instant.now().plusSeconds(60).toEpochMilli()
        advanceUntilIdle()

        assertEquals(listOf("Milk"), viewModel.uiState.value.items.map { it.name })
    }

    @Test
    fun `remote label batch shows first toast and queues the rest`() = runTest(testDispatcher) {
        loadFixtures()

        fakeSyncManager.emit(listOf("checked: Milk", "Milk → Bread"))
        runCurrent()

        assertEquals("checked: Milk", viewModel.changeToast.value?.text)

        viewModel.onToastSwipedDismissed(viewModel.changeToast.value?.id ?: -1L)
        runCurrent()

        assertEquals("Milk → Bread", viewModel.changeToast.value?.text)
    }

    @Test
    fun `new batch queues behind the visible toast and shows after dismissal`() = runTest(testDispatcher) {
        loadFixtures()

        fakeSyncManager.emit(listOf("checked: Milk"))
        runCurrent()
        val firstToast = viewModel.changeToast.value
        assertEquals("checked: Milk", firstToast?.text)

        fakeSyncManager.emit(listOf("new item: Bread"))
        runCurrent()

        assertEquals("checked: Milk", viewModel.changeToast.value?.text)

        viewModel.onToastSwipedDismissed(firstToast!!.id)
        runCurrent()

        val queuedToast = viewModel.changeToast.value
        assertEquals("new item: Bread", queuedToast?.text)
        assertTrue(queuedToast!!.id != firstToast.id)
    }

    @Test
    fun `toast auto-dismisses after the timeout`() = runTest(testDispatcher) {
        loadFixtures()

        fakeSyncManager.emit(listOf("checked: Milk"))
        runCurrent()
        assertEquals("checked: Milk", viewModel.changeToast.value?.text)

        testDispatcher.scheduler.advanceTimeBy(ListViewModel.TOAST_AUTO_DISMISS_MS)
        runCurrent()

        assertNull(viewModel.changeToast.value)
    }

    @Test
    fun `disabled preference suppresses toast display`() = runTest(testDispatcher) {
        loadFixtures()

        themeRepository.setChangeToastsEnabled(false)
        advanceUntilIdle()

        fakeSyncManager.emit(listOf("checked: Milk"))
        runCurrent()

        assertNull(viewModel.changeToast.value)
    }

    @Test
    fun `re-enabling the preference resumes toast display`() = runTest(testDispatcher) {
        loadFixtures()

        themeRepository.setChangeToastsEnabled(false)
        advanceUntilIdle()
        fakeSyncManager.emit(listOf("checked: Milk"))
        runCurrent()
        assertNull(viewModel.changeToast.value)

        themeRepository.setChangeToastsEnabled(true)
        advanceUntilIdle()
        fakeSyncManager.emit(listOf("new item: Bread"))
        runCurrent()

        assertEquals("new item: Bread", viewModel.changeToast.value?.text)
    }
}

private class FakeRealtimeSyncManager : RealtimeSyncManagerContract {
    private val labels = MutableSharedFlow<List<ChangeToastLabel>>(extraBufferCapacity = 64)

    override val changeToastLabels: SharedFlow<List<ChangeToastLabel>> = labels

    suspend fun emit(batch: List<String>) {
        labels.emit(batch.map { ChangeToastLabel(it) })
    }
}

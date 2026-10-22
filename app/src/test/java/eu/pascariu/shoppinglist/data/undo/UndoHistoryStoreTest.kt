package eu.pascariu.shoppinglist.data.undo

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class UndoHistoryStoreTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var dataStoreFile: File
    private lateinit var storeScope: CoroutineScope
    private lateinit var dataStore: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>

    private val json = Json

    @Before
    fun setUp() {
        dataStoreFile = File(tmpFolder.root, "undo_history_test.preferences_pb")
        storeScope = CoroutineScope(testDispatcher + Job())
        dataStore = PreferenceDataStoreFactory.create(scope = storeScope) {
            dataStoreFile
        }
    }

    @After
    fun tearDown() {
        storeScope.cancel()
    }

    private fun createStore(): UndoHistoryStore =
        UndoHistoryStore(
            dataStore = dataStore,
            json = json,
            storeScope = storeScope,
        )

    @Test
    fun `record adds entry on top`() = runTest(testDispatcher) {
        val store = createStore()
        val now = System.currentTimeMillis()
        store.record(UndoEntry(1, from = false, to = true, at = now))
        advanceUntilIdle()

        assertEquals(listOf(UndoEntry(1, false, true, now)), store.entries.value)
    }

    @Test
    fun `record evicts oldest beyond 50 entries`() = runTest(testDispatcher) {
        val store = createStore()
        val now = System.currentTimeMillis()
        repeat(50) { index ->
            store.record(UndoEntry(index.toLong() + 1, from = false, to = true, at = now + index))
        }
        store.record(UndoEntry(999, from = false, to = true, at = now + 9999))
        advanceUntilIdle()

        val entries = store.entries.value
        assertEquals(50, entries.size)
        assertEquals(999L, entries.first().itemId)
        assertEquals(2L, entries.last().itemId)
    }

    @Test
    fun `prune removes expired entries`() = runTest(testDispatcher) {
        val store = createStore()
        val now = System.currentTimeMillis()
        store.record(UndoEntry(1, from = false, to = true, at = now - UndoConfig.TTL.toMillis() - 1))
        store.record(UndoEntry(2, from = false, to = true, at = now))
        advanceUntilIdle()

        store.prune()
        advanceUntilIdle()

        assertEquals(listOf(UndoEntry(2, false, true, now)), store.entries.value)
    }

    @Test
    fun `removeTop returns and drops top entry`() = runTest(testDispatcher) {
        val store = createStore()
        val now = System.currentTimeMillis()
        store.record(UndoEntry(1, from = false, to = true, at = now))
        store.record(UndoEntry(2, from = false, to = true, at = now + 1))
        advanceUntilIdle()

        val top = store.removeTop()
        advanceUntilIdle()

        assertEquals(UndoEntry(2, false, true, now + 1), top)
        assertEquals(listOf(UndoEntry(1, false, true, now)), store.entries.value)
    }

    @Test
    fun `removeTop on empty history returns null`() = runTest(testDispatcher) {
        val store = createStore()
        advanceUntilIdle()

        assertNull(store.removeTop())
    }

    @Test
    fun `restoreTop puts entry back on top`() = runTest(testDispatcher) {
        val store = createStore()
        val now = System.currentTimeMillis()
        val entry = UndoEntry(1, from = false, to = true, at = now)
        store.record(entry)
        advanceUntilIdle()
        store.removeTop()
        advanceUntilIdle()

        store.restoreTop(entry)
        advanceUntilIdle()

        assertEquals(listOf(entry), store.entries.value)
    }

    @Test
    fun `history is persisted and restored from datastore file`() = runTest(testDispatcher) {
        val store = createStore()
        val now = System.currentTimeMillis()
        val entry = UndoEntry(42, from = false, to = true, at = now)
        store.record(entry)
        advanceUntilIdle()

        storeScope.cancel()
        val secondScope = CoroutineScope(testDispatcher + Job())
        val secondDataStore = PreferenceDataStoreFactory.create(scope = secondScope) {
            dataStoreFile
        }
        val secondStore = UndoHistoryStore(
            dataStore = secondDataStore,
            json = json,
            storeScope = secondScope,
        )
        advanceUntilIdle()

        assertEquals(listOf(entry), secondStore.entries.value)
        secondScope.cancel()
    }

    @Test
    fun `corrupt persisted history restores as empty`() = runTest(testDispatcher) {
        dataStoreFile.writeBytes(byteArrayOf(0x00, 0x01, 0x02))
        val store = createStore()
        advanceUntilIdle()

        assertTrue(store.entries.value.isEmpty())
    }

    @Test
    fun `concurrent records all land in history`() = runTest(testDispatcher) {
        val store = createStore()
        val now = System.currentTimeMillis()

        val jobs = (1..20).map { index ->
            launch {
                store.record(UndoEntry(index.toLong(), from = false, to = true, at = now + index))
            }
        }
        jobs.joinAll()
        advanceUntilIdle()

        assertEquals(20, store.entries.value.size)
    }

    @Test
    fun `concurrent record and removeTop do not lose entries`() = runTest(testDispatcher) {
        val store = createStore()
        val now = System.currentTimeMillis()
        repeat(20) { index ->
            store.record(UndoEntry(index.toLong() + 1, from = false, to = true, at = now + index))
        }
        advanceUntilIdle()

        val writers = List(10) { writer ->
            launch {
                repeat(5) { attempt ->
                    store.record(
                        UndoEntry(
                            itemId = 100L + writer * 10 + attempt,
                            from = false,
                            to = true,
                            at = now + 1000 + writer * 10 + attempt,
                        ),
                    )
                }
            }
        }
        val removers = List(10) {
            launch {
                repeat(5) {
                    store.removeTop()
                }
            }
        }
        writers.joinAll()
        removers.joinAll()
        advanceUntilIdle()

        assertEquals(20, store.entries.value.size)
    }
}

package eu.pascariu.shoppinglist.data.prefs

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.TestScope
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class ListPreferencesDataSourceTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var storeScope: CoroutineScope
    private lateinit var dataSource: ListPreferencesDataSource

    @Before
    fun setUp() {
        val storeDispatcher = StandardTestDispatcher(testDispatcher.scheduler)
        storeScope = CoroutineScope(storeDispatcher + Job())
        dataSource = ListPreferencesDataSource(
            dataStore = PreferenceDataStoreFactory.create(scope = storeScope) {
                File(tmpFolder.root, "list_prefs_test.preferences_pb")
            },
            storeScope = storeScope,
            ioDispatcher = storeDispatcher,
        )
    }

    @After
    fun tearDown() {
        storeScope.cancel()
    }

    private suspend fun TestScope.settle() {
        advanceUntilIdle()
    }

    @Test
    fun `widget stack expanded defaults to false`() = runTest(testDispatcher) {
        settle()

        assertFalse(dataSource.widgetStackExpanded.value)
    }

    @Test
    fun `widget stack expanded round-trips through storage`() = runTest(testDispatcher) {
        settle()

        dataSource.setWidgetStackExpanded(true)
        settle()

        assertTrue(dataSource.widgetStackExpanded.value)
    }

    @Test
    fun `active shop id defaults to null`() = runTest(testDispatcher) {
        settle()

        assertNull(dataSource.activeShopId.value)
    }

    @Test
    fun `active shop id round-trips through storage`() = runTest(testDispatcher) {
        settle()

        dataSource.setActiveShopId(42L)
        settle()

        assertEquals(42L, dataSource.activeShopId.value)
    }

    @Test
    fun `active shop id can be cleared`() = runTest(testDispatcher) {
        settle()

        dataSource.setActiveShopId(42L)
        settle()
        dataSource.setActiveShopId(null)
        settle()

        assertNull(dataSource.activeShopId.value)
    }

    @Test
    fun `search expanded defaults to false`() = runTest(testDispatcher) {
        settle()

        assertFalse(dataSource.searchExpanded.value)
    }

    @Test
    fun `search expanded round-trips through storage`() = runTest(testDispatcher) {
        settle()

        dataSource.setSearchExpanded(true)
        settle()

        assertTrue(dataSource.searchExpanded.value)
    }

    @Test
    fun `show checked defaults to true`() = runTest(testDispatcher) {
        settle()

        assertTrue(dataSource.showChecked.value)
    }

    @Test
    fun `show checked round-trips through storage`() = runTest(testDispatcher) {
        settle()

        dataSource.setShowChecked(false)
        settle()

        assertFalse(dataSource.showChecked.value)
    }

    @Test
    fun `sort mode enabled defaults to true`() = runTest(testDispatcher) {
        settle()

        assertTrue(dataSource.sortModeEnabled.value)
    }

    @Test
    fun `sort mode enabled round-trips through storage`() = runTest(testDispatcher) {
        settle()

        dataSource.setSortModeEnabled(false)
        settle()

        assertFalse(dataSource.sortModeEnabled.value)
    }

    @Test
    fun `sort order defaults to manual`() = runTest(testDispatcher) {
        settle()

        assertEquals(SortOrder.MANUAL, dataSource.sortOrder.value)
    }

    @Test
    fun `sort order round-trips through storage`() = runTest(testDispatcher) {
        settle()

        dataSource.setSortOrder(SortOrder.FREQUENCY)
        settle()

        assertEquals(SortOrder.FREQUENCY, dataSource.sortOrder.value)
    }

    @Test
    fun `change toasts enabled defaults to true`() = runTest(testDispatcher) {
        settle()

        assertTrue(dataSource.changeToastsEnabled.value)
    }

    @Test
    fun `change toasts enabled round-trips through storage`() = runTest(testDispatcher) {
        settle()

        dataSource.setChangeToastsEnabled(false)
        settle()

        assertFalse(dataSource.changeToastsEnabled.value)
    }

    @Test
    fun `keys are isolated - writing one does not affect the other`() = runTest(testDispatcher) {
        settle()

        dataSource.setWidgetStackExpanded(true)
        settle()
        dataSource.setActiveShopId(7L)
        settle()

        assertTrue(dataSource.widgetStackExpanded.value)
        assertEquals(7L, dataSource.activeShopId.value)

        dataSource.setActiveShopId(null)
        settle()

        assertTrue(dataSource.widgetStackExpanded.value)
        assertNull(dataSource.activeShopId.value)
    }

    @Test
    fun `state survives restart simulation with a fresh data source on the same file`() =
        runTest(testDispatcher) {
            settle()

            dataSource.setWidgetStackExpanded(true)
            dataSource.setActiveShopId(9L)
            dataSource.setSearchExpanded(true)
            dataSource.setShowChecked(false)
            dataSource.setSortModeEnabled(false)
            dataSource.setSortOrder(SortOrder.ALPHABETICAL)
            dataSource.setChangeToastsEnabled(false)
            settle()

            storeScope.cancel()

            val restartedScope = CoroutineScope(StandardTestDispatcher(testDispatcher.scheduler) + Job())
            val restarted = ListPreferencesDataSource(
                dataStore = PreferenceDataStoreFactory.create(scope = restartedScope) {
                    File(tmpFolder.root, "list_prefs_test.preferences_pb")
                },
                storeScope = restartedScope,
                ioDispatcher = StandardTestDispatcher(testDispatcher.scheduler),
            )
            advanceUntilIdle()

            assertTrue(restarted.widgetStackExpanded.value)
            assertEquals(9L, restarted.activeShopId.value)
            assertTrue(restarted.searchExpanded.value)
            assertFalse(restarted.showChecked.value)
            assertFalse(restarted.sortModeEnabled.value)
            assertEquals(SortOrder.ALPHABETICAL, restarted.sortOrder.value)
            assertFalse(restarted.changeToastsEnabled.value)
        }

    @Test
    fun `setter reverts in-memory state when persistence fails`() =
        runTest(testDispatcher) {
            val unreadableFile = File(tmpFolder.root, "unreadable.preferences_pb")
            unreadableFile.mkdirs()
            val failingScope = CoroutineScope(StandardTestDispatcher(testDispatcher.scheduler) + Job())
            val failing = ListPreferencesDataSource(
                dataStore = PreferenceDataStoreFactory.create(scope = failingScope) { unreadableFile },
                storeScope = failingScope,
                ioDispatcher = StandardTestDispatcher(testDispatcher.scheduler),
            )
            advanceUntilIdle()

            failing.setWidgetStackExpanded(true)
            failing.setActiveShopId(3L)

            assertFalse(failing.widgetStackExpanded.value)
            assertNull(failing.activeShopId.value)

            failingScope.cancel()
        }

    @Test
    fun `concurrent setters do not revert each other`() = runTest(testDispatcher) {
        settle()

        coroutineScope {
            launch { dataSource.setSortModeEnabled(false) }
            launch { dataSource.setSortOrder(SortOrder.FREQUENCY) }
        }
        settle()

        assertFalse(dataSource.sortModeEnabled.value)
        assertEquals(SortOrder.FREQUENCY, dataSource.sortOrder.value)
    }
}

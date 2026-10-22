package eu.pascariu.shoppinglist.data.prefs

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
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
            settle()

            storeScope.cancel()

            val restarted = ListPreferencesDataSource(
                dataStore = PreferenceDataStoreFactory.create(
                    scope = CoroutineScope(StandardTestDispatcher(testDispatcher.scheduler) + Job())
                ) {
                    File(tmpFolder.root, "list_prefs_test.preferences_pb")
                },
                ioDispatcher = StandardTestDispatcher(testDispatcher.scheduler),
            )
            advanceUntilIdle()

            assertTrue(restarted.widgetStackExpanded.value)
            assertEquals(9L, restarted.activeShopId.value)
        }
}

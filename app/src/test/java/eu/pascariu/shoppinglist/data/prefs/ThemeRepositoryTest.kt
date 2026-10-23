package eu.pascariu.shoppinglist.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import eu.pascariu.shoppinglist.ui.theme.ThemeId
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class ThemeRepositoryTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var storeScope: CoroutineScope
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: ThemeRepository

    @Before
    fun setUp() {
        val storeDispatcher = StandardTestDispatcher(testDispatcher.scheduler)
        storeScope = CoroutineScope(storeDispatcher + Job())
        dataStore = PreferenceDataStoreFactory.create(scope = storeScope) {
            File(tmpFolder.root, "theme_prefs_test.preferences_pb")
        }
        repository = ThemeRepository(
            dataStore = dataStore,
            storeScope = storeScope,
            ioDispatcher = storeDispatcher,
        )
    }

    @After
    fun tearDown() {
        storeScope.cancel()
    }

    @Test
    fun `theme defaults to DEFAULT`() = runTest(testDispatcher) {
        advanceUntilIdle()

        assertEquals(ThemeId.DEFAULT, repository.theme.value)
    }

    @Test
    fun `selection persists through storage`() = runTest(testDispatcher) {
        advanceUntilIdle()

        repository.setTheme(ThemeId.MINT)
        advanceUntilIdle()

        assertEquals(ThemeId.MINT, repository.theme.value)
    }

    @Test
    fun `selection can be reset to DEFAULT`() = runTest(testDispatcher) {
        advanceUntilIdle()

        repository.setTheme(ThemeId.OCEAN)
        advanceUntilIdle()
        repository.setTheme(ThemeId.DEFAULT)
        advanceUntilIdle()

        assertEquals(ThemeId.DEFAULT, repository.theme.value)
    }

    @Test
    fun `unknown stored value falls back to DEFAULT`() = runTest(testDispatcher) {
        dataStore.edit { prefs -> prefs[ThemeRepository.KEY_THEME] = "not-a-theme" }
        val lateRepository = ThemeRepository(
            dataStore = dataStore,
            storeScope = storeScope,
            ioDispatcher = StandardTestDispatcher(testDispatcher.scheduler),
        )
        advanceUntilIdle()

        assertEquals(ThemeId.DEFAULT, lateRepository.theme.value)
    }
}

package eu.pascariu.shoppinglist.ui.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import eu.pascariu.shoppinglist.data.prefs.ThemeRepository
import eu.pascariu.shoppinglist.ui.theme.ThemeId
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var storeScope: CoroutineScope
    private lateinit var repository: ThemeRepository
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val storeDispatcher = StandardTestDispatcher(testDispatcher.scheduler)
        storeScope = CoroutineScope(storeDispatcher + Job())
        repository = ThemeRepository(
            dataStore = PreferenceDataStoreFactory.create(scope = storeScope) {
                File(tmpFolder.root, "settings_vm_test.preferences_pb")
            },
            storeScope = storeScope,
            ioDispatcher = storeDispatcher,
        )
        viewModel = SettingsViewModel(repository)
    }

    @After
    fun tearDown() {
        storeScope.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun `theme selection delegates to repository`() = runTest(testDispatcher) {
        advanceUntilIdle()

        viewModel.onThemeSelected(ThemeId.SUNSET)
        advanceUntilIdle()

        assertEquals(ThemeId.SUNSET, viewModel.theme.value)
    }

    @Test
    fun `toggle persists through repository`() = runTest(testDispatcher) {
        advanceUntilIdle()

        viewModel.onChangeToastsToggled(false)
        advanceUntilIdle()

        assertFalse(viewModel.changeToastsEnabled.value)
    }
}

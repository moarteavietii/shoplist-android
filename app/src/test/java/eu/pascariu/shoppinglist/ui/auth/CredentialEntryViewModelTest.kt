package eu.pascariu.shoppinglist.ui.auth

import eu.pascariu.shoppinglist.data.auth.AccessCredentials
import eu.pascariu.shoppinglist.data.auth.TokenStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CredentialEntryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeTokenStore: FakeTokenStore

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeTokenStore = FakeTokenStore()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `submit saves trimmed credentials and marks state saved`() {
        val viewModel = CredentialEntryViewModel(fakeTokenStore)
        viewModel.onClientIdChange(" id-1 ")
        viewModel.onClientSecretChange(" secret-1 ")

        viewModel.submit()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(AccessCredentials("id-1", "secret-1"), fakeTokenStore.saved)
        assertTrue(viewModel.uiState.value.saved)
    }

    @Test
    fun `submit with blank fields shows error and does not save`() {
        val viewModel = CredentialEntryViewModel(fakeTokenStore)

        viewModel.submit()

        assertEquals("Both client id and secret are required", viewModel.uiState.value.error)
        assertEquals(null, fakeTokenStore.saved)
    }

    @Test
    fun `saved flag resets after handling so a reused screen does not close immediately`() {
        val viewModel = CredentialEntryViewModel(fakeTokenStore)
        viewModel.onClientIdChange("id-1")
        viewModel.onClientSecretChange("secret-1")
        viewModel.submit()
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.saved)

        viewModel.onSavedHandled()

        assertFalse(viewModel.uiState.value.saved)
    }

    @Test
    fun `saved flag can be set again after being handled`() {
        val viewModel = CredentialEntryViewModel(fakeTokenStore)
        viewModel.onClientIdChange("id-1")
        viewModel.onClientSecretChange("secret-1")
        viewModel.submit()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.onSavedHandled()

        viewModel.onClientIdChange("id-2")
        viewModel.onClientSecretChange("secret-2")
        viewModel.submit()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.saved)
        assertEquals(AccessCredentials("id-2", "secret-2"), fakeTokenStore.saved)
    }

    private class FakeTokenStore(var saved: AccessCredentials? = null) : TokenStore {
        override fun load(): AccessCredentials? = saved
        override fun save(credentials: AccessCredentials) {
            saved = credentials
        }
        override fun clear() {
            saved = null
        }
    }
}

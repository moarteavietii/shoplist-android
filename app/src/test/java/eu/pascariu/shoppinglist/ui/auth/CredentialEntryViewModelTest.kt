package eu.pascariu.shoppinglist.ui.auth

import eu.pascariu.shoppinglist.data.auth.AccessCredentials
import eu.pascariu.shoppinglist.data.auth.AuthEvent
import eu.pascariu.shoppinglist.data.auth.AuthEventBusImpl
import eu.pascariu.shoppinglist.data.auth.TokenStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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

    private val testDispatcher = UnconfinedTestDispatcher()
    private val authEventBus = AuthEventBusImpl()
    private val tokenStore = FakeTokenStore()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `submit saves credentials and marks state saved`() {
        val viewModel = CredentialEntryViewModel(tokenStore, authEventBus)

        viewModel.onClientIdChange("id-1")
        viewModel.onClientSecretChange("secret-1")
        viewModel.submit()

        assertEquals(AccessCredentials("id-1", "secret-1"), tokenStore.saved)
        assertTrue(viewModel.uiState.value.saved)
    }

    @Test
    fun `credentials rejected event resets saved flag`() {
        val viewModel = CredentialEntryViewModel(tokenStore, authEventBus)
        viewModel.onClientIdChange("id-1")
        viewModel.onClientSecretChange("secret-1")
        viewModel.submit()
        assertTrue(viewModel.uiState.value.saved)

        authEventBus.emit(AuthEvent.CredentialsRejected)

        assertFalse(viewModel.uiState.value.saved)
    }

    @Test
    fun `unrelated auth events do not reset saved flag`() {
        val viewModel = CredentialEntryViewModel(tokenStore, authEventBus)
        viewModel.onClientIdChange("id-1")
        viewModel.onClientSecretChange("secret-1")
        viewModel.submit()
        assertTrue(viewModel.uiState.value.saved)

        authEventBus.emit(AuthEvent.CredentialsRejected)

        assertEquals(AccessCredentials("id-1", "secret-1"), tokenStore.saved)
    }
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

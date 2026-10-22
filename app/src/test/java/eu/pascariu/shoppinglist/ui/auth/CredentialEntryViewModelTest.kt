package eu.pascariu.shoppinglist.ui.auth

import eu.pascariu.shoppinglist.data.auth.AccessCredentials
import eu.pascariu.shoppinglist.data.auth.AuthEvent
import eu.pascariu.shoppinglist.data.auth.AuthEventBusImpl
import eu.pascariu.shoppinglist.data.auth.TokenStore
import eu.pascariu.shoppinglist.di.IoDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
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
    private val ioDispatcher: TestDispatcher = StandardTestDispatcher(testDispatcher.scheduler)
    private val authEventBus = AuthEventBusImpl()
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
        val viewModel = CredentialEntryViewModel(fakeTokenStore, authEventBus, ioDispatcher)
        viewModel.onClientIdChange(" id-1 ")
        viewModel.onClientSecretChange(" secret-1 ")

        viewModel.submit()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(AccessCredentials("id-1", "secret-1"), fakeTokenStore.saved)
        assertTrue(viewModel.uiState.value.saved)
    }

    @Test
    fun `submit with blank fields shows error and does not save`() {
        val viewModel = CredentialEntryViewModel(fakeTokenStore, authEventBus, ioDispatcher)

        viewModel.submit()

        assertEquals("Both client id and secret are required", viewModel.uiState.value.error)
        assertEquals(null, fakeTokenStore.saved)
    }

    @Test
    fun `saved flag resets after handling so a reused screen does not close immediately`() {
        val viewModel = CredentialEntryViewModel(fakeTokenStore, authEventBus, ioDispatcher)
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
        val viewModel = CredentialEntryViewModel(fakeTokenStore, authEventBus, ioDispatcher)
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

    @Test
    fun `credentials rejected event resets saved flag`() {
        val viewModel = CredentialEntryViewModel(fakeTokenStore, authEventBus, ioDispatcher)
        viewModel.onClientIdChange("id-1")
        viewModel.onClientSecretChange("secret-1")
        viewModel.submit()
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.saved)

        authEventBus.emit(AuthEvent.CredentialsRejected)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.saved)
    }

    @Test
    fun `unrelated auth events do not reset saved flag`() {
        val viewModel = CredentialEntryViewModel(fakeTokenStore, authEventBus, ioDispatcher)
        viewModel.onClientIdChange("id-1")
        viewModel.onClientSecretChange("secret-1")
        viewModel.submit()
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.saved)

        authEventBus.emit(AuthEvent.CredentialsRejected)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(AccessCredentials("id-1", "secret-1"), fakeTokenStore.saved)
    }

    @Test
    fun `scanned qr payload populates fields without saving`() {
        val viewModel = CredentialEntryViewModel(fakeTokenStore, authEventBus, ioDispatcher)

        viewModel.onQrPayloadScanned("https://example.com/auth?id=scanned-id&secret=scanned-secret")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("scanned-id", viewModel.uiState.value.clientId)
        assertEquals("scanned-secret", viewModel.uiState.value.clientSecret)
        assertEquals(null, fakeTokenStore.saved)
        assertFalse(viewModel.uiState.value.saved)
    }

    @Test
    fun `scanned qr credentials are only saved after user submits`() {
        val viewModel = CredentialEntryViewModel(fakeTokenStore, authEventBus, ioDispatcher)
        viewModel.onQrPayloadScanned("https://example.com/auth?id=scanned-id&secret=scanned-secret")

        viewModel.submit()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(AccessCredentials("scanned-id", "scanned-secret"), fakeTokenStore.saved)
        assertTrue(viewModel.uiState.value.saved)
    }

    @Test
    fun `unrecognized qr payload shows error and does not save`() {
        val viewModel = CredentialEntryViewModel(fakeTokenStore, authEventBus, ioDispatcher)

        viewModel.onQrPayloadScanned("not-a-credential-payload")

        assertEquals("Unrecognized QR code format", viewModel.uiState.value.error)
        assertEquals(null, fakeTokenStore.saved)
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

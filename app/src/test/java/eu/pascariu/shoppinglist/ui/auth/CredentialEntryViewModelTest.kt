package eu.pascariu.shoppinglist.ui.auth

import app.cash.turbine.test
import eu.pascariu.shoppinglist.data.auth.AccessCredentials
import eu.pascariu.shoppinglist.data.auth.AuthEvent
import eu.pascariu.shoppinglist.data.auth.AuthEventBusImpl
import eu.pascariu.shoppinglist.data.auth.TokenStore
import eu.pascariu.shoppinglist.data.network.HealthApi
import eu.pascariu.shoppinglist.data.network.HealthResponse
import eu.pascariu.shoppinglist.di.IoDispatcher
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class CredentialEntryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val ioDispatcher: TestDispatcher = StandardTestDispatcher(testDispatcher.scheduler)
    private val authEventBus = AuthEventBusImpl()
    private lateinit var fakeTokenStore: FakeTokenStore
    private lateinit var fakeHealthApi: FakeHealthApi

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeTokenStore = FakeTokenStore()
        fakeHealthApi = FakeHealthApi()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): CredentialEntryViewModel =
        CredentialEntryViewModel(fakeTokenStore, fakeHealthApi, authEventBus, ioDispatcher)

    @Test
    fun `submit saves trimmed credentials and marks state saved`() {
        val viewModel = createViewModel()
        viewModel.onClientIdChange(" id-1 ")
        viewModel.onClientSecretChange(" secret-1 ")

        viewModel.submit()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(AccessCredentials("id-1", "secret-1"), fakeTokenStore.saved)
        assertTrue(viewModel.uiState.value.saved)
    }

    @Test
    fun `submit with blank fields shows error and does not save`() {
        val viewModel = createViewModel()

        viewModel.submit()

        assertEquals("Both client id and secret are required", viewModel.uiState.value.error)
        assertEquals(null, fakeTokenStore.saved)
    }

    @Test
    fun `saved flag resets after handling so a reused screen does not close immediately`() {
        val viewModel = createViewModel()
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
        val viewModel = createViewModel()
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
        val viewModel = createViewModel()
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
        val viewModel = createViewModel()
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
        val viewModel = createViewModel()

        viewModel.onQrPayloadScanned("https://example.com/auth?id=scanned-id&secret=scanned-secret")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("scanned-id", viewModel.uiState.value.clientId)
        assertEquals("scanned-secret", viewModel.uiState.value.clientSecret)
        assertEquals(null, fakeTokenStore.saved)
        assertFalse(viewModel.uiState.value.saved)
    }

    @Test
    fun `scanned qr credentials are only saved after user submits`() {
        val viewModel = createViewModel()
        viewModel.onQrPayloadScanned("https://example.com/auth?id=scanned-id&secret=scanned-secret")

        viewModel.submit()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(AccessCredentials("scanned-id", "scanned-secret"), fakeTokenStore.saved)
        assertTrue(viewModel.uiState.value.saved)
    }

    @Test
    fun `unrecognized qr payload shows error and does not save`() {
        val viewModel = createViewModel()

        viewModel.onQrPayloadScanned("not-a-credential-payload")

        assertEquals("Unrecognized QR code format", viewModel.uiState.value.error)
        assertEquals(null, fakeTokenStore.saved)
    }

    @Test
    fun `valid probe saves credentials and emits saved`() = runViewModelTest { viewModel ->
        viewModel.onClientIdChange("id-1")
        viewModel.onClientSecretChange("secret-1")
        fakeHealthApi.behavior = { HealthResponse(status = "ok") }

        viewModel.uiState.test {
            viewModel.submit()
            awaitItem().let { state -> assertFalse(state.isSaving) }
            assertTrue(awaitItem().isSaving)
            val savedState = awaitItem()
            assertTrue(savedState.saved)
            assertFalse(savedState.isSaving)
            assertNull(savedState.error)
        }
        assertEquals(AccessCredentials("id-1", "secret-1"), fakeTokenStore.saved)
    }

    @Test
    fun `rejected probe shows invalid credentials error and stores nothing`() = runViewModelTest { viewModel ->
        viewModel.onClientIdChange("id-1")
        viewModel.onClientSecretChange("secret-1")
        fakeHealthApi.behavior = { throw httpException(401) }

        viewModel.uiState.test {
            viewModel.submit()
            skipItems(1)
            assertTrue(awaitItem().isSaving)
            val rejectedState = awaitItem()
            assertEquals(CredentialEntryViewModel.INVALID_CREDENTIALS_MESSAGE, rejectedState.error)
            assertEquals(CredentialErrorKind.InvalidCredentials, rejectedState.errorKind)
            assertFalse(rejectedState.isSaving)
            assertFalse(rejectedState.saved)
        }
        assertEquals(null, fakeTokenStore.saved)
    }

    @Test
    fun `forbidden probe shows invalid credentials error and stores nothing`() = runViewModelTest { viewModel ->
        viewModel.onClientIdChange("id-1")
        viewModel.onClientSecretChange("secret-1")
        fakeHealthApi.behavior = { throw httpException(403) }

        viewModel.uiState.test {
            viewModel.submit()
            skipItems(1)
            assertTrue(awaitItem().isSaving)
            val rejectedState = awaitItem()
            assertEquals(CredentialEntryViewModel.INVALID_CREDENTIALS_MESSAGE, rejectedState.error)
            assertEquals(CredentialErrorKind.InvalidCredentials, rejectedState.errorKind)
            assertFalse(rejectedState.isSaving)
        }
        assertEquals(null, fakeTokenStore.saved)
    }

    @Test
    fun `unreachable probe shows connectivity error and stores nothing`() = runViewModelTest { viewModel ->
        viewModel.onClientIdChange("id-1")
        viewModel.onClientSecretChange("secret-1")
        fakeHealthApi.behavior = { throw IOException("connection refused") }

        viewModel.uiState.test {
            viewModel.submit()
            skipItems(1)
            assertTrue(awaitItem().isSaving)
            val unreachableState = awaitItem()
            assertEquals(CredentialEntryViewModel.UNREACHABLE_MESSAGE, unreachableState.error)
            assertEquals(CredentialErrorKind.Unreachable, unreachableState.errorKind)
            assertFalse(unreachableState.isSaving)
            assertFalse(unreachableState.saved)
        }
        assertEquals(null, fakeTokenStore.saved)
    }

    @Test
    fun `unexpected http error shows connectivity error and stores nothing`() = runViewModelTest { viewModel ->
        viewModel.onClientIdChange("id-1")
        viewModel.onClientSecretChange("secret-1")
        fakeHealthApi.behavior = { throw httpException(500) }

        viewModel.uiState.test {
            viewModel.submit()
            skipItems(1)
            assertTrue(awaitItem().isSaving)
            val unreachableState = awaitItem()
            assertEquals(CredentialEntryViewModel.UNREACHABLE_MESSAGE, unreachableState.error)
            assertEquals(CredentialErrorKind.Unreachable, unreachableState.errorKind)
            assertFalse(unreachableState.isSaving)
        }
        assertEquals(null, fakeTokenStore.saved)
    }

    @Test
    fun `probe uses candidate credentials without touching the store first`() = runViewModelTest { viewModel ->
        viewModel.onClientIdChange("id-1")
        viewModel.onClientSecretChange("secret-1")
        fakeHealthApi.behavior = { HealthResponse(status = "ok") }

        viewModel.submit()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(AccessCredentials("id-1", "secret-1"), fakeHealthApi.lastProbeCredentials)
        assertEquals(AccessCredentials("id-1", "secret-1"), fakeTokenStore.saved)
    }

    @Test
    fun `rejected probe keeps previously stored credentials`() = runViewModelTest { viewModel ->
        fakeTokenStore.saved = AccessCredentials("old-id", "old-secret")
        viewModel.onClientIdChange("id-1")
        viewModel.onClientSecretChange("secret-1")
        fakeHealthApi.behavior = { throw httpException(401) }

        viewModel.submit()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(AccessCredentials("old-id", "old-secret"), fakeTokenStore.saved)
        assertEquals(CredentialErrorKind.InvalidCredentials, viewModel.uiState.value.errorKind)
    }

    @Test
    fun `unreachable probe keeps previously stored credentials`() = runViewModelTest { viewModel ->
        fakeTokenStore.saved = AccessCredentials("old-id", "old-secret")
        viewModel.onClientIdChange("id-1")
        viewModel.onClientSecretChange("secret-1")
        fakeHealthApi.behavior = { throw IOException("connection refused") }

        viewModel.submit()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(AccessCredentials("old-id", "old-secret"), fakeTokenStore.saved)
        assertEquals(CredentialErrorKind.Unreachable, viewModel.uiState.value.errorKind)
    }

    @Test
    fun `credentials rejected event produces the rejection banner`() = runViewModelTest { viewModel ->
        viewModel.uiState.test {
            assertFalse(awaitItem().rejectionMessage != null)
            authEventBus.emit(AuthEvent.CredentialsRejected)
            val bannerState = awaitItem()
            assertEquals(CredentialEntryViewModel.REJECTION_MESSAGE, bannerState.rejectionMessage)
            assertFalse(bannerState.saved)
        }
        assertEquals(null, fakeTokenStore.saved)
    }

    @Test
    fun `rejected probe does not set the rejection banner`() = runViewModelTest { viewModel ->
        viewModel.onClientIdChange("id-1")
        viewModel.onClientSecretChange("secret-1")
        fakeHealthApi.behavior = { throw httpException(401) }

        viewModel.uiState.test {
            viewModel.submit()
            skipItems(1)
            assertTrue(awaitItem().isSaving)
            val rejectedState = awaitItem()
            assertEquals(CredentialEntryViewModel.INVALID_CREDENTIALS_MESSAGE, rejectedState.error)
            assertNull(rejectedState.rejectionMessage)
        }
    }

    @Test
    fun `editing a field clears the rejection banner`() = runViewModelTest { viewModel ->
        authEventBus.emit(AuthEvent.CredentialsRejected)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(CredentialEntryViewModel.REJECTION_MESSAGE, viewModel.uiState.value.rejectionMessage)

        viewModel.onClientIdChange("id-1")

        assertNull(viewModel.uiState.value.rejectionMessage)
    }

    private fun runViewModelTest(block: suspend (viewModel: CredentialEntryViewModel) -> Unit) =
        kotlinx.coroutines.test.runTest {
            Dispatchers.setMain(testDispatcher)
            try {
                val viewModel = createViewModel()
                testDispatcher.scheduler.advanceUntilIdle()
                block(viewModel)
            } finally {
                Dispatchers.resetMain()
            }
        }

    private class FakeHealthApi(var behavior: () -> HealthResponse = {
        HealthResponse(status = "ok")
    }) : HealthApi {
        var lastProbeCredentials: AccessCredentials? = null
            private set

        override suspend fun getHealth(): HealthResponse = behavior()

        override suspend fun probeHealth(clientId: String, clientSecret: String): HealthResponse {
            lastProbeCredentials = AccessCredentials(clientId, clientSecret)
            return behavior()
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
}

private fun httpException(code: Int): HttpException =
    HttpException(Response.error<Any>(code, okhttp3.ResponseBody.create(null, "")))

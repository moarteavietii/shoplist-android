package eu.pascariu.shoppinglist.data.auth

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthResponseInterceptorTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var authEventBus: AuthEventBus
    private lateinit var tokenStore: FakeCredentialsTokenStore

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        authEventBus = AuthEventBusImpl()
        tokenStore = FakeCredentialsTokenStore()
        tokenStore.save(AccessCredentials(clientId = "id", clientSecret = "secret"))
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    private fun buildClient(): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(AuthResponseInterceptor(authEventBus, tokenStore))
            .build()

    @Test
    fun `emits credentials rejected on 401`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(401))

        authEventBus.events.test {
            buildClient().newCall(Request.Builder().url(mockWebServer.url("/health")).build())
                .execute()
                .close()

            assertEquals(AuthEvent.CredentialsRejected, awaitItem())
        }
    }

    @Test
    fun `clears stored credentials on 401`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(401))

        buildClient().newCall(Request.Builder().url(mockWebServer.url("/health")).build())
            .execute()
            .close()

        assertNull(tokenStore.load())
    }

    @Test
    fun `clears stored credentials on 403`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(403))

        buildClient().newCall(Request.Builder().url(mockWebServer.url("/health")).build())
            .execute()
            .close()

        assertNull(tokenStore.load())
    }

    @Test
    fun `keeps stored credentials on success`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        buildClient().newCall(Request.Builder().url(mockWebServer.url("/health")).build())
            .execute()
            .close()

        assertTrue(tokenStore.load() != null)
    }

    @Test
    fun `fake token store clear removes credentials`() {
        assertFalse(tokenStore.load() == null)
        tokenStore.clear()
        assertNull(tokenStore.load())
    }

    @Test
    fun `emits credentials rejected on 403`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(403))

        authEventBus.events.test {
            buildClient().newCall(Request.Builder().url(mockWebServer.url("/health")).build())
                .execute()
                .close()

            assertEquals(AuthEvent.CredentialsRejected, awaitItem())
        }
    }

    @Test
    fun `emits nothing on success`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        buildClient().newCall(Request.Builder().url(mockWebServer.url("/health")).build())
            .execute()
            .close()

        assertNull(authEventBus.events.replayCache.firstOrNull())
    }
}

private class FakeCredentialsTokenStore(var saved: AccessCredentials? = null) : TokenStore {
    override fun load(): AccessCredentials? = saved
    override fun save(credentials: AccessCredentials) {
        saved = credentials
    }
    override fun clear() {
        saved = null
    }
}

package eu.pascariu.shoppinglist.data.auth

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class AuthResponseInterceptorTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var authEventBus: AuthEventBus

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        authEventBus = AuthEventBusImpl()
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    private fun buildClient(): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(AuthResponseInterceptor(authEventBus))
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

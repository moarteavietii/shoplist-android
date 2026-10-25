package eu.pascariu.shoppinglist.data.auth

import app.cash.turbine.test
import java.net.InetAddress
import kotlinx.coroutines.test.runTest
import okhttp3.Dns
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

        authEventBus.events.test {
            buildClient().newCall(Request.Builder().url(mockWebServer.url("/health")).build())
                .execute()
                .close()

            expectNoEvents()
        }
    }

    @Test
    fun `explicit credential probe with 401 keeps stored credentials and emits nothing`() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(401))

        val request = Request.Builder().url(mockWebServer.url("/health"))
            .header(CloudflareAccessInterceptor.HEADER_CLIENT_ID, "probe-id")
            .header(CloudflareAccessInterceptor.HEADER_CLIENT_SECRET, "probe-secret")
            .build()
        authEventBus.events.test {
            buildClient().newCall(request).execute().close()

            expectNoEvents()
        }
        assertEquals(AccessCredentials(clientId = "id", clientSecret = "secret"), tokenStore.load())
    }

    @Test
    fun `emits credentials rejected when redirected to the access gate`() = runTest {
        val gateHost = "gate.cloudflareaccess.com"
        val gateUrl = mockWebServer.url("/").newBuilder().host(gateHost).build()
        mockWebServer.enqueue(
            MockResponse().setResponseCode(302)
                .setHeader("Location", gateUrl.resolve("/login").toString())
        )
        mockWebServer.enqueue(
            MockResponse().setResponseCode(200)
                .setBody("<!DOCTYPE html><html><head><title>Access login</title></head></html>")
        )

        val client = OkHttpClient.Builder()
            .dns { hostname ->
                if (hostname == gateHost) {
                    listOf(InetAddress.getByName("127.0.0.1"))
                } else {
                    Dns.SYSTEM.lookup(hostname)
                }
            }
            .addInterceptor(AuthResponseInterceptor(authEventBus, tokenStore))
            .build()

        authEventBus.events.test {
            client.newCall(Request.Builder().url(mockWebServer.url("/api/v1/items")).build())
                .execute()
                .close()

            assertEquals(AuthEvent.CredentialsRejected, awaitItem())
        }
        assertNull(tokenStore.load())
    }

    @Test
    fun `emits nothing when gate redirect is an explicit credential probe`() = runTest {
        val gateHost = "probe.cloudflareaccess.com"
        val gateUrl = mockWebServer.url("/").newBuilder().host(gateHost).build()
        mockWebServer.enqueue(
            MockResponse().setResponseCode(302)
                .setHeader("Location", gateUrl.resolve("/login").toString())
        )
        mockWebServer.enqueue(
            MockResponse().setResponseCode(200)
                .setBody("<!DOCTYPE html><html><head><title>Access login</title></head></html>")
        )

        val client = OkHttpClient.Builder()
            .dns { hostname ->
                if (hostname == gateHost) {
                    listOf(InetAddress.getByName("127.0.0.1"))
                } else {
                    Dns.SYSTEM.lookup(hostname)
                }
            }
            .addInterceptor(AuthResponseInterceptor(authEventBus, tokenStore))
            .build()

        val request = Request.Builder().url(mockWebServer.url("/health"))
            .header(CloudflareAccessInterceptor.HEADER_CLIENT_ID, "probe-id")
            .header(CloudflareAccessInterceptor.HEADER_CLIENT_SECRET, "probe-secret")
            .build()
        authEventBus.events.test {
            client.newCall(request).execute().close()

            expectNoEvents()
        }
        assertEquals(AccessCredentials(clientId = "id", clientSecret = "secret"), tokenStore.load())
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

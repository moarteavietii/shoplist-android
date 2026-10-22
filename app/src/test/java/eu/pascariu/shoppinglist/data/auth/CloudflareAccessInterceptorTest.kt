package eu.pascariu.shoppinglist.data.auth

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class CloudflareAccessInterceptorTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var fakeTokenStore: FakeTokenStore

    @Before
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        fakeTokenStore = FakeTokenStore()
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    private fun buildClient(): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(CloudflareAccessInterceptor(fakeTokenStore))
            .build()

    @Test
    fun `injects both headers when credentials are stored`() {
        fakeTokenStore.saved = AccessCredentials(clientId = "id-1", clientSecret = "secret-1")
        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        buildClient().newCall(Request.Builder().url(mockWebServer.url("/health")).build())
            .execute()
            .close()

        val recorded = mockWebServer.takeRequest()
        assertEquals("id-1", recorded.getHeader(CloudflareAccessInterceptor.HEADER_CLIENT_ID))
        assertEquals("secret-1", recorded.getHeader(CloudflareAccessInterceptor.HEADER_CLIENT_SECRET))
    }

    @Test
    fun `injects no headers when credentials are missing`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        buildClient().newCall(Request.Builder().url(mockWebServer.url("/health")).build())
            .execute()
            .close()

        val recorded = mockWebServer.takeRequest()
        assertNull(recorded.getHeader(CloudflareAccessInterceptor.HEADER_CLIENT_ID))
        assertNull(recorded.getHeader(CloudflareAccessInterceptor.HEADER_CLIENT_SECRET))
    }

    @Test
    fun `no-op interceptor injects no headers even with credentials stored`() {
        fakeTokenStore.saved = AccessCredentials(clientId = "id-1", clientSecret = "secret-1")
        mockWebServer.enqueue(MockResponse().setResponseCode(200))
        val client = OkHttpClient.Builder()
            .addInterceptor(NoOpAuthInterceptor())
            .build()

        client.newCall(Request.Builder().url(mockWebServer.url("/health")).build())
            .execute()
            .close()

        val recorded = mockWebServer.takeRequest()
        assertNull(recorded.getHeader(CloudflareAccessInterceptor.HEADER_CLIENT_ID))
        assertNull(recorded.getHeader(CloudflareAccessInterceptor.HEADER_CLIENT_SECRET))
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

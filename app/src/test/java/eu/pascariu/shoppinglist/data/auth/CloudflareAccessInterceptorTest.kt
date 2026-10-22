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

    private lateinit var apiServer: MockWebServer
    private lateinit var otherServer: MockWebServer
    private lateinit var fakeTokenStore: FakeTokenStore

    @Before
    fun setUp() {
        apiServer = MockWebServer()
        apiServer.start()
        otherServer = MockWebServer()
        otherServer.start()
        fakeTokenStore = FakeTokenStore()
    }

    @After
    fun tearDown() {
        apiServer.shutdown()
        otherServer.shutdown()
    }

    private fun buildClient(): OkHttpClient =
        OkHttpClient.Builder()
            .addNetworkInterceptor(
                CloudflareAccessInterceptor(fakeTokenStore, apiServer.url("/").toString())
            )
            .build()

    @Test
    fun `injects both headers when credentials are stored`() {
        fakeTokenStore.saved = AccessCredentials(clientId = "id-1", clientSecret = "secret-1")
        apiServer.enqueue(MockResponse().setResponseCode(200))

        buildClient().newCall(Request.Builder().url(apiServer.url("/health")).build())
            .execute()
            .close()

        val recorded = apiServer.takeRequest()
        assertEquals("id-1", recorded.getHeader(CloudflareAccessInterceptor.HEADER_CLIENT_ID))
        assertEquals("secret-1", recorded.getHeader(CloudflareAccessInterceptor.HEADER_CLIENT_SECRET))
    }

    @Test
    fun `injects no headers when credentials are missing`() {
        apiServer.enqueue(MockResponse().setResponseCode(200))

        buildClient().newCall(Request.Builder().url(apiServer.url("/health")).build())
            .execute()
            .close()

        val recorded = apiServer.takeRequest()
        assertNull(recorded.getHeader(CloudflareAccessInterceptor.HEADER_CLIENT_ID))
        assertNull(recorded.getHeader(CloudflareAccessInterceptor.HEADER_CLIENT_SECRET))
    }

    @Test
    fun `no-op interceptor injects no headers even with credentials stored`() {
        fakeTokenStore.saved = AccessCredentials(clientId = "id-1", clientSecret = "secret-1")
        apiServer.enqueue(MockResponse().setResponseCode(200))
        val client = OkHttpClient.Builder()
            .addInterceptor(NoOpAuthInterceptor())
            .build()

        client.newCall(Request.Builder().url(apiServer.url("/health")).build())
            .execute()
            .close()

        val recorded = apiServer.takeRequest()
        assertNull(recorded.getHeader(CloudflareAccessInterceptor.HEADER_CLIENT_ID))
        assertNull(recorded.getHeader(CloudflareAccessInterceptor.HEADER_CLIENT_SECRET))
    }

    @Test
    fun `strips credentials for requests to other hosts`() {
        fakeTokenStore.saved = AccessCredentials(clientId = "id-1", clientSecret = "secret-1")
        otherServer.enqueue(MockResponse().setResponseCode(200))

        buildClient().newCall(Request.Builder().url(otherServer.url("/elsewhere")).build())
            .execute()
            .close()

        val recorded = otherServer.takeRequest()
        assertNull(recorded.getHeader(CloudflareAccessInterceptor.HEADER_CLIENT_ID))
        assertNull(recorded.getHeader(CloudflareAccessInterceptor.HEADER_CLIENT_SECRET))
    }

    @Test
    fun `does not leak credentials on cross-host redirect`() {
        fakeTokenStore.saved = AccessCredentials(clientId = "id-1", clientSecret = "secret-1")
        apiServer.enqueue(
            MockResponse().setResponseCode(302)
                .setHeader("Location", otherServer.url("/elsewhere"))
        )
        otherServer.enqueue(MockResponse().setResponseCode(200))

        buildClient().newCall(Request.Builder().url(apiServer.url("/health")).build())
            .execute()
            .close()

        val redirected = otherServer.takeRequest()
        assertNull(redirected.getHeader(CloudflareAccessInterceptor.HEADER_CLIENT_ID))
        assertNull(redirected.getHeader(CloudflareAccessInterceptor.HEADER_CLIENT_SECRET))
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

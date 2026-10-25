package eu.pascariu.shoppinglist.data.auth

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MockCloudflareGateImplTest {

    private lateinit var server: MockWebServer
    private lateinit var fakeTokenStore: GateFakeTokenStore

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        fakeTokenStore = GateFakeTokenStore()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun buildClient(mockEnabled: Boolean): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(MockCloudflareGateImpl(fakeTokenStore, mockEnabled))
            .build()

    @Test
    fun `fake credential pair passes through to the backend`() {
        fakeTokenStore.saved = AccessCredentials(
            clientId = MockCloudflareGateImpl.FAKE_CLIENT_ID,
            clientSecret = MockCloudflareGateImpl.FAKE_CLIENT_SECRET,
        )
        server.enqueue(MockResponse().setResponseCode(200))

        val response = buildClient(mockEnabled = true)
            .newCall(Request.Builder().url(server.url("/health")).build())
            .execute()

        assertEquals(200, response.code)
        response.close()
        val recorded = server.takeRequest()
        assertEquals(
            MockCloudflareGateImpl.FAKE_CLIENT_ID,
            recorded.getHeader(CloudflareAccessInterceptor.HEADER_CLIENT_ID),
        )
        assertEquals(
            MockCloudflareGateImpl.FAKE_CLIENT_SECRET,
            recorded.getHeader(CloudflareAccessInterceptor.HEADER_CLIENT_SECRET),
        )
    }

    @Test
    fun `wrong credential pair yields synthesized 401 with no network call`() {
        fakeTokenStore.saved = AccessCredentials(clientId = "wrong-id", clientSecret = "wrong-secret")

        val response = buildClient(mockEnabled = true)
            .newCall(Request.Builder().url(server.url("/health")).build())
            .execute()

        assertEquals(401, response.code)
        response.close()
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `missing credentials yield synthesized 401 with no network call`() {
        val response = buildClient(mockEnabled = true)
            .newCall(Request.Builder().url(server.url("/health")).build())
            .execute()

        assertEquals(401, response.code)
        response.close()
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `correct id with wrong secret yields synthesized 401`() {
        fakeTokenStore.saved = AccessCredentials(
            clientId = MockCloudflareGateImpl.FAKE_CLIENT_ID,
            clientSecret = "not-the-secret",
        )

        val response = buildClient(mockEnabled = true)
            .newCall(Request.Builder().url(server.url("/health")).build())
            .execute()

        assertEquals(401, response.code)
        response.close()
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `flag off passes request through untouched even with non-fake credentials`() {
        fakeTokenStore.saved = AccessCredentials(clientId = "wrong-id", clientSecret = "wrong-secret")
        server.enqueue(MockResponse().setResponseCode(200))

        val response = buildClient(mockEnabled = false)
            .newCall(Request.Builder().url(server.url("/health")).build())
            .execute()

        assertEquals(200, response.code)
        response.close()
        val recorded = server.takeRequest()
        assertNull(recorded.getHeader(CloudflareAccessInterceptor.HEADER_CLIENT_ID))
        assertNull(recorded.getHeader(CloudflareAccessInterceptor.HEADER_CLIENT_SECRET))
    }

    @Test
    fun `explicit probe headers are validated against the fake pair`() {
        server.enqueue(MockResponse().setResponseCode(200))

        val request = Request.Builder().url(server.url("/health"))
            .header(CloudflareAccessInterceptor.HEADER_CLIENT_ID, MockCloudflareGateImpl.FAKE_CLIENT_ID)
            .header(CloudflareAccessInterceptor.HEADER_CLIENT_SECRET, MockCloudflareGateImpl.FAKE_CLIENT_SECRET)
            .build()
        val response = buildClient(mockEnabled = true).newCall(request).execute()

        assertEquals(200, response.code)
        response.close()
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `explicit probe headers that do not match the fake pair yield synthesized 401`() {
        val request = Request.Builder().url(server.url("/health"))
            .header(CloudflareAccessInterceptor.HEADER_CLIENT_ID, "probe-id")
            .header(CloudflareAccessInterceptor.HEADER_CLIENT_SECRET, "probe-secret")
            .build()

        val response = buildClient(mockEnabled = true).newCall(request).execute()

        assertEquals(401, response.code)
        response.close()
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `synthesized 401 carries the authorized request so outer interceptors see it`() {
        fakeTokenStore.saved = AccessCredentials(clientId = "wrong-id", clientSecret = "wrong-secret")

        val response = buildClient(mockEnabled = true)
            .newCall(Request.Builder().url(server.url("/health")).build())
            .execute()

        assertEquals(401, response.code)
        assertTrue(response.request.header(CloudflareAccessInterceptor.HEADER_CLIENT_ID) != null)
        response.close()
    }
}

private class GateFakeTokenStore(var saved: AccessCredentials? = null) : TokenStore {
    override fun load(): AccessCredentials? = saved
    override fun save(credentials: AccessCredentials) {
        saved = credentials
    }
    override fun clear() {
        saved = null
    }
}

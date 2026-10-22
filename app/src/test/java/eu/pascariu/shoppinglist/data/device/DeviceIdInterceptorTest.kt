package eu.pascariu.shoppinglist.data.device

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeviceIdInterceptorTest {

    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpClient
    private val fakeStore = FakeDeviceIdStore()

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = OkHttpClient.Builder()
            .addInterceptor(DeviceIdInterceptor(fakeStore))
            .build()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `post carries device id header`() {
        server.enqueue(MockResponse().setResponseCode(200))
        val request = Request.Builder()
            .url(server.url("/api/shops"))
            .post("{}".toRequestBody())
            .build()

        client.newCall(request).execute().close()

        assertEquals("test-device-id", server.takeRequest().getHeader("X-Device-Id"))
    }

    @Test
    fun `patch carries device id header`() {
        server.enqueue(MockResponse().setResponseCode(200))
        val request = Request.Builder()
            .url(server.url("/api/items/1"))
            .patch("{}".toRequestBody())
            .build()

        client.newCall(request).execute().close()

        assertEquals("test-device-id", server.takeRequest().getHeader("X-Device-Id"))
    }

    @Test
    fun `delete carries device id header`() {
        server.enqueue(MockResponse().setResponseCode(204))
        val request = Request.Builder()
            .url(server.url("/api/items/1"))
            .delete()
            .build()

        client.newCall(request).execute().close()

        assertEquals("test-device-id", server.takeRequest().getHeader("X-Device-Id"))
    }

    @Test
    fun `get omits device id header`() {
        server.enqueue(MockResponse().setResponseCode(200))
        val request = Request.Builder()
            .url(server.url("/api/shops"))
            .get()
            .build()

        client.newCall(request).execute().close()

        assertNull(server.takeRequest().getHeader("X-Device-Id"))
    }

    @Test
    fun `device id is stable across requests`() {
        server.enqueue(MockResponse().setResponseCode(200))
        server.enqueue(MockResponse().setResponseCode(200))
        val request = Request.Builder()
            .url(server.url("/api/shops"))
            .post("{}".toRequestBody())
            .build()

        client.newCall(request).execute().close()
        client.newCall(request).execute().close()

        val first = server.takeRequest().getHeader("X-Device-Id")
        val second = server.takeRequest().getHeader("X-Device-Id")
        assertEquals(first, second)
        assertTrue(fakeStore.creationCount == 1)
    }
}

private class FakeDeviceIdStore : DeviceIdStore {
    var creationCount = 0
    private var deviceId: String? = null

    override fun getOrCreate(): String {
        return deviceId ?: "test-device-id".also {
            creationCount++
            deviceId = it
        }
    }
}

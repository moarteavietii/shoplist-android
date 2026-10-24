package eu.pascariu.shoppinglist.data.connectivity

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Test

class BackendReachabilityListenerTest {

    private class FakeReachability : BackendReachability {
        var reachableConfirmations = 0
        var unreachableConfirmations = 0
        override fun confirmReachable() {
            reachableConfirmations++
        }

        override fun confirmUnreachable() {
            unreachableConfirmations++
        }
    }

    private fun client(reachability: FakeReachability): OkHttpClient =
        OkHttpClient.Builder()
            .eventListener(BackendReachabilityListener(reachability))
            .build()

    @Test
    fun `successful response confirms reachable`() {
        val server = MockWebServer()
        server.enqueue(MockResponse().setBody("ok"))
        server.start()
        val reachability = FakeReachability()

        client(reachability).newCall(Request.Builder().url(server.url("/health")).build())
            .execute()
            .close()

        assertEquals(1, reachability.reachableConfirmations)
        assertEquals(0, reachability.unreachableConfirmations)
        server.shutdown()
    }

    @Test
    fun `failed call confirms unreachable`() {
        val server = MockWebServer()
        val url = server.url("/health")
        server.shutdown()
        val reachability = FakeReachability()

        runCatching {
            client(reachability).newCall(Request.Builder().url(url).build()).execute()
        }

        assertEquals(0, reachability.reachableConfirmations)
        assertEquals(true, reachability.unreachableConfirmations >= 1)
    }
}

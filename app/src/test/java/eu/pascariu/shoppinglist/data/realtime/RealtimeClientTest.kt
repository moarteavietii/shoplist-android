package eu.pascariu.shoppinglist.data.realtime

import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RealtimeClientTest {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private lateinit var server: MockWebServer
    private lateinit var client: RealtimeClient

    @After
    fun tearDown() {
        if (::client.isInitialized) client.close()
        if (::server.isInitialized) server.shutdown()
    }

    private fun startServer(routing: (path: String, requestIndex: Int) -> String) {
        val requestCounts = mutableMapOf<String, Int>()
        server = MockWebServer()
        server.dispatcher = object : okhttp3.mockwebserver.Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path ?: ""
                val index = (requestCounts[path] ?: 0).also { requestCounts[path] = it + 1 }
                return MockResponse()
                    .setHeader("Content-Type", "text/event-stream")
                    .setBody(routing(path, index))
            }
        }
        server.start()
        client = RealtimeClient(OkHttpClient(), json, server.url("/").toString(), listId = 1L)
    }

    @Test
    fun `item stream parses frames into typed events and ignores heartbeats`() = runBlocking {
        startServer(
            routing = { _, requestIndex ->
                if (requestIndex == 0) {
                    """
                    event: connected
                    data: {}

                    event: item:created
                    data: {"type":"item:created","listId":1,"item":{"id":42,"listId":1,"name":"Milk","isChecked":false,"orderMap":{"2":"a0"},"quantity":1,"isTemporary":false,"checkCount":0,"expiresAt":null,"createdAt":"2026-10-22T10:00:00.000Z","visibleIn":[1]},"originDeviceId":"device-a"}

                    : ping

                    event: item:deleted
                    data: {"type":"item:deleted","listId":1,"itemId":42,"originDeviceId":"device-b"}

                    event: item:corrupted
                    data: {bad json

                    event: item:updated
                    data: {"type":"item:updated","listId":1,"item":{"id":42,"listId":1,"name":"Whole milk","isChecked":true,"orderMap":{"2":"a0"},"quantity":1,"isTemporary":false,"checkCount":1,"expiresAt":null,"createdAt":"2026-10-22T10:00:00.000Z","visibleIn":[1]}}

                    """.trimIndent() + "\n\n"
                } else {
                    "event: connected\ndata: {}\n\n"
                }
            },
        )

        val created = CompletableDeferred<eu.pascariu.shoppinglist.data.model.ItemEvent>()
        val deleted = CompletableDeferred<eu.pascariu.shoppinglist.data.model.ItemEvent>()
        val updated = CompletableDeferred<eu.pascariu.shoppinglist.data.model.ItemEvent>()
        val job = launch {
            client.itemEvents.collect { event ->
                when (event.type) {
                    "item:created" -> created.complete(event)
                    "item:deleted" -> deleted.complete(event)
                    "item:updated" -> updated.complete(event)
                }
            }
        }
        delay(200)
        client.start()

        withTimeout(5_000) {
            val createdEvent = created.await()
            assertEquals("Milk", createdEvent.item?.name)
            assertEquals("device-a", createdEvent.originDeviceId)

            val deletedEvent = deleted.await()
            assertEquals(42L, deletedEvent.itemId)
            assertNull(deletedEvent.item)

            val updatedEvent = updated.await()
            assertEquals("Whole milk", updatedEvent.item?.name)
            assertNull(updatedEvent.originDeviceId)
        }
        job.cancel()
    }

    @Test
    fun `connected frame is consumed as the connect signal`() = runBlocking {
        startServer(
            routing = { path, requestIndex ->
                if (path.contains("/shops/subscribe") && requestIndex == 0) {
                    """
                    event: connected
                    data: {}

                    event: shop:created
                    data: {"type":"shop:created","shop":{"id":4,"name":"Global","createdAt":"2026-10-22T10:00:00.000Z"},"originDeviceId":"device-a"}

                    """.trimIndent() + "\n\n"
                } else {
                    "event: connected\ndata: {}\n\n"
                }
            },
        )

        val shopCreated = CompletableDeferred<eu.pascariu.shoppinglist.data.model.ShopEvent>()
        val eventsJob = launch {
            client.shopEvents.collect { event ->
                if (event.type == "shop:created") shopCreated.complete(event)
            }
        }
        delay(200)
        client.start()

        withTimeout(5_000) {
            val event = shopCreated.await()
            assertEquals("Global", event.shop?.name)
            assertEquals("device-a", event.originDeviceId)
        }
        eventsJob.cancel()
    }

    @Test
    fun `server close schedules a reconnect while visible`() = runBlocking {
        startServer(routing = { _, _ -> "event: connected\ndata: {}\n\n" })
        client = RealtimeClient(
            okHttpClient = OkHttpClient(),
            json = json,
            baseUrl = server.url("/").toString(),
            listId = 1L,
            initialBackoff = 10.milliseconds,
            maxBackoff = 50.milliseconds,
        )

        var itemsConnects = 0
        val secondConnect = CompletableDeferred<Unit>()
        val job = launch {
            client.connectSignals.collect { stream ->
                if (stream == RealtimeClient.Stream.ITEMS) {
                    itemsConnects++
                    if (itemsConnects == 2) secondConnect.complete(Unit)
                }
            }
        }
        client.start()

        withTimeout(5_000) { secondConnect.await() }
        job.cancel()
    }
}

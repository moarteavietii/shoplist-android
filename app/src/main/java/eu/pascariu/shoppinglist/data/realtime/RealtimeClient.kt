package eu.pascariu.shoppinglist.data.realtime

import eu.pascariu.shoppinglist.data.model.ItemEvent
import eu.pascariu.shoppinglist.data.model.ShopEvent
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources

class RealtimeClient(
    private val okHttpClient: OkHttpClient,
    private val json: Json,
    baseUrl: String,
    listId: Long,
    private val initialBackoff: Duration = 1.seconds,
    private val maxBackoff: Duration = 30.seconds,
) : RealtimeClientContract {

    enum class Stream { ITEMS, SHOPS }

    private val itemsUrl = "${baseUrl.trimEnd('/')}/api/lists/$listId/subscribe"
    private val shopsUrl = "${baseUrl.trimEnd('/')}/api/shops/subscribe"

    private val itemEventsFlow = MutableSharedFlow<ItemEvent>(extraBufferCapacity = 64)
    override val itemEvents: SharedFlow<ItemEvent> = itemEventsFlow.asSharedFlow()

    private val shopEventsFlow = MutableSharedFlow<ShopEvent>(extraBufferCapacity = 64)
    override val shopEvents: SharedFlow<ShopEvent> = shopEventsFlow.asSharedFlow()

    private val connectSignalsFlow = MutableSharedFlow<Stream>(extraBufferCapacity = 8)
    override val connectSignals: SharedFlow<Stream> = connectSignalsFlow.asSharedFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val streamJobs = mutableListOf<Job>()

    @Volatile private var running = false

    @Volatile private var closed = false

    override fun start() {
        if (closed || running) return
        running = true
        streamJobs += scope.launch {
            runStream(itemsUrl, itemEventsFlow, ItemEvent.serializer(), Stream.ITEMS)
        }
        streamJobs += scope.launch {
            runStream(shopsUrl, shopEventsFlow, ShopEvent.serializer(), Stream.SHOPS)
        }
    }

    override fun stop() {
        running = false
        streamJobs.forEach { it.cancel() }
        streamJobs.clear()
    }

    fun close() {
        closed = true
        stop()
        scope.cancel()
    }

    private suspend fun <T> runStream(
        url: String,
        eventsFlow: MutableSharedFlow<T>,
        serializer: KSerializer<T>,
        stream: Stream,
    ) {
        var backoff = initialBackoff
        while (running && scope.isActive) {
            val connected = connectOnce(url, eventsFlow, serializer, stream)
            if (connected) {
                backoff = initialBackoff
            } else {
                val jitter = Random.nextLong(0, backoff.inWholeMilliseconds / 2 + 1)
                delay(backoff + jitter.milliseconds)
                backoff = (backoff * 2).coerceAtMost(maxBackoff)
            }
        }
    }

    private suspend fun <T> connectOnce(
        url: String,
        eventsFlow: MutableSharedFlow<T>,
        serializer: KSerializer<T>,
        stream: Stream,
    ): Boolean {
        val finished = CompletableDeferred<Unit>()
        var connected = false
        val request = Request.Builder().url(url).build()
        val listener = object : EventSourceListener() {
            override fun onOpen(eventSource: EventSource, response: Response) {
                connected = true
            }

            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                if (type == CONNECTED_EVENT_TYPE) {
                    connectSignalsFlow.tryEmit(stream)
                    return
                }
                val event = runCatching { json.decodeFromString(serializer, data) }.getOrNull() ?: return
                eventsFlow.tryEmit(event)
            }

            override fun onClosed(eventSource: EventSource) {
                finished.complete(Unit)
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                finished.complete(Unit)
            }
        }
        val eventSource = EventSources.createFactory(okHttpClient).newEventSource(request, listener)
        try {
            finished.await()
        } finally {
            eventSource.cancel()
        }
        return connected
    }

    companion object {
        const val DEFAULT_LIST_ID = 1L

        private const val CONNECTED_EVENT_TYPE = "connected"
    }
}

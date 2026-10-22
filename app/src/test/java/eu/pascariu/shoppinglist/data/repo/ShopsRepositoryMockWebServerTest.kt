package eu.pascariu.shoppinglist.data.repo

import eu.pascariu.shoppinglist.data.api.ShopsApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class ShopsRepositoryMockWebServerTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: ShopsRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
        val api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ShopsApi::class.java)
        repository = ShopsRepository(api)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun shopJson(id: Long, name: String, createdAt: String = "2026-10-22T09:00:00Z"): String =
        "{\"id\":$id,\"name\":\"$name\",\"createdAt\":\"$createdAt\"}"

    private fun enqueueShopListJson(vararg shopJsons: String): MockResponse =
        MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody("[${shopJsons.joinToString(",")}]")

    private fun enqueueShopResponse(shopJson: String, code: Int = 200): MockResponse =
        MockResponse()
            .setResponseCode(code)
            .setHeader("Content-Type", "application/json")
            .setBody(shopJson)

    @Test
    fun `listShops deserializes real JSON and sorts Global first then A to Z`() = runTest {
        server.enqueue(
            enqueueShopListJson(
                shopJson(id = 2, name = "bakery"),
                shopJson(id = 3, name = "Alpha"),
                shopJson(id = 4, name = "Global"),
            )
        )

        repository.refetch()

        val recorded = server.takeRequest()
        assertEquals("GET", recorded.method)
        assertEquals("/api/shops", recorded.path)
        assertEquals(listOf(4L, 3L, 2L), repository.shops.first().map { it.id })
    }

    @Test
    fun `create posts JSON body and inserts server shop in sorted position`() = runTest {
        server.enqueue(enqueueShopListJson(shopJson(id = 3, name = "Bakery")))
        repository.refetch()
        server.takeRequest()
        server.enqueue(enqueueShopResponse(shopJson(id = 1, name = "Global")))

        val created = repository.create(name = "Global")

        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/api/shops", recorded.path)
        assertTrue(recorded.body.readUtf8().contains("\"name\":\"Global\""))
        assertEquals(1L, created.id)
        assertEquals(listOf(1L, 3L), repository.shops.first().map { it.id })
    }

    @Test
    fun `rename patches JSON body to shop path and re-sorts cache`() = runTest {
        server.enqueue(enqueueShopListJson(shopJson(id = 2, name = "Bakery")))
        repository.refetch()
        server.takeRequest()
        server.enqueue(enqueueShopResponse(shopJson(id = 2, name = "Global")))

        val renamed = repository.rename(shopId = 2, name = "Global")

        val recorded = server.takeRequest()
        assertEquals("PATCH", recorded.method)
        assertEquals("/api/shops/2", recorded.path)
        assertTrue(recorded.body.readUtf8().contains("\"name\":\"Global\""))
        assertEquals("Global", renamed.name)
        assertEquals(listOf(2L), repository.shops.first().map { it.id })
    }

    @Test
    fun `delete sends DELETE to shop path and removes from cache`() = runTest {
        server.enqueue(enqueueShopListJson(shopJson(id = 2, name = "Bakery")))
        repository.refetch()
        server.takeRequest()
        server.enqueue(MockResponse().setResponseCode(204))

        repository.delete(shopId = 2)

        val recorded = server.takeRequest()
        assertEquals("DELETE", recorded.method)
        assertEquals("/api/shops/2", recorded.path)
        assertTrue(repository.shops.first().isEmpty())
    }

    @Test
    fun `409 on create surfaces HttpException and leaves cache unchanged`() = runTest {
        server.enqueue(enqueueShopListJson(shopJson(id = 2, name = "Bakery")))
        repository.refetch()
        server.enqueue(
            MockResponse()
                .setResponseCode(409)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"error\":\"duplicate\"}")
        )

        val thrown = runCatching { repository.create(name = "Bakery") }

        assertTrue(thrown.exceptionOrNull() is HttpException)
        assertEquals(409, (thrown.exceptionOrNull() as HttpException).code())
        assertEquals(listOf(2L), repository.shops.first().map { it.id })
    }

    @Test
    fun `403 on rename of Global surfaces HttpException and leaves cache unchanged`() = runTest {
        server.enqueue(enqueueShopListJson(shopJson(id = 4, name = "Global")))
        repository.refetch()
        server.enqueue(MockResponse().setResponseCode(403))

        val thrown = runCatching { repository.rename(shopId = 4, name = "Mine") }

        assertTrue(thrown.exceptionOrNull() is HttpException)
        assertEquals(403, (thrown.exceptionOrNull() as HttpException).code())
        assertEquals(listOf("Global"), repository.shops.first().map { it.name })
    }

    @Test
    fun `500 on delete surfaces HttpException and leaves cache unchanged`() = runTest {
        server.enqueue(enqueueShopListJson(shopJson(id = 2, name = "Bakery")))
        repository.refetch()
        server.enqueue(MockResponse().setResponseCode(500))

        val thrown = runCatching { repository.delete(shopId = 2) }

        assertTrue(thrown.exceptionOrNull() is HttpException)
        assertEquals(500, (thrown.exceptionOrNull() as HttpException).code())
        assertEquals(listOf(2L), repository.shops.first().map { it.id })
    }
}

package eu.pascariu.shoppinglist.data.repo

import eu.pascariu.shoppinglist.data.api.ShopsApi
import eu.pascariu.shoppinglist.data.model.CreateShopRequest
import eu.pascariu.shoppinglist.data.model.RenameShopRequest
import eu.pascariu.shoppinglist.data.model.Shop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class ShopsRepositoryTest {

    @Test
    fun `first fetch populates cache ordered Global first then A to Z`() = runTest {
        val api = FakeShopsApi(shopsForFetch = listOf(bakery(), supermarket(), global()))
        val repository = ShopsRepository(api)

        repository.refetch()

        assertEquals(
            listOf("Global", "Bakery", "Supermarket"),
            repository.shops.first().map { it.name },
        )
    }

    @Test
    fun `refetch replaces cache wholesale`() = runTest {
        val api = FakeShopsApi(shopsForFetch = listOf(bakery(), global()))
        val repository = ShopsRepository(api)
        repository.refetch()

        api.shopsForFetch = listOf(shop(9, "Zoo"))
        repository.refetch()

        assertEquals(listOf("Zoo"), repository.shops.first().map { it.name })
    }

    @Test
    fun `create re-sorts cache Global first then A to Z`() = runTest {
        val api = FakeShopsApi(shopsForFetch = listOf(bakery(), global()))
        val repository = ShopsRepository(api)
        repository.refetch()

        api.createdShop = shop(3, "Alpine")
        repository.create("Alpine")

        assertEquals(CreateShopRequest(name = "Alpine"), api.lastCreateRequest)
        assertEquals(
            listOf("Global", "Alpine", "Bakery"),
            repository.shops.first().map { it.name },
        )
    }

    @Test
    fun `rename re-sorts cache`() = runTest {
        val api = FakeShopsApi(shopsForFetch = listOf(bakery(), supermarket(), global()))
        val repository = ShopsRepository(api)
        repository.refetch()

        api.renamedShop = shop(1, "Zoo")
        repository.rename(shopId = 1, name = "Zoo")

        assertEquals(1L, api.lastRenameShopId)
        assertEquals(RenameShopRequest(name = "Zoo"), api.lastRenameRequest)
        assertEquals(
            listOf("Global", "Supermarket", "Zoo"),
            repository.shops.first().map { it.name },
        )
    }

    @Test
    fun `delete removes cached entry`() = runTest {
        val api = FakeShopsApi(shopsForFetch = listOf(bakery(), global()))
        val repository = ShopsRepository(api)
        repository.refetch()

        repository.delete(shopId = 1)

        assertEquals(listOf("Global"), repository.shops.first().map { it.name })
        assertTrue(api.deleted)
        assertEquals(1L, api.lastDeleteShopId)
    }

    @Test
    fun `409 on create surfaces error and leaves cache unchanged`() = runTest {
        val api = FakeShopsApi(shopsForFetch = listOf(bakery(), global()))
        val repository = ShopsRepository(api)
        repository.refetch()
        api.createError = httpException(409)

        val thrown = runCatching { repository.create("Alpine") }

        assertTrue(thrown.exceptionOrNull() is HttpException)
        assertEquals(listOf("Global", "Bakery"), repository.shops.first().map { it.name })
    }

    @Test
    fun `422 on rename surfaces error and leaves cache unchanged`() = runTest {
        val api = FakeShopsApi(shopsForFetch = listOf(bakery(), global()))
        val repository = ShopsRepository(api)
        repository.refetch()
        api.renameError = httpException(422)

        val thrown = runCatching { repository.rename(shopId = 1, name = "Zoo") }

        assertTrue(thrown.exceptionOrNull() is HttpException)
        assertEquals(listOf("Global", "Bakery"), repository.shops.first().map { it.name })
    }

    @Test
    fun `403 on delete surfaces error and leaves cache unchanged`() = runTest {
        val api = FakeShopsApi(shopsForFetch = listOf(bakery(), global()))
        val repository = ShopsRepository(api)
        repository.refetch()
        api.deleteResponse = Response.error(403, okhttp3.ResponseBody.create(null, ""))

        val thrown = runCatching { repository.delete(shopId = 4) }

        assertTrue(thrown.exceptionOrNull() is HttpException)
        assertEquals(listOf("Global", "Bakery"), repository.shops.first().map { it.name })
    }
}

private fun shop(id: Long, name: String) = Shop(
    id = id,
    name = name,
    createdAt = "2026-10-22T10:00:00.000Z",
)

private fun bakery() = shop(1, "Bakery")
private fun supermarket() = shop(2, "Supermarket")
private fun global() = shop(4, "Global")

private fun httpException(code: Int): HttpException =
    HttpException(Response.error<Any>(code, okhttp3.ResponseBody.create(null, "")))

private class FakeShopsApi(
    var shopsForFetch: List<Shop>,
) : ShopsApi {
    var createdShop: Shop? = null
    var renamedShop: Shop? = null
    var createError: HttpException? = null
    var renameError: HttpException? = null
    var deleted = false
    var deleteResponse: Response<Unit> = Response.success(Unit)
    var lastCreateRequest: CreateShopRequest? = null
    var lastRenameShopId: Long? = null
    var lastRenameRequest: RenameShopRequest? = null
    var lastDeleteShopId: Long? = null

    override suspend fun listShops(): List<Shop> = shopsForFetch

    override suspend fun createShop(body: CreateShopRequest): Shop {
        lastCreateRequest = body
        createError?.let { throw it }
        return requireNotNull(createdShop)
    }

    override suspend fun renameShop(shopId: Long, body: RenameShopRequest): Shop {
        lastRenameShopId = shopId
        lastRenameRequest = body
        renameError?.let { throw it }
        return requireNotNull(renamedShop)
    }

    override suspend fun deleteShop(shopId: Long): Response<Unit> {
        deleted = true
        lastDeleteShopId = shopId
        return deleteResponse
    }
}

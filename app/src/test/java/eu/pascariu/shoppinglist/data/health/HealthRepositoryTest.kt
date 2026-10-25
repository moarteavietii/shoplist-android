package eu.pascariu.shoppinglist.data.health

import eu.pascariu.shoppinglist.data.network.HealthApi
import eu.pascariu.shoppinglist.data.network.HealthResponse
import eu.pascariu.shoppinglist.testutil.FakeHealthApi
import eu.pascariu.shoppinglist.testutil.httpException
import java.io.IOException
import kotlinx.serialization.SerializationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class HealthRepositoryTest {

    @Test
    fun `maps ok status to healthy`() = runTest {
        val repository = HealthRepository(FakeHealthApi(healthBehavior = { HealthResponse(status = "ok") }))

        val result = repository.refresh()

        assertEquals(HealthStatus.Healthy(message = "ok"), result)
    }

    @Test
    fun `maps unexpected status to unreachable`() = runTest {
        val repository = HealthRepository(FakeHealthApi(healthBehavior = { HealthResponse(status = "degraded") }))

        val result = repository.refresh()

        assertEquals(HealthStatus.Unreachable(reason = "unexpected status: degraded"), result)
    }

    @Test
    fun `maps HTTP error to unreachable`() = runTest {
        val repository = HealthRepository(FakeHealthApi(healthBehavior = { throw httpException(500) }))

        val result = repository.refresh()

        assertEquals(HealthStatus.Unreachable(reason = "HTTP 500"), result)
    }

    @Test
    fun `maps malformed response to unreachable`() = runTest {
        val repository = HealthRepository(FakeHealthApi(healthBehavior = { throw SerializationException("bad json") }))

        val result = repository.refresh()

        assertEquals(HealthStatus.Unreachable(reason = "malformed response"), result)
    }

    @Test
    fun `maps IO failure to unreachable`() = runTest {
        val repository = HealthRepository(FakeHealthApi(healthBehavior = { throw IOException("connection refused") }))

        val result = repository.refresh()

        assertEquals(HealthStatus.Unreachable(reason = "backend unreachable"), result)
    }
}

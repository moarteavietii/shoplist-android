package eu.pascariu.shoppinglist.ui.health

import eu.pascariu.shoppinglist.data.health.HealthRepository
import eu.pascariu.shoppinglist.data.health.HealthStatus
import eu.pascariu.shoppinglist.data.network.HealthApi
import eu.pascariu.shoppinglist.data.network.HealthResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HealthViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `checkHealth reports repository result`() = runTest {
        val viewModel = HealthViewModel(HealthRepository(FakeHealthApi { HealthResponse(status = "degraded") }))

        viewModel.checkHealth()
        advanceUntilIdle()

        assertEquals(
            HealthStatus.Unreachable(reason = "unexpected status: degraded"),
            viewModel.healthStatus.value,
        )
    }

    @Test
    fun `checkHealth replaces stale unreachable state after re-entry`() = runTest {
        val fakeApi = FakeHealthApi { HealthResponse(status = "degraded") }
        val viewModel = HealthViewModel(HealthRepository(fakeApi))

        viewModel.checkHealth()
        advanceUntilIdle()
        assertEquals(
            HealthStatus.Unreachable(reason = "unexpected status: degraded"),
            viewModel.healthStatus.value,
        )

        fakeApi.behavior = { HealthResponse(status = "ok") }
        viewModel.checkHealth()
        advanceUntilIdle()

        assertEquals(HealthStatus.Healthy(message = "ok"), viewModel.healthStatus.value)
    }
}

private class FakeHealthApi(var behavior: () -> HealthResponse) : HealthApi {
    override suspend fun getHealth(): HealthResponse = behavior()

    override suspend fun probeHealth(clientId: String, clientSecret: String): HealthResponse = behavior()
}

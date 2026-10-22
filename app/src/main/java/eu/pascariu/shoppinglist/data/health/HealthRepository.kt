package eu.pascariu.shoppinglist.data.health

import eu.pascariu.shoppinglist.data.network.HealthApi
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.SerializationException
import retrofit2.HttpException

sealed interface HealthStatus {
    data object Loading : HealthStatus
    data class Healthy(val message: String) : HealthStatus
    data class Unreachable(val reason: String) : HealthStatus
}

@Singleton
class HealthRepository @Inject constructor(
    private val healthApi: HealthApi,
) {
    suspend fun refresh(): HealthStatus =
        try {
            val response = healthApi.getHealth()
            if (response.status.equals("ok", ignoreCase = true)) {
                HealthStatus.Healthy(message = response.status)
            } else {
                HealthStatus.Unreachable(reason = "unexpected status: ${response.status}")
            }
        } catch (e: HttpException) {
            HealthStatus.Unreachable(reason = "HTTP ${e.code()}")
        } catch (e: SerializationException) {
            HealthStatus.Unreachable(reason = "malformed response")
        } catch (e: IOException) {
            HealthStatus.Unreachable(reason = "backend unreachable")
        }
}

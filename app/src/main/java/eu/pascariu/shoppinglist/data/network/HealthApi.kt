package eu.pascariu.shoppinglist.data.network

import kotlinx.serialization.Serializable
import retrofit2.http.GET

@Serializable
data class HealthResponse(
    val status: String,
)

interface HealthApi {
    @GET("health")
    suspend fun getHealth(): HealthResponse
}

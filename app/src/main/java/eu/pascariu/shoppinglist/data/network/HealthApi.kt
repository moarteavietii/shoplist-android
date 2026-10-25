package eu.pascariu.shoppinglist.data.network

import eu.pascariu.shoppinglist.data.auth.CloudflareAccessInterceptor
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Header

@Serializable
data class HealthResponse(
    val status: String,
)

interface HealthApi {
    @GET("health")
    suspend fun getHealth(): HealthResponse

    @GET("health")
    suspend fun probeHealth(
        @Header(CloudflareAccessInterceptor.HEADER_CLIENT_ID) clientId: String,
        @Header(CloudflareAccessInterceptor.HEADER_CLIENT_SECRET) clientSecret: String,
    ): HealthResponse
}

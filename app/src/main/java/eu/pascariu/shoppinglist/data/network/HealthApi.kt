package eu.pascariu.shoppinglist.data.network

import eu.pascariu.shoppinglist.data.auth.CloudflareAccessInterceptor
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Header

@Serializable
data class HealthResponse(
    val status: String,
)

@Serializable
class ShopProbeEntry

interface HealthApi {
    @GET("health")
    suspend fun getHealth(): HealthResponse

    @GET("api/v1/shops")
    suspend fun probeShops(
        @Header(CloudflareAccessInterceptor.HEADER_CLIENT_ID) clientId: String,
        @Header(CloudflareAccessInterceptor.HEADER_CLIENT_SECRET) clientSecret: String,
    ): List<ShopProbeEntry>
}

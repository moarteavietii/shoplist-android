package eu.pascariu.shoppinglist.data.network

import eu.pascariu.shoppinglist.data.auth.CloudflareAccessInterceptor
import eu.pascariu.shoppinglist.data.model.Shop
import retrofit2.http.GET
import retrofit2.http.Header

interface HealthApi {
    @GET("api/v1/shops")
    suspend fun probeShops(
        @Header(CloudflareAccessInterceptor.HEADER_CLIENT_ID) clientId: String,
        @Header(CloudflareAccessInterceptor.HEADER_CLIENT_SECRET) clientSecret: String,
    ): List<Shop>
}

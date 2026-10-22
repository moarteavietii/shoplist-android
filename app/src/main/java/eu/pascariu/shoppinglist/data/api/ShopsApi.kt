package eu.pascariu.shoppinglist.data.api

import eu.pascariu.shoppinglist.data.model.CreateShopRequest
import eu.pascariu.shoppinglist.data.model.RenameShopRequest
import eu.pascariu.shoppinglist.data.model.Shop
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface ShopsApi {

    @GET("api/shops")
    suspend fun listShops(): List<Shop>

    @POST("api/shops")
    suspend fun createShop(@Body body: CreateShopRequest): Shop

    @PATCH("api/shops/{shopId}")
    suspend fun renameShop(@Path("shopId") shopId: Long, @Body body: RenameShopRequest): Shop

    @DELETE("api/shops/{shopId}")
    suspend fun deleteShop(@Path("shopId") shopId: Long): Response<Unit>
}

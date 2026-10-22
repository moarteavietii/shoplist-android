package eu.pascariu.shoppinglist.data.api

import eu.pascariu.shoppinglist.data.model.CreateItemRequest
import eu.pascariu.shoppinglist.data.model.Item
import eu.pascariu.shoppinglist.data.model.UpdateItemRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface ItemsApi {

    @GET("api/lists/{listId}/items")
    suspend fun listItems(@Path("listId") listId: Long): List<Item>

    @POST("api/lists/{listId}/items")
    suspend fun createItem(@Path("listId") listId: Long, @Body body: CreateItemRequest): Item

    @PATCH("api/items/{itemId}")
    suspend fun updateItem(@Path("itemId") itemId: Long, @Body body: UpdateItemRequest): Item

    @DELETE("api/items/{itemId}")
    suspend fun deleteItem(@Path("itemId") itemId: Long): Response<Unit>
}

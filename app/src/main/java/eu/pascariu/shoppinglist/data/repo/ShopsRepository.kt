package eu.pascariu.shoppinglist.data.repo

import eu.pascariu.shoppinglist.data.api.ShopsApi
import eu.pascariu.shoppinglist.data.model.CreateShopRequest
import eu.pascariu.shoppinglist.data.model.RenameShopRequest
import eu.pascariu.shoppinglist.data.model.Shop
import eu.pascariu.shoppinglist.domain.search.orderViewsGlobalFirst
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import retrofit2.HttpException

@Singleton
class ShopsRepository @Inject constructor(
    private val shopsApi: ShopsApi,
) {

    private val cache = MutableStateFlow<List<Shop>>(emptyList())
    val shops: StateFlow<List<Shop>> = cache.asStateFlow()

    suspend fun refetch() {
        cache.value = sort(shopsApi.listShops())
    }

    suspend fun create(name: String): Shop {
        val created = shopsApi.createShop(CreateShopRequest(name = name))
        cache.update { sort(it + created) }
        return created
    }

    suspend fun rename(shopId: Long, name: String): Shop {
        val renamed = shopsApi.renameShop(shopId, RenameShopRequest(name = name))
        cache.update { current -> sort(current.map { if (it.id == renamed.id) renamed else it }) }
        return renamed
    }

    suspend fun delete(shopId: Long) {
        val response = shopsApi.deleteShop(shopId)
        if (!response.isSuccessful) throw HttpException(response)
        cache.update { current -> current.filterNot { it.id == shopId } }
    }

    fun applyCreated(shop: Shop) {
        cache.update { current ->
            if (current.any { it.id == shop.id }) current else sort(current + shop)
        }
    }

    fun applyUpdated(shop: Shop) {
        cache.update { current ->
            val existing = current.firstOrNull { it.id == shop.id } ?: return@update current
            if (existing == shop) current else sort(current.map { if (it.id == shop.id) shop else it })
        }
    }

    fun applyDeleted(shopId: Long) {
        cache.update { current -> current.filterNot { it.id == shopId } }
    }

    private fun sort(shops: List<Shop>): List<Shop> = orderViewsGlobalFirst(shops)
}

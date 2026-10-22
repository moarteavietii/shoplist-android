package eu.pascariu.shoppinglist.data.repo

import eu.pascariu.shoppinglist.data.api.ShopsApi
import eu.pascariu.shoppinglist.data.model.CreateShopRequest
import eu.pascariu.shoppinglist.data.model.RenameShopRequest
import eu.pascariu.shoppinglist.data.model.Shop
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
        cache.value = sort(cache.value + created)
        return created
    }

    suspend fun rename(shopId: Long, name: String): Shop {
        val renamed = shopsApi.renameShop(shopId, RenameShopRequest(name = name))
        cache.value = sort(cache.value.map { if (it.id == renamed.id) renamed else it })
        return renamed
    }

    suspend fun delete(shopId: Long) {
        shopsApi.deleteShop(shopId)
        cache.value = cache.value.filterNot { it.id == shopId }
    }

    private fun sort(shops: List<Shop>): List<Shop> =
        shops.sortedWith(
            compareByDescending<Shop> { it.name == GLOBAL_SHOP_NAME }
                .thenBy { it.name.lowercase() }
                .thenBy { it.name },
        )

    private companion object {
        const val GLOBAL_SHOP_NAME = "Global"
    }
}

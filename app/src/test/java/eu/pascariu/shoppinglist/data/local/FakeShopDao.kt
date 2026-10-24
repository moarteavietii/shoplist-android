package eu.pascariu.shoppinglist.data.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeShopDao : ShopDao {

    private val rows = MutableStateFlow<List<ShopEntity>>(emptyList())

    override fun observeAll() = rows.map { list -> list.sortedBy { it.id } }

    override suspend fun getById(shopId: Long): ShopEntity? = rows.value.firstOrNull { it.id == shopId }

    override suspend fun upsertAll(shops: List<ShopEntity>) {
        val mutable = rows.value.toMutableList()
        shops.forEach { entity ->
            mutable.removeAll { it.id == entity.id }
            mutable.add(entity)
        }
        rows.value = mutable
    }

    override suspend fun upsert(shop: ShopEntity) = upsertAll(listOf(shop))

    override suspend fun deleteById(shopId: Long) {
        rows.value = rows.value.filterNot { it.id == shopId }
    }

    override suspend fun clear() {
        rows.value = emptyList()
    }

    override suspend fun replaceAll(shops: List<ShopEntity>) {
        rows.value = shops.toList()
    }
}

package eu.pascariu.shoppinglist.data.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeItemDao : ItemDao {

    private val rows = MutableStateFlow<List<ItemEntity>>(emptyList())

    override fun observeAll() = rows.map { list -> list.sortedBy { it.id } }

    override suspend fun getById(itemId: Long): ItemEntity? = rows.value.firstOrNull { it.id == itemId }

    override suspend fun upsertAll(items: List<ItemEntity>) {
        val mutable = rows.value.toMutableList()
        items.forEach { entity ->
            mutable.removeAll { it.id == entity.id }
            mutable.add(entity)
        }
        rows.value = mutable
    }

    override suspend fun upsert(item: ItemEntity) = upsertAll(listOf(item))

    override suspend fun deleteById(itemId: Long) {
        rows.value = rows.value.filterNot { it.id == itemId }
    }

    override suspend fun clear() {
        rows.value = emptyList()
    }

    override suspend fun replaceAll(items: List<ItemEntity>) {
        rows.value = items.toList()
    }
}

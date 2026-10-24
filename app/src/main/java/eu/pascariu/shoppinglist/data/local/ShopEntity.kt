package eu.pascariu.shoppinglist.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import eu.pascariu.shoppinglist.data.model.Shop

@Entity(tableName = "shops")
data class ShopEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val createdAt: String,
)

fun Shop.toEntity(): ShopEntity = ShopEntity(
    id = id,
    name = name,
    createdAt = createdAt,
)

fun ShopEntity.toModel(): Shop = Shop(
    id = id,
    name = name,
    createdAt = createdAt,
)

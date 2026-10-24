package eu.pascariu.shoppinglist.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import eu.pascariu.shoppinglist.data.model.Item
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey val id: Long,
    val listId: Long,
    val name: String,
    val isChecked: Boolean,
    val orderMapJson: String,
    val quantity: Int,
    val isTemporary: Boolean,
    val checkCount: Int,
    val expiresAt: String?,
    val createdAt: String,
    val visibleInJson: String,
)

private val json = kotlinx.serialization.json.Json

fun Item.toEntity(): ItemEntity = ItemEntity(
    id = id,
    listId = listId,
    name = name,
    isChecked = isChecked,
    orderMapJson = json.encodeToString(orderMap),
    quantity = quantity,
    isTemporary = isTemporary,
    checkCount = checkCount,
    expiresAt = expiresAt,
    createdAt = createdAt,
    visibleInJson = json.encodeToString(visibleIn),
)

fun ItemEntity.toModel(): Item = Item(
    id = id,
    listId = listId,
    name = name,
    isChecked = isChecked,
    orderMap = json.decodeFromString(orderMapJson),
    quantity = quantity,
    isTemporary = isTemporary,
    checkCount = checkCount,
    expiresAt = expiresAt,
    createdAt = createdAt,
    visibleIn = json.decodeFromString(visibleInJson),
)

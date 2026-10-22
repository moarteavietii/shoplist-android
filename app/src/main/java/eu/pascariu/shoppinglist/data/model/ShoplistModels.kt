package eu.pascariu.shoppinglist.data.model

import kotlinx.serialization.Serializable

@JvmInline
value class ShopId(val value: Long)

typealias VisibleIn = List<Long>

typealias OrderMap = Map<String, String>

@Serializable
data class Item(
    val id: Long,
    val listId: Long,
    val name: String,
    val isChecked: Boolean,
    val orderMap: OrderMap,
    val quantity: Int,
    val isTemporary: Boolean,
    val checkCount: Int,
    val expiresAt: String?,
    val createdAt: String,
    val visibleIn: VisibleIn,
)

@Serializable
data class Shop(
    val id: Long,
    val name: String,
    val createdAt: String,
)

@Serializable
data class CreateItemRequest(
    val name: String,
    val visibleIn: VisibleIn? = null,
    val orderMap: OrderMap? = null,
    val quantity: Int? = null,
    val isTemporary: Boolean? = null,
)

@Serializable
data class UpdateItemRequest(
    val name: String? = null,
    val isChecked: Boolean? = null,
    val isUndo: Boolean? = null,
    val orderMap: OrderMap? = null,
    val visibleIn: VisibleIn? = null,
    val quantity: Int? = null,
    val isTemporary: Boolean? = null,
)

@Serializable
data class CreateShopRequest(
    val name: String,
)

@Serializable
data class RenameShopRequest(
    val name: String,
)

@Serializable
data class ErrorPayload(
    val error: String,
)

@Serializable
data class ItemEvent(
    val type: String,
    val listId: Long,
    val item: Item? = null,
    val itemId: Long? = null,
    val originDeviceId: String? = null,
)

@Serializable
data class ShopEvent(
    val type: String,
    val shop: Shop? = null,
    val shopId: Long? = null,
    val originDeviceId: String? = null,
)

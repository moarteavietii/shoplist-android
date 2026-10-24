package eu.pascariu.shoppinglist.data.local

import eu.pascariu.shoppinglist.data.model.CreateItemRequest
import eu.pascariu.shoppinglist.data.model.CreateShopRequest
import eu.pascariu.shoppinglist.data.model.RenameShopRequest
import eu.pascariu.shoppinglist.data.model.UpdateItemRequest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
sealed class MutationPayload {

    @Serializable
    @SerialName("item_create")
    data class ItemCreate(
        val listId: Long,
        val placeholderId: Long,
        val request: CreateItemRequest,
    ) : MutationPayload()

    @Serializable
    @SerialName("item_update")
    data class ItemUpdate(
        val itemId: Long,
        val request: UpdateItemRequest,
    ) : MutationPayload()

    @Serializable
    @SerialName("item_delete")
    data class ItemDelete(
        val itemId: Long,
    ) : MutationPayload()

    @Serializable
    @SerialName("item_check")
    data class ItemCheck(
        val itemId: Long,
        val isChecked: Boolean,
        val isUndo: Boolean? = null,
    ) : MutationPayload()

    @Serializable
    @SerialName("shop_create")
    data class ShopCreate(
        val placeholderId: Long,
        val request: CreateShopRequest,
    ) : MutationPayload()

    @Serializable
    @SerialName("shop_rename")
    data class ShopRename(
        val shopId: Long,
        val request: RenameShopRequest,
    ) : MutationPayload()

    @Serializable
    @SerialName("shop_delete")
    data class ShopDelete(
        val shopId: Long,
    ) : MutationPayload()
}

fun MutationPayload.toJson(json: Json): String = when (this) {
    is MutationPayload.ItemCreate -> json.encodeToString(MutationPayload.serializer(), this)
    is MutationPayload.ItemUpdate -> json.encodeToString(MutationPayload.serializer(), this)
    is MutationPayload.ItemDelete -> json.encodeToString(MutationPayload.serializer(), this)
    is MutationPayload.ItemCheck -> json.encodeToString(MutationPayload.serializer(), this)
    is MutationPayload.ShopCreate -> json.encodeToString(MutationPayload.serializer(), this)
    is MutationPayload.ShopRename -> json.encodeToString(MutationPayload.serializer(), this)
    is MutationPayload.ShopDelete -> json.encodeToString(MutationPayload.serializer(), this)
}

val MutationPayload.kind: MutationKind
    get() = when (this) {
        is MutationPayload.ItemCreate -> MutationKind.ITEM_CREATE
        is MutationPayload.ItemUpdate -> MutationKind.ITEM_UPDATE
        is MutationPayload.ItemDelete -> MutationKind.ITEM_DELETE
        is MutationPayload.ItemCheck -> MutationKind.ITEM_CHECK
        is MutationPayload.ShopCreate -> MutationKind.SHOP_CREATE
        is MutationPayload.ShopRename -> MutationKind.SHOP_RENAME
        is MutationPayload.ShopDelete -> MutationKind.SHOP_DELETE
    }

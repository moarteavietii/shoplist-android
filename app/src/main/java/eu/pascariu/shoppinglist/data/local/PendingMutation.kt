package eu.pascariu.shoppinglist.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MutationKind {
    ITEM_CREATE,
    ITEM_UPDATE,
    ITEM_DELETE,
    ITEM_CHECK,
    SHOP_CREATE,
    SHOP_RENAME,
    SHOP_DELETE,
}

@Entity(tableName = "pending_mutations")
data class PendingMutation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: MutationKind,
    val payloadJson: String,
    val createdAt: Long,
)

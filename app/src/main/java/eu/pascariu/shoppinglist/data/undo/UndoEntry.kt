package eu.pascariu.shoppinglist.data.undo

import kotlinx.serialization.Serializable

@Serializable
data class UndoEntry(
    val itemId: Long,
    val from: Boolean,
    val to: Boolean,
    val at: Long,
)

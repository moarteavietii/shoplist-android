package eu.pascariu.shoppinglist.ui.list

import eu.pascariu.shoppinglist.data.realtime.ToastSeverity

data class ChangeToast(
    val id: Long,
    val text: String,
    val severity: ToastSeverity = ToastSeverity.Info,
)

package com.example.shoplist_android.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Immutable
data class ShoplistShapes(
    val small: Shape,
    val medium: Shape,
    val large: Shape,
)

val ShoplistShapesDefault = ShoplistShapes(
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(16.dp),
)

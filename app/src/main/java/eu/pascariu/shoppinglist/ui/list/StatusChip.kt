package eu.pascariu.shoppinglist.ui.list

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.pascariu.shoppinglist.ui.theme.LocalShoplistColors
import eu.pascariu.shoppinglist.ui.theme.LocalShoplistTypography

private val CHIP_LED_SIZE = 8.dp

@Composable
fun StatusChip(
    label: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalShoplistColors.current
    Row(
        modifier = modifier
            .background(colors.chrome, RoundedCornerShape(50))
            .border(1.dp, colors.border, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(CHIP_LED_SIZE)
                .background(colors.ledOn, CircleShape),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = LocalShoplistTypography.current.label,
            color = colors.content,
        )
    }
}

package eu.pascariu.shoppinglist.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import eu.pascariu.shoppinglist.ui.theme.LocalShoplistColors
import eu.pascariu.shoppinglist.ui.theme.LocalShoplistTypography

enum class ModalButtonStyle { Primary, Danger, Quiet }

@Composable
fun ModalButton(
    label: String,
    style: ModalButtonStyle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = LocalShoplistColors.current
    val typography = LocalShoplistTypography.current

    val containerColor: Color
    val contentColor: Color
    val borderColor: Color
    when (style) {
        ModalButtonStyle.Primary -> {
            containerColor = colors.ledOn
            contentColor = colors.background
            borderColor = Color.Transparent
        }
        ModalButtonStyle.Danger -> {
            containerColor = Color.Transparent
            contentColor = colors.danger
            borderColor = colors.danger
        }
        ModalButtonStyle.Quiet -> {
            containerColor = Color.Transparent
            contentColor = colors.contentMuted
            borderColor = colors.border
        }
    }

    val appliedContainer = if (enabled) containerColor else containerColor.copy(alpha = 0.5f)
    val appliedContent = if (enabled) contentColor else contentColor.copy(alpha = 0.5f)
    val appliedBorder = if (enabled) borderColor else borderColor.copy(alpha = 0.5f)

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 44.dp)
            .background(appliedContainer, RoundedCornerShape(8.dp))
            .border(1.dp, appliedBorder, RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = typography.label,
            color = appliedContent,
        )
    }
}

package eu.pascariu.shoppinglist.ui.health

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eu.pascariu.shoppinglist.data.health.HealthStatus
import eu.pascariu.shoppinglist.ui.theme.LocalShoplistColors
import eu.pascariu.shoppinglist.ui.theme.LocalShoplistTypography

@Composable
fun HealthScreen(
    viewModel: HealthViewModel = hiltViewModel(),
) {
    val colors = LocalShoplistColors.current
    val status by viewModel.healthStatus.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(24.dp),
        contentAlignment = Alignment.BottomStart,
    ) {
        when (val current = status) {
            HealthStatus.Loading -> LedStatusRow(
                dotColor = colors.ledOff,
                text = "checking…",
            )
            is HealthStatus.Healthy -> LedStatusRow(
                dotColor = colors.ledOn,
                text = "backend healthy",
            )
            is HealthStatus.Unreachable -> LedStatusRow(
                dotColor = colors.danger,
                text = "backend unreachable · ${current.reason}",
            )
        }
    }
}

@Composable
private fun LedStatusRow(
    dotColor: Color,
    text: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalShoplistColors.current
    val typography = LocalShoplistTypography.current
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(dotColor, CircleShape),
        )
        Text(
            text = text,
            style = typography.ledDot,
            color = colors.contentMuted,
        )
    }
}

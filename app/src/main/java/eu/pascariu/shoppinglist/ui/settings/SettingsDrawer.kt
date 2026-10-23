package eu.pascariu.shoppinglist.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import eu.pascariu.shoppinglist.ui.drawer.DrawerShell
import eu.pascariu.shoppinglist.ui.theme.LocalShoplistColors
import eu.pascariu.shoppinglist.ui.theme.LocalShoplistShapes
import eu.pascariu.shoppinglist.ui.theme.ShoplistColors
import eu.pascariu.shoppinglist.ui.theme.LocalShoplistTypography
import eu.pascariu.shoppinglist.ui.theme.ShoplistThemePalettes
import eu.pascariu.shoppinglist.ui.theme.ThemeId

@Composable
fun SettingsDrawer(
    visible: Boolean,
    activeThemeId: ThemeId,
    changeToastsEnabled: Boolean,
    onThemeSelected: (ThemeId) -> Unit,
    onChangeToastsToggled: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalShoplistColors.current
    val typography = LocalShoplistTypography.current

    DrawerShell(visible = visible, onDismiss = onDismiss, modifier = modifier) {
        Text(
            text = "Settings",
            style = typography.label,
            color = colors.contentMuted,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = "Theme",
                style = typography.label,
                color = colors.contentFaint,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            ThemeId.entries.forEach { themeId ->
                ThemeRow(
                    themeId = themeId,
                    isActive = themeId == activeThemeId,
                    onSelected = { onThemeSelected(themeId) },
                )
            }

            Text(
                text = "Change toasts",
                style = typography.label,
                color = colors.contentFaint,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button) {
                        onChangeToastsToggled(!changeToastsEnabled)
                    }
                    .padding(vertical = 10.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Show change toasts",
                    style = typography.body,
                    color = colors.content,
                    modifier = Modifier.weight(1f),
                )
                LedDot(filled = changeToastsEnabled)
            }
        }
    }
}

@Composable
private fun ThemeRow(
    themeId: ThemeId,
    isActive: Boolean,
    onSelected: () -> Unit,
) {
    val colors = LocalShoplistColors.current
    val typography = LocalShoplistTypography.current
    val palette = ShoplistThemePalettes.getValue(themeId)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button) { onSelected() }
            .padding(vertical = 10.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ThemeSwatch(palette = palette)
        Text(
            text = themeId.displayName,
            style = typography.body,
            color = if (isActive) colors.ledOn else colors.content,
            modifier = Modifier.weight(1f),
        )
        if (isActive) {
            LedDot(filled = true)
        }
    }
}

@Composable
private fun ThemeSwatch(palette: ShoplistColors) {
    Row {
        SwatchDot(color = palette.background)
        SwatchDot(color = palette.accent)
        SwatchDot(color = palette.content)
    }
}

@Composable
private fun SwatchDot(color: Color) {
    val colors = LocalShoplistColors.current
    Box(
        modifier = Modifier
            .padding(horizontal = 1.dp)
            .size(14.dp)
            .background(color, LocalShoplistShapes.current.small)
            .border(0.5.dp, colors.border, LocalShoplistShapes.current.small),
    )
}

@Composable
private fun LedDot(filled: Boolean) {
    val colors = LocalShoplistColors.current
    Box(
        modifier = Modifier
            .size(8.dp)
            .background(
                color = if (filled) colors.ledOn else Color.Transparent,
                shape = LocalShoplistShapes.current.small,
            ),
    )
}

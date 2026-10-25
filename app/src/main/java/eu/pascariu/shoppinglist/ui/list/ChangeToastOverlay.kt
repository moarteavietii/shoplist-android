package eu.pascariu.shoppinglist.ui.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.pascariu.shoppinglist.data.realtime.ToastSeverity
import eu.pascariu.shoppinglist.ui.theme.LocalShoplistColors
import eu.pascariu.shoppinglist.ui.theme.LocalShoplistTypography
import kotlin.math.abs
import kotlinx.coroutines.launch

private val TOAST_SWIPE_DISMISS_THRESHOLD = 72.dp
private val TOAST_PILL_SHAPE = RoundedCornerShape(50)
private val TOAST_LED_SIZE = 8.dp
private val TOAST_SHADOW_ELEVATION = 6.dp

@Composable
fun ChangeToastOverlay(
    toast: ChangeToast?,
    onDismissed: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var lastToast by remember { mutableStateOf<ChangeToast?>(null) }
    LaunchedEffect(toast) {
        if (toast != null) {
            lastToast = toast
        }
    }

    AnimatedVisibility(
        visible = toast != null,
        enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        lastToast?.let { visibleToast ->
            ToastContent(
                toast = visibleToast,
                onDismissed = onDismissed,
            )
        }
    }
}

@Composable
private fun ToastContent(
    toast: ChangeToast,
    onDismissed: (Long) -> Unit,
) {
    val colors = LocalShoplistColors.current
    val typography = LocalShoplistTypography.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val dismissThresholdPx = with(density) { TOAST_SWIPE_DISMISS_THRESHOLD.toPx() }
    val currentOnDismissed by rememberUpdatedState(onDismissed)
    val dragOffsetX = remember { Animatable(0f) }

    LaunchedEffect(toast.id) {
        dragOffsetX.snapTo(0f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .graphicsLayer {
                    translationX = dragOffsetX.value
                    alpha = 1f - (abs(dragOffsetX.value) / (dismissThresholdPx * 2f))
                        .coerceIn(0f, 0.6f)
                }
                .shadow(elevation = TOAST_SHADOW_ELEVATION, shape = TOAST_PILL_SHAPE)
                .background(colors.surfaceInverse, TOAST_PILL_SHAPE)
                .clickable(
                    role = Role.Button,
                    onClickLabel = "Dismiss notification",
                ) { currentOnDismissed(toast.id) }
                .pointerInput(toast.id) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            val offset = dragOffsetX.value
                            when {
                                abs(offset) >= dismissThresholdPx -> scope.launch {
                                    dragOffsetX.animateTo(
                                        targetValue = if (offset > 0f) 2000f else -2000f,
                                        animationSpec = tween(durationMillis = 180),
                                    )
                                    currentOnDismissed(toast.id)
                                }
                                offset != 0f -> scope.launch { dragOffsetX.animateTo(0f) }
                            }
                        },
                        onDragCancel = {
                            if (dragOffsetX.value != 0f) {
                                scope.launch { dragOffsetX.animateTo(0f) }
                            }
                        },
                    ) { _, dragAmount ->
                        scope.launch { dragOffsetX.snapTo(dragOffsetX.value + dragAmount) }
                    }
                }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ToastLed(
                severity = toast.severity,
                modifier = Modifier.padding(end = 8.dp),
            )
            Text(
                text = toast.text,
                style = typography.body,
                color = colors.contentInverse,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ToastLed(severity: ToastSeverity, modifier: Modifier = Modifier) {
    val colors = LocalShoplistColors.current
    val ledColor = when (severity) {
        ToastSeverity.Info -> colors.ledOn
        ToastSeverity.Danger -> colors.danger
    }
    Box(
        modifier = modifier
            .size(TOAST_LED_SIZE)
            .background(ledColor, CircleShape),
    )
}

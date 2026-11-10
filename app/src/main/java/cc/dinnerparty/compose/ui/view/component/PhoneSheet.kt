package cc.dinnerparty.compose.ui.view.component

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import com.moriafly.salt.ui.SaltTheme
import kotlinx.coroutines.launch
import java.util.concurrent.CancellationException

/** 手机端底部浮层，从底部滑入滑出。 */
@Composable
fun <T : Any> PhoneSheet(
    value: T?,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    modal: Boolean = false,
    content: @Composable (T) -> Unit
) {
    // 滑入滑出动画期间保留内容
    val shown = rememberLastNonNull(value)
    val sheetShape = SaltTheme.shapes.large

    // 返回手势的拖动位移，0 为原位、1 为完全移出屏幕
    val backOffset = remember { Animatable(0f) }
    val dragOffset = remember { Animatable(0f) }
    val scrollState = rememberScrollState()
    var sheetHeight by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    PredictiveBackHandler(enabled = value != null) { progress ->
        try {
            progress.collect { backOffset.snapTo(it.progress) }
            onDismissRequest()
        } catch (_: CancellationException) {
            // 手势取消，回弹到原位
            scope.launch { backOffset.animateTo(0f) }
        }
    }

    // 重新弹出时复位
    LaunchedEffect(value) {
        if (value != null) {
            backOffset.snapTo(0f)
            dragOffset.snapTo(0f)
            scrollState.scrollTo(0)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        // 模态遮罩，点击关闭
        AnimatedVisibility(
            visible = modal && value != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ScrimColor)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismissRequest
                    )
            )
        }

        AnimatedVisibility(
            visible = value != null,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
            modifier = modifier
                .onSizeChanged { sheetHeight = it.height.toFloat() }
                .graphicsLayer {
                    translationY = backOffset.value * size.height + dragOffset.value
                }
        ) {
            shown?.let {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = OverlayEdgeGap)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(bottom = OverlayEdgeGap)
                        .blockTouch()
                        .mapGlass(sheetShape)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = maxHeight * SheetMaxRatio)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(HandleZone)
                                .pointerInput(value) {
                                    if (value == null) return@pointerInput
                                    val tracker = VelocityTracker()
                                    var dragged = 0f
                                    detectVerticalDragGestures(
                                        onDragStart = {
                                            tracker.resetTracking()
                                            dragged = dragOffset.value
                                        },
                                        onVerticalDrag = { change, amount ->
                                            change.consume()
                                            tracker.addPosition(change.uptimeMillis, change.position)
                                            dragged = (dragged + amount).coerceAtLeast(0f)
                                            scope.launch { dragOffset.snapTo(dragged) }
                                        },
                                        onDragEnd = {
                                            val velocity = tracker.calculateVelocity().y
                                            val dismiss = dragged >= sheetHeight * DismissFraction ||
                                                velocity >= DismissVelocity
                                            if (dismiss) onDismissRequest()
                                            else scope.launch { dragOffset.animateTo(0f) }
                                        },
                                        onDragCancel = {
                                            scope.launch { dragOffset.animateTo(0f) }
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = HandleWidth, height = HandleHeight)
                                    .clip(CircleShape)
                                    .background(SaltTheme.colors.subText.copy(alpha = HandleAlpha))
                            )
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(scrollState)
                        ) {
                            content(it)
                        }
                    }
                }
            }
        }
    }
}

/** 手柄可拖动区域高度 */
private val HandleZone = 24.dp

private val HandleWidth = 36.dp

private val HandleHeight = 4.dp

private const val HandleAlpha = 0.35f

private const val DismissFraction = 0.25f

private const val DismissVelocity = 900f

/** 面板最大高度占屏幕比例 */
private const val SheetMaxRatio = 0.92f

/** 手机端底部浮层，内容与数据无关。 */
@Composable
fun PhoneSheet(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    modal: Boolean = false,
    content: @Composable () -> Unit
) {
    PhoneSheet(
        value = if (visible) Unit else null,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        modal = modal
    ) { content() }
}

/** 模态遮罩颜色。 */
private val ScrimColor = Color.Black.copy(alpha = 0.32f)

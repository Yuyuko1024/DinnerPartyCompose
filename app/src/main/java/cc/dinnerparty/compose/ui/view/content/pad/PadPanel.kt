package cc.dinnerparty.compose.ui.view.content.pad

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cc.dinnerparty.compose.ui.view.component.LocalMotion
import cc.dinnerparty.compose.ui.view.component.blockTouch
import cc.dinnerparty.compose.ui.view.component.mapGlass
import cc.dinnerparty.compose.ui.view.component.rememberLastNonNull
import com.moriafly.salt.ui.SaltTheme

/** Pad 端右侧浮层面板，从右侧滑入滑出。 */
@Composable
fun <T : Any> PadPanel(
    value: T?,
    modifier: Modifier = Modifier,
    width: Dp = PadPanelWidth,
    content: @Composable (T) -> Unit
) {
    // 滑入滑出动画期间保留内容
    val shown = rememberLastNonNull(value)
    // 关闭动效时只淡入淡出
    val motion = LocalMotion.current

    AnimatedVisibility(
        visible = value != null,
        enter = if (motion) slideInHorizontally { it } else fadeIn(),
        exit = if (motion) slideOutHorizontally { it } else fadeOut(),
        modifier = modifier
    ) {
        shown?.let {
            Box(
                modifier = Modifier
                    .width(width)
                    .padding(PadPanelPadding)
                    .blockTouch()
                    .mapGlass(SaltTheme.shapes.medium)
                    .verticalScroll(rememberScrollState())
            ) {
                content(it)
            }
        }
    }
}

/** Pad 端右侧浮层面板，内容与数据无关。 */
@Composable
fun PadPanel(
    visible: Boolean,
    modifier: Modifier = Modifier,
    width: Dp = PadPanelWidth,
    content: @Composable () -> Unit
) {
    PadPanel(
        value = if (visible) Unit else null,
        modifier = modifier,
        width = width
    ) { content() }
}

/** 面板默认宽度。 */
private val PadPanelWidth = 360.dp

/** 面板与屏幕边缘的间距。 */
private val PadPanelPadding = 12.dp

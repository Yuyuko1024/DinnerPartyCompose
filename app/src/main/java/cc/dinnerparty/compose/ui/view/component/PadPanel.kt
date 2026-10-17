package cc.dinnerparty.compose.ui.view.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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

    AnimatedVisibility(
        visible = value != null,
        enter = slideInHorizontally { it },
        exit = slideOutHorizontally { it },
        modifier = modifier
    ) {
        shown?.let {
            Box(
                modifier = Modifier
                    .width(width)
                    .padding(PadPanelPadding)
                    .consumeTouchEvents()
                    .clip(PadPanelShape)
                    .background(SaltTheme.colors.subBackground)
                    .border(1.dp, SaltTheme.colors.stroke, PadPanelShape)
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

/** 面板圆角。 */
private val PadPanelShape = RoundedCornerShape(16.dp)

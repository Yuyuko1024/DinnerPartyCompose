package cc.dinnerparty.compose.ui.view.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/** 浮动浮层与屏幕边缘的间距 */
internal val OverlayEdgeGap = 12.dp

/** 阻断命中且不消费事件，避免点击穿透到下层兄弟。 */
internal fun Modifier.blockTouch(): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            awaitPointerEvent()
        }
    }
}

/** 记住最后一次非空的值，滑出动画期间仍有内容可渲染。 */
@Composable
internal fun <T : Any> rememberLastNonNull(value: T?): T? {
    var last by remember { mutableStateOf(value) }
    if (value != null) last = value
    return last
}

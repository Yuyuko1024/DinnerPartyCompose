package cc.dinnerparty.compose.ui.view.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/** 吃掉落在组件上的触摸事件，避免穿透到下层。 */
internal fun Modifier.consumeTouchEvents(): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            awaitPointerEvent().changes.forEach { it.consume() }
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

package cc.dinnerparty.compose.ui.view.component

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
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
    content: @Composable (T) -> Unit
) {
    // 滑入滑出动画期间保留内容
    val shown = rememberLastNonNull(value)

    // 返回手势的拖动位移，0 为原位、1 为完全移出屏幕
    val backOffset = remember { Animatable(0f) }
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
        if (value != null) backOffset.snapTo(0f)
    }

    AnimatedVisibility(
        visible = value != null,
        enter = slideInVertically { it },
        exit = slideOutVertically { it },
        modifier = modifier.graphicsLayer { translationY = backOffset.value * size.height }
    ) {
        shown?.let {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .consumeTouchEvents()
                    .clip(SheetShape)
                    .background(SaltTheme.colors.subBackground)
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                content(it)
            }
        }
    }
}

/** 浮层顶部圆角。 */
private val SheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)

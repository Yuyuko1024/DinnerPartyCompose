package cc.dinnerparty.compose.ui.view.component

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.moriafly.salt.ui.ButtonDefaults
import com.moriafly.salt.ui.ControlSize
import com.moriafly.salt.ui.dialog.DialogDefaults

/** 弹框宽度上限 */
private val GlassDialogWidth = 320.dp

/** 弹框与窗口边缘的间距 */
private val GlassDialogGap = 16.dp

/** 弹框圆角半径 与 SaltUI 弹框一致 */
private val GlassDialogShape = RoundedCornerShape(
    ButtonDefaults.containerHeight(ControlSize.Regular) / 2 + 14.dp
)

/** 液态玻璃弹框容器 内容按 SaltUI 弹框内边距排列 */
@Composable
fun GlassDialog(
    onDismissRequest: () -> Unit,
    dismissOnClickOutside: Boolean = true,
    dismissOnBackPress: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    // 弹框底面在整窗坐标系中的位置 用于判定点外关闭
    val panelBounds = remember { mutableStateOf<Rect?>(null) }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = dismissOnClickOutside,
            dismissOnBackPress = dismissOnBackPress,
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(GlassDialogGap)
                .pointerInput(onDismissRequest, dismissOnClickOutside) {
                    if (!dismissOnClickOutside) return@pointerInput
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val up = waitForUpOrCancellation()
                        val bounds = panelBounds.value
                        if (bounds != null && up != null) {
                            if (!bounds.contains(down.position) && !bounds.contains(up.position)) {
                                onDismissRequest()
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = GlassDialogWidth)
                    .fillMaxWidth()
                    .onGloballyPositioned { panelBounds.value = it.boundsInParent() }
                    .dropShadow(GlassDialogShape, DialogDefaults.ambientShadow)
                    .dropShadow(GlassDialogShape, DialogDefaults.keyShadow)
                    .dialogGlass(GlassDialogShape)
                    .padding(DialogDefaults.contentPadding),
                content = content
            )
        }
    }
}

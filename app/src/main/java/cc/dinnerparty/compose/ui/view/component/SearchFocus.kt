package cc.dinnerparty.compose.ui.view.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

/** 收回搜索框焦点并收起键盘 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun rememberDismiss(): () -> Unit {
    val focus = LocalFocusManager.current
    val ime = LocalSoftwareKeyboardController.current
    val dismiss: () -> Unit = {
        focus.clearFocus()
        ime?.hide()
    }
    return remember(focus, ime) { dismiss }
}

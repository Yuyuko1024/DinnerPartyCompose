package cc.dinnerparty.compose.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import cc.dinnerparty.compose.R
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.Text
import com.moriafly.salt.ui.dialog.YesNoDialog
import com.moriafly.salt.ui.noRippleClickable

// 用户协议与隐私政策地址
private const val USER_AGREEMENT_URL = "https://www.dinnerparty.cc/agreement"
private const val PRIVACY_POLICY_URL = "https://www.dinnerparty.cc/privacy"

/** 首次启动的隐私政策同意弹框。外部点击和返回键都关不掉，只能选同意或退出。 */
@Composable
fun PrivacyConsentDialog(
    onAgree: () -> Unit,
    onDecline: () -> Unit
) {
    val appName = stringResource(R.string.app_name)
    val uriHandler = LocalUriHandler.current

    YesNoDialog(
        onDismissRequest = onDecline,
        onConfirm = onAgree,
        title = stringResource(R.string.privacy_dialog_title),
        content = stringResource(R.string.privacy_dialog_content, appName),
        drawContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                PolicyLink(text = stringResource(R.string.privacy_terms_user_agreement)) {
                    uriHandler.openUriSafely(USER_AGREEMENT_URL)
                }
                PolicyLink(text = stringResource(R.string.privacy_terms_privacy_policy)) {
                    uriHandler.openUriSafely(PRIVACY_POLICY_URL)
                }
            }
        },
        cancelText = stringResource(R.string.privacy_action_decline),
        confirmText = stringResource(R.string.privacy_action_agree),
        properties = DialogProperties(
            dismissOnClickOutside = false,
            dismissOnBackPress = false
        )
    )
}

/** 条款链接，打开失败时静默忽略。 */
@Composable
private fun PolicyLink(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        modifier = Modifier.noRippleClickable(onClick = onClick),
        color = SaltTheme.colors.highlight
    )
}

/** 捕获 `openUri` 抛出的异常。 */
private fun UriHandler.openUriSafely(uri: String) {
    runCatching { openUri(uri) }
}

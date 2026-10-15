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

// TODO 站点还没有这两个页面，路径先按下面的约定写死；上线或改地址时只改这里。
private const val USER_AGREEMENT_URL = "https://www.dinnerparty.cc/agreement"
private const val PRIVACY_POLICY_URL = "https://www.dinnerparty.cc/privacy"

/**
 * 首次启动的隐私政策同意弹框。
 *
 * 用户同意之前不能创建任何地图对象（高德合规要求，见 `agreeAmapPrivacy`），
 * 所以这个弹框是进地图的硬门禁：外部点击和返回键都不允许关掉，
 * 想用只能点「同意并继续」，不想用就点「不同意」退出。
 */
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

/** 条款链接。设备上没有浏览器等可处理的应用时静默失败，不打断用户。 */
@Composable
private fun PolicyLink(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        modifier = Modifier.noRippleClickable(onClick = onClick),
        color = SaltTheme.colors.highlight
    )
}

/** `openUri` 在没有应用能处理该 uri 时会抛异常，这里吞掉——打不开条款不该让 App 崩。 */
private fun UriHandler.openUriSafely(uri: String) {
    runCatching { openUri(uri) }
}

package cc.dinnerparty.compose.ui.view.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cc.dinnerparty.compose.R
import com.moriafly.salt.ui.Text

/** 首次启动的隐私政策同意弹框 外部点击和返回键都关不掉 只能选同意或退出 */
@Composable
fun PrivacyConsentDialog(
    onAgree: () -> Unit,
    onDecline: () -> Unit
) {
    val appName = stringResource(R.string.app_name)
    val uriHandler = LocalUriHandler.current

    YesNoContentDialog(
        title = stringResource(R.string.privacy_dialog_title, appName),
        onDismissRequest = onDecline,
        onConfirm = onAgree,
        dismissOnClickOutside = false,
        dismissOnBackPress = false,
        cancelText = stringResource(R.string.privacy_action_decline),
        confirmText = stringResource(R.string.privacy_action_agree),
    ) {
        Text(
            text = stringResource(R.string.privacy_dialog_content),
            modifier = Modifier.padding(bottom = 16.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            /*PolicyLink(text = stringResource(R.string.privacy_terms_user_agreement)) {
                uriHandler.openUriSafely(USER_AGREEMENT_URL)
            }*/
            PolicyLink(text = stringResource(R.string.privacy_terms_privacy_policy)) {
                uriHandler.openUriSafely(PRIVACY_POLICY_URL)
            }

            PolicyLink(text = stringResource(R.string.privacy_terms_privacy_policy_amap)) {
                uriHandler.openUriSafely(PRIVACY_POLICY_AMAP_URL)
            }
        }
    }
}


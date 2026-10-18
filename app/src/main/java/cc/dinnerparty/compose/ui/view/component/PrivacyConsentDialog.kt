package cc.dinnerparty.compose.ui.view.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import cc.dinnerparty.compose.R
import com.moriafly.salt.ui.dialog.YesNoDialog

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
        title = stringResource(R.string.privacy_dialog_title, appName),
        content = stringResource(R.string.privacy_dialog_content),
        drawContent = {
            Row(
                modifier = Modifier.padding(16.dp, 0.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
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
        },
        cancelText = stringResource(R.string.privacy_action_decline),
        confirmText = stringResource(R.string.privacy_action_agree),
        properties = DialogProperties(
            dismissOnClickOutside = false,
            dismissOnBackPress = false
        )
    )
}


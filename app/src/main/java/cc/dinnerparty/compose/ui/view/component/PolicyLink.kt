package cc.dinnerparty.compose.ui.view.component

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.UriHandler
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.Text

/** 用户协议地址。 */
internal const val USER_AGREEMENT_URL = "https://www.dinnerparty.cc/agreement"

/** 隐私政策地址。 */
internal const val PRIVACY_POLICY_URL = "https://www.dinnerparty.cc/privacy"

/** 高德隐私政策地址。 */
internal const val PRIVACY_POLICY_AMAP_URL = "https://lbs.amap.com/pages/privacy/"

/** 条款链接，点击后交给外部处理。 */
@Composable
internal fun PolicyLink(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        modifier = Modifier.clickable(onClick = onClick),
        color = SaltTheme.colors.highlight
    )
}

/** 打开地址，失败时静默忽略。 */
internal fun UriHandler.openUriSafely(uri: String) {
    runCatching { openUri(uri) }
}

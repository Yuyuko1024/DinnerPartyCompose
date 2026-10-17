package cc.dinnerparty.compose.ui.view.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.dinnerparty.compose.BuildConfig
import cc.dinnerparty.compose.R
import com.moriafly.salt.ui.Icon
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.Text

/** 关于页内容，应用标识、版本号与隐私条款链接。 */
@Composable
fun AboutContent(
    modifier: Modifier = Modifier,
    onClose: () -> Unit = {}
) {
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(AboutPadding)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.mipmap.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(AppLogoSize)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.app_name),
                modifier = Modifier.weight(1f),
                color = SaltTheme.colors.text,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Box(
                modifier = Modifier
                    .size(CloseButtonSize)
                    .clip(CircleShape)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.about_close),
                    modifier = Modifier.size(18.dp),
                    tint = SaltTheme.colors.subText
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Column {
            Text(
                text = stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
                color = SaltTheme.colors.subText,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                PolicyLink(text = stringResource(R.string.privacy_terms_privacy_policy)) {
                    uriHandler.openUriSafely(PRIVACY_POLICY_URL)
                }
                PolicyLink(text = stringResource(R.string.privacy_terms_privacy_policy_amap)) {
                    uriHandler.openUriSafely(PRIVACY_POLICY_AMAP_URL)
                }
            }
        }
    }
}

private val AboutPadding = 16.dp
private val AppLogoSize = 48.dp
private val CloseButtonSize = 24.dp

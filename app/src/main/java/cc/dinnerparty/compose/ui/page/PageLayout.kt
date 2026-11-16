package cc.dinnerparty.compose.ui.page

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moriafly.salt.ui.Button
import com.moriafly.salt.ui.Icon
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.Text
import com.moriafly.salt.ui.UnstableSaltUiApi

/** Pad 端页面内容宽度占屏幕的比例 */
internal const val PadWidthRatio = 0.6f

internal val PagePadding = 16.dp

internal val CardPadding = 8.dp

internal val RowPadY = 10.dp

private val BackIconSize = 18.dp

/** 全屏页顶栏 大标题与返回按钮 */
@UnstableSaltUiApi
@Composable
internal fun PageHeader(
    title: String,
    backText: String,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = PagePadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            color = SaltTheme.colors.text,
            style = SaltTheme.textStyles.largeTitle,
        )
        Button(
            onClick = onBack,
            text = backText,
            leadingIcon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(BackIconSize),
                    tint = SaltTheme.colors.onHighlight,
                )
            },
        )
    }
}

/** 页面标题下的路径说明 */
@UnstableSaltUiApi
@Composable
internal fun PagePath(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(bottom = PagePadding),
        color = SaltTheme.colors.subText,
        style = SaltTheme.textStyles.sub,
    )
}

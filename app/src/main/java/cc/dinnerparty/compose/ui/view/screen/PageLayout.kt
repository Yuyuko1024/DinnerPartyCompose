package cc.dinnerparty.compose.ui.view.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cc.dinnerparty.compose.R
import cc.dinnerparty.compose.ui.adaptive.LocalIsTablet
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

/** 全屏页标题下的路径与返回文案 */
internal class PageInfo(val path: String, val backText: String)

/** 全屏页骨架 手机占满宽 Pad 居中占六成 */
@UnstableSaltUiApi
@Composable
internal fun PageScaffold(
    info: PageInfo,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val isTablet = LocalIsTablet.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SaltTheme.colors.background)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxHeight()
                .fillMaxWidth(if (isTablet) PadWidthRatio else 1f)
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = PagePadding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = PagePadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.settings_page_title),
                    modifier = Modifier.weight(1f),
                    color = SaltTheme.colors.text,
                    style = SaltTheme.textStyles.largeTitle,
                )
                Button(
                    onClick = onBack,
                    text = info.backText,
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
            Text(
                text = info.path,
                modifier = Modifier.padding(bottom = PagePadding),
                color = SaltTheme.colors.subText,
                style = SaltTheme.textStyles.sub,
            )
            content()
        }
    }
}

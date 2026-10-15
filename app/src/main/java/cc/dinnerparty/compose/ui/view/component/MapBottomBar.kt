package cc.dinnerparty.compose.ui.view.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.dinnerparty.compose.R
import com.moriafly.salt.ui.Icon
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.Text
import com.moriafly.salt.ui.noRippleClickable

/** 单个底栏条目的高度（Web `.fm-map-toolbar` 里按钮 `min-height:58`）。 */
private val BottomBarItemHeight = 58.dp

/**
 * 地图页底部操作栏。
 *
 * 对应 Web 端 `.fm-map-toolbar`：顶边 1px 描边 + 表面色背景，
 * 5 个等宽条目——更多 / 收藏 / 添加 / 顺路吃 / 账号。
 * 其中「添加」恒用主色（Web 中它是常驻强调项，非选中态）。
 *
 * 目前只有视觉骨架，回调均为空实现，等接口接入后再填逻辑。
 *
 * 应用是 edge-to-edge 的，所以背景自身一直铺到屏幕最底边（盖住系统导航栏那块），
 * 只让内容条避开导航栏——否则底边会露出一条地图。
 *
 * @param accountName 留空则显示未登录文案。
 */
@Composable
fun MapBottomBar(
    modifier: Modifier = Modifier,
    accountName: String? = null,
    onMoreClick: () -> Unit = {},
    onFavoritesClick: () -> Unit = {},
    onAddClick: () -> Unit = {},
    onAlongRouteClick: () -> Unit = {},
    onAccountClick: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SaltTheme.colors.subBackground)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(SaltTheme.colors.stroke)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            BottomBarItem(
                imageVector = Icons.Outlined.MoreHoriz,
                text = stringResource(R.string.action_more),
                onClick = onMoreClick
            )
            BottomBarItem(
                imageVector = Icons.Outlined.FavoriteBorder,
                text = stringResource(R.string.action_favorites),
                onClick = onFavoritesClick
            )
            BottomBarItem(
                imageVector = Icons.Outlined.AddCircleOutline,
                text = stringResource(R.string.action_add),
                onClick = onAddClick,
                iconSize = 27.dp,
                highlighted = true
            )
            BottomBarItem(
                imageVector = Icons.AutoMirrored.Outlined.DirectionsWalk,
                text = stringResource(R.string.action_along_route),
                onClick = onAlongRouteClick
            )
            AccountItem(
                text = accountName ?: stringResource(R.string.map_account_guest),
                onClick = onAccountClick
            )
        }
    }
}

@Composable
private fun RowScope.BottomBarItem(
    imageVector: ImageVector,
    text: String,
    onClick: () -> Unit,
    iconSize: Dp = 24.dp,
    highlighted: Boolean = false
) {
    val color = if (highlighted) {
        SaltTheme.colors.highlight
    } else {
        SaltTheme.colors.subText.copy(alpha = 0.5f)
    }

    Column(
        modifier = Modifier
            .weight(1f)
            .height(BottomBarItemHeight)
            .noRippleClickable(onClick = onClick),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            tint = color
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = text,
            color = color,
            fontSize = 11.sp,
            maxLines = 1
        )
    }
}

/** 账号条目：圆形头像 + 用户名，头像资源接入前用 Person 图标占位。 */
@Composable
private fun RowScope.AccountItem(
    text: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .height(BottomBarItemHeight)
            .noRippleClickable(onClick = onClick),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(SaltTheme.colors.highlight),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = SaltTheme.colors.onHighlight
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = text,
            color = SaltTheme.colors.subText.copy(alpha = 0.5f),
            fontSize = 11.sp,
            maxLines = 1
        )
    }
}

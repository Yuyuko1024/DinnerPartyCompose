package cc.dinnerparty.compose.ui.view.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.dinnerparty.compose.R
import cc.dinnerparty.compose.api.model.User
import cc.dinnerparty.compose.ui.theme.dinnerPartySemanticColors
import com.moriafly.salt.ui.Icon
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.Text

/** 单个底栏条目的高度。 */
private val BottomBarItemHeight = 58.dp

/**
 * 地图页底部操作栏，含更多 / 收藏 / 添加 / 顺路吃 / 账号五个条目，其中「添加」恒用主色。
 *
 * @param user 已登录的住民，null 表示未登录。
 */
@Composable
fun MapBottomBar(
    modifier: Modifier = Modifier,
    user: User? = null,
    avatarVersion: Long = 0L,
    onMoreClick: () -> Unit = {},
    onFavoritesClick: () -> Unit = {},
    onAddClick: () -> Unit = {},
    onAlongRouteClick: () -> Unit = {},
    onAccountClick: () -> Unit = {},
    adding: Boolean = false,
) {
    val actions = phoneMapActions(
        onMoreClick = onMoreClick,
        onFavoritesClick = onFavoritesClick,
        onAddClick = onAddClick,
        onAlongRouteClick = onAlongRouteClick,
        adding = adding,
    )

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
            actions.forEach { action -> BottomBarItem(action) }
            AccountItem(
                user = user,
                avatarVersion = avatarVersion,
                onClick = onAccountClick
            )
        }
    }
}

@Composable
private fun RowScope.BottomBarItem(action: MapAction) {
    val color = when {
        action.danger -> dinnerPartySemanticColors().danger
        action.highlighted -> SaltTheme.colors.highlight
        else -> SaltTheme.colors.subText.copy(alpha = 0.5f)
    }

    Column(
        modifier = Modifier
            .weight(1f)
            .height(BottomBarItemHeight)
            .clickable(onClick = action.onClick),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        action.imageVector?.let { imageVector ->
            Icon(
                imageVector = imageVector,
                contentDescription = null,
                modifier = Modifier.size(action.iconSize),
                tint = color
            )
        }
        action.iconRes?.let {
            Icon(
                imageVector = ImageVector.vectorResource(it),
                contentDescription = null,
                modifier = Modifier.size(action.iconSize),
                tint = color
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = action.label,
            color = color,
            fontSize = 11.sp,
            maxLines = 1
        )
    }
}

/** 账号条目，圆形头像加用户名。 */
@Composable
private fun RowScope.AccountItem(
    user: User?,
    avatarVersion: Long,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .height(BottomBarItemHeight)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (user == null) {
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
        } else {
            UserAvatar(user = user, avatarVersion = avatarVersion)
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = user?.username ?: stringResource(R.string.map_account_guest),
            color = if (user == null) {
                SaltTheme.colors.subText.copy(alpha = 0.5f)
            } else {
                SaltTheme.colors.subText
            },
            fontSize = 11.sp,
            maxLines = 1
        )
    }
}

package cc.dinnerparty.compose.ui.view.content

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cc.dinnerparty.compose.R
import cc.dinnerparty.compose.api.model.User
import cc.dinnerparty.compose.ui.view.component.UserAvatar
import com.moriafly.salt.ui.Item
import com.moriafly.salt.ui.ItemArrowType
import com.moriafly.salt.ui.ItemDivider
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.Text
import com.moriafly.salt.ui.UnstableSaltUiApi

/** 住民菜单的操作 */
class AccountOps(
    val onLogout: () -> Unit = {},
    val onSettings: () -> Unit = {},
    val onProfile: () -> Unit = {},
)

/** 已登录的住民菜单 */
@UnstableSaltUiApi
@Composable
fun AccountContent(
    modifier: Modifier = Modifier,
    ops: AccountOps = AccountOps(),
    user: User? = null,
    avatarVersion: Long = 0L,
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            user?.let {
                UserAvatar(
                    user = it,
                    size = HeaderAvatarSize,
                    avatarVersion = avatarVersion
                )
            }
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    text = user?.username ?: "-",
                    style = SaltTheme.textStyles.main
                )
                Text(
                    text = stringResource(R.string.account_verified),
                    color = SaltTheme.colors.subText,
                    style = SaltTheme.textStyles.sub
                )
            }
        }
        Item(
            text = stringResource(R.string.account_my_places),
            arrowType = ItemArrowType.None,
            onClick = {},
        )
        Item(
            text = stringResource(R.string.account_my_profile),
            arrowType = ItemArrowType.None,
            onClick = ops.onProfile,
        )
        Item(
            text = stringResource(R.string.account_settings),
            arrowType = ItemArrowType.None,
            onClick = ops.onSettings,
        )
        ItemDivider()
        Item(
            textColor = SaltTheme.colors.error,
            text = stringResource(R.string.account_logout),
            arrowType = ItemArrowType.None,
            onClick = ops.onLogout,
        )
    }
}

private val HeaderAvatarSize = 48.dp
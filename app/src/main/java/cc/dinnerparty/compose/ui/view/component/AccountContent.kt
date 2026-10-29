package cc.dinnerparty.compose.ui.view.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cc.dinnerparty.compose.R
import cc.dinnerparty.compose.api.model.User
import com.moriafly.salt.ui.Item
import com.moriafly.salt.ui.ItemArrow
import com.moriafly.salt.ui.ItemArrowType
import com.moriafly.salt.ui.RoundedColumn
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.Text
import com.moriafly.salt.ui.UnstableSaltUiApi
import com.moriafly.salt.ui.enabledAlpha
import com.moriafly.salt.ui.innerPadding

@UnstableSaltUiApi
@Composable
fun AccountContent(
    modifier: Modifier = Modifier,
    onUpdateAvatar: () -> Unit,
    onUpdateUsername: () -> Unit,
    onUpdatePassword: () -> Unit,
    onExitLogin: () -> Unit,
    onDeleteUser: () -> Unit,
    user: User?,
    avatarVersion: Long = 0L
) {
    RoundedColumn(
        modifier = Modifier.fillMaxWidth(),
        paddingValues = PaddingValues(
            horizontal = 0.dp,
            vertical = 0.dp
        ),
        color = Color.Transparent
    ) {
        // 头像
        Row(
            modifier = Modifier.fillMaxWidth()
                .heightIn(SaltTheme.dimens.item)
                .enabledAlpha(true)
                .clickable {
                    onUpdateAvatar()
                }
                .innerPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            user?.let {
                UserAvatar(
                    user = it,
                    size = 48.dp,
                    avatarVersion = avatarVersion
                )
            }
            Text(
                text = stringResource(R.string.account_change_avatar),
                modifier = Modifier.weight(1f).padding(8.dp, 0.dp)
            )
            ItemArrow(ItemArrowType.Arrow)
        }

        Item(
            text = stringResource(R.string.account_current_username, user?.username ?: "-"),
            onClick = {
                onUpdateUsername()
            }
        )
        Item(
            text = stringResource(R.string.account_change_password),
            onClick = {
                onUpdatePassword()
            }
        )
        Item(
            text = stringResource(R.string.account_logout_title),
            sub = stringResource(R.string.account_logout_sub),
            onClick = {
                onExitLogin()
            }
        )
        Item(
            textColor = ErrorColor,
            text = stringResource(R.string.account_delete_title),
            sub = stringResource(R.string.account_delete_sub),
            onClick = {
                onDeleteUser()
            }
        )
    }
}

private val ErrorColor = Color(0xFFD9534F)
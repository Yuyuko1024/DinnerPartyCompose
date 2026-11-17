package cc.dinnerparty.compose.ui.page

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cc.dinnerparty.compose.R
import cc.dinnerparty.compose.api.model.User
import cc.dinnerparty.compose.ui.view.component.UserAvatar
import com.moriafly.salt.ui.Button
import com.moriafly.salt.ui.ButtonAppearance
import com.moriafly.salt.ui.ButtonIntent
import com.moriafly.salt.ui.Item
import com.moriafly.salt.ui.ItemArrow
import com.moriafly.salt.ui.ItemArrowType
import com.moriafly.salt.ui.ItemDivider
import com.moriafly.salt.ui.RoundedColumn
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.Text
import com.moriafly.salt.ui.UnstableSaltUiApi

/** 住民设置页的数据 */
class SettingsPageUi(
    val user: User? = null,
    val avatarVersion: Long = 0L,
)

/** 住民设置页的操作 */
class SettingsPageOps(
    val onBack: () -> Unit = {},
    val onAvatar: () -> Unit = {},
    val onUsername: () -> Unit = {},
    val onPassword: () -> Unit = {},
    val onLogoutAll: () -> Unit = {},
    val onDelete: () -> Unit = {},
)

/** 卡片里需要按钮的行 */
private class ActionSpec(
    val text: String,
    val hint: String,
    val button: String,
    val danger: Boolean = false,
)

/** 住民设置全屏页 */
@UnstableSaltUiApi
@Composable
fun SettingsPage(
    ui: SettingsPageUi,
    ops: SettingsPageOps,
    modifier: Modifier = Modifier,
) {
    PageScaffold(
        info = PageInfo(
            path = stringResource(R.string.settings_page_path),
            backText = stringResource(R.string.settings_back),
        ),
        onBack = ops.onBack,
        modifier = modifier,
    ) {
        RoundedColumn(
            modifier = Modifier.padding(bottom = PagePadding),
            paddingValues = PaddingValues(vertical = CardPadding),
        ) {
            AvatarRow(ui = ui, onAvatar = ops.onAvatar)
            ItemDivider()
            Item(
                text = stringResource(
                    R.string.settings_name,
                    ui.user?.username.orEmpty()
                ),
                arrowType = ItemArrowType.Arrow,
                onClick = ops.onUsername,
            )
            ItemDivider()
            Item(
                text = stringResource(R.string.settings_password),
                arrowType = ItemArrowType.Arrow,
                onClick = ops.onPassword,
            )
            ItemDivider()
            Item(
                text = stringResource(R.string.settings_theme),
                arrowType = ItemArrowType.Arrow,
                onClick = {},
            )
            ItemDivider()
            Item(
                text = stringResource(R.string.settings_skin),
                arrowType = ItemArrowType.Arrow,
                onClick = {},
            )
            ItemDivider()
            Item(
                text = stringResource(R.string.settings_map),
                arrowType = ItemArrowType.Arrow,
                onClick = {},
            )
            ItemDivider()
            ActionRow(
                spec = ActionSpec(
                    text = stringResource(R.string.settings_logout_all),
                    hint = stringResource(R.string.settings_logout_all_hint),
                    button = stringResource(R.string.settings_logout_all_button),
                ),
                onClick = ops.onLogoutAll,
            )
            ItemDivider()
            ActionRow(
                spec = ActionSpec(
                    text = stringResource(R.string.settings_delete),
                    hint = stringResource(R.string.settings_delete_hint),
                    button = stringResource(R.string.settings_delete_button),
                    danger = true,
                ),
                onClick = ops.onDelete,
            )
        }
    }
}

/** 头像行 头像靠右显示 */
@UnstableSaltUiApi
@Composable
private fun AvatarRow(ui: SettingsPageUi, onAvatar: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = SaltTheme.dimens.item)
            .clickable(onClick = onAvatar)
            .padding(horizontal = SaltTheme.dimens.padding, vertical = RowPadY),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.settings_avatar),
            modifier = Modifier.weight(1f),
            color = SaltTheme.colors.text,
        )
        ui.user?.let {
            UserAvatar(user = it, size = AvatarSize, avatarVersion = ui.avatarVersion)
        }
        Spacer(modifier = Modifier.width(4.dp))
        ItemArrow(arrowType = ItemArrowType.Arrow)
    }
}

/** 标题加说明 右侧带按钮的行 */
@UnstableSaltUiApi
@Composable
private fun ActionRow(spec: ActionSpec, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SaltTheme.dimens.padding, vertical = RowPadY),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = spec.text,
                color = if (spec.danger) SaltTheme.colors.error else SaltTheme.colors.text,
            )
            Text(
                text = spec.hint,
                color = SaltTheme.colors.subText,
                style = SaltTheme.textStyles.sub,
            )
        }
        Button(
            onClick = onClick,
            modifier = Modifier.padding(start = 12.dp),
            text = spec.button,
            appearance = if (spec.danger) ButtonAppearance.Filled else ButtonAppearance.Subtle,
            intent = if (spec.danger) ButtonIntent.Destructive else ButtonIntent.Normal,
        )
    }
}

/** 头像行右侧头像尺寸 */
private val AvatarSize = 40.dp

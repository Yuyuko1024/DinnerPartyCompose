package cc.dinnerparty.compose.ui.view.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cc.dinnerparty.compose.R
import cc.dinnerparty.compose.ui.theme.dinnerPartyAccents
import cc.dinnerparty.compose.ui.theme.hexText
import cc.dinnerparty.compose.ui.view.component.AccentDialog
import cc.dinnerparty.compose.ui.view.component.isGlassOn
import com.moriafly.salt.ui.ItemArrow
import com.moriafly.salt.ui.ItemArrowType
import com.moriafly.salt.ui.ItemDivider
import com.moriafly.salt.ui.ItemSwitcher
import com.moriafly.salt.ui.RoundedColumn
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.Text
import com.moriafly.salt.ui.UnstableSaltUiApi
import com.moriafly.salt.ui.innerPadding

/** 色块直径 */
private val SwatchSize = 20.dp

/** 取色目标 */
private enum class ColorSlot { Primary, Secondary }

/** 个性化主题页的开关 */
enum class ThemeSwitch { Dark, FollowSystem, Glass }

/** 个性化主题页的数据 */
class ThemePageUi(
    val darkTheme: Boolean = false,
    val followSystem: Boolean = true,
    val liquidGlass: Boolean = true,
)

/** 个性化主题页的操作 */
class ThemePageOps(
    val onBack: () -> Unit = {},
    val onSwitch: (ThemeSwitch, Boolean) -> Unit = { _, _ -> },
    val onPrimary: (Int?) -> Unit = {},
    val onSecondary: (Int?) -> Unit = {},
)

/** 个性化主题全屏页 */
@UnstableSaltUiApi
@Composable
fun ThemePage(
    ui: ThemePageUi,
    ops: ThemePageOps,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val glassOn = isGlassOn()
    val systemDark = isSystemInDarkTheme()
    val accents = dinnerPartyAccents()
    var picking by remember { mutableStateOf<ColorSlot?>(null) }

    PageScaffold(
        info = PageInfo(
            path = stringResource(R.string.theme_page_path),
            backText = stringResource(R.string.back_to_settings),
        ),
        onBack = ops.onBack,
        modifier = modifier,
    ) {
        RoundedColumn(
            modifier = Modifier.padding(bottom = PagePadding),
            paddingValues = PaddingValues(vertical = CardPadding),
        ) {
            ThemeColorRow(
                text = stringResource(R.string.theme_primary),
                color = accents.primary,
                onClick = { picking = ColorSlot.Primary },
            )
            ItemDivider()
            ThemeColorRow(
                text = stringResource(R.string.theme_secondary),
                color = accents.secondary,
                onClick = { picking = ColorSlot.Secondary },
            )
            ItemDivider()
            ItemSwitcher(
                state = ui.followSystem,
                onChange = { enabled ->
                    if (!enabled) ops.onSwitch(ThemeSwitch.Dark, systemDark)
                    ops.onSwitch(ThemeSwitch.FollowSystem, enabled)
                },
                text = stringResource(R.string.theme_follow_system),
            )
            ItemDivider()
            ItemSwitcher(
                state = if (ui.followSystem) systemDark else ui.darkTheme,
                onChange = { enabled -> ops.onSwitch(ThemeSwitch.Dark, enabled) },
                text = stringResource(R.string.theme_dark),
                enabled = !ui.followSystem,
            )
            ItemDivider()
            ItemSwitcher(
                state = ui.liquidGlass,
                onChange = { enabled ->
                    if (enabled && !glassOn) {
                        Toast.makeText(
                            context,
                            R.string.settings_glass_unsupported,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    ops.onSwitch(ThemeSwitch.Glass, enabled)
                },
                text = stringResource(
                    if (glassOn) R.string.settings_glass else R.string.settings_blur
                ),
            )
        }
    }

    val slot = picking
    if (slot != null) {
        val isPrimary = slot == ColorSlot.Primary
        AccentDialog(
            title = stringResource(
                if (isPrimary) R.string.theme_primary else R.string.theme_secondary
            ),
            accent = if (isPrimary) accents.primary else accents.secondary,
            onDismiss = { picking = null },
            onPick = { color ->
                picking = null
                val argb = color?.toArgb()
                if (isPrimary) ops.onPrimary(argb) else ops.onSecondary(argb)
            },
        )
    }
}

/** 主题色一行 右侧显示当前色块与色值 */
@Composable
private fun ThemeColorRow(text: String, color: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = SaltTheme.dimens.item)
            .clickable(onClick = onClick)
            .innerPadding(vertical = false),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .size(SwatchSize)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(SaltTheme.dimens.subPadding))
        Text(
            text = color.hexText(),
            color = SaltTheme.colors.subText,
            style = SaltTheme.textStyles.sub,
        )
        ItemArrow(arrowType = ItemArrowType.Arrow)
    }
}

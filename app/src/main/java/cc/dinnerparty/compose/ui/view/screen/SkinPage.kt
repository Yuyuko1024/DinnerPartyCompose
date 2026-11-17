package cc.dinnerparty.compose.ui.view.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.dinnerparty.compose.R
import cc.dinnerparty.compose.ui.theme.dinnerPartyAccents
import com.moriafly.salt.ui.Button
import com.moriafly.salt.ui.Icon
import com.moriafly.salt.ui.ItemEdit
import com.moriafly.salt.ui.RoundedColumn
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.Text
import com.moriafly.salt.ui.UnstableSaltUiApi

/** 皮肤预览区宽高比 */
private const val PreviewRatio = 1.5f

/** 皮肤卡片圆角 */
private val SkinCorner = 16.dp

/** 皮肤卡片内边距 */
private val SkinPadding = 10.dp

/** 预览区圆角 */
private val PreviewCorner = 10.dp

/** 预览区图标尺寸 */
private val PreviewIcon = 32.dp

/** 区块之间的间距 */
private val SectionGap = 20.dp

/** 标题与内容的间距 */
private val TitleGap = 10.dp

/** 主题皮肤页的操作 */
class SkinPageOps(
    val onBack: () -> Unit = {},
    /** 兑换码提交 参数为用户输入的原样内容 */
    val onRedeem: (String) -> Unit = {},
)

/** 主题皮肤全屏页 */
@UnstableSaltUiApi
@Composable
fun SkinPage(
    ops: SkinPageOps,
    modifier: Modifier = Modifier,
) {
    var code by remember { mutableStateOf("") }

    PageScaffold(
        info = PageInfo(
            path = stringResource(R.string.skin_page_path),
            backText = stringResource(R.string.back_to_settings),
        ),
        onBack = ops.onBack,
        modifier = modifier,
    ) {
        Text(
            text = stringResource(R.string.skin_intro),
            color = SaltTheme.colors.subText,
            style = SaltTheme.textStyles.sub,
        )
        Spacer(modifier = Modifier.height(SectionGap))
        SectionTitle(text = stringResource(R.string.skin_mine))
        Spacer(modifier = Modifier.height(TitleGap))
        SkinTile()
        Spacer(modifier = Modifier.height(SectionGap))
        SectionTitle(text = stringResource(R.string.skin_redeem_title))
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.skin_redeem_hint),
            color = SaltTheme.colors.subText,
            style = SaltTheme.textStyles.sub,
        )
        Spacer(modifier = Modifier.height(TitleGap))
        RoundedColumn(
            modifier = Modifier.fillMaxWidth(),
            paddingValues = PaddingValues(0.dp),
        ) {
            ItemEdit(
                text = code,
                onChange = { code = it },
                hint = stringResource(R.string.skin_code_hint),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { ops.onRedeem(code) }
                ),
            )
        }
        Spacer(modifier = Modifier.height(TitleGap))
        Button(
            onClick = { ops.onRedeem(code) },
            text = stringResource(R.string.skin_redeem),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(PagePadding))
    }
}

/** 区块标题 */
@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = SaltTheme.colors.text,
        style = SaltTheme.textStyles.main,
        fontWeight = FontWeight.SemiBold,
    )
}

/** 皮肤卡片 使用中的那张带主色描边 */
@UnstableSaltUiApi
@Composable
private fun SkinTile() {
    val accents = dinnerPartyAccents()
    val shape = RoundedCornerShape(SkinCorner)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(SaltTheme.colors.subBackground)
            .border(2.dp, accents.primary, shape)
    ) {
        Column(modifier = Modifier.padding(SkinPadding)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(PreviewRatio)
                    .clip(RoundedCornerShape(PreviewCorner))
                    .background(SaltTheme.colors.popup),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Palette,
                    contentDescription = null,
                    modifier = Modifier.size(PreviewIcon),
                    tint = accents.secondary,
                )
            }
            Spacer(modifier = Modifier.height(TitleGap))
            Text(
                text = stringResource(R.string.skin_default_name),
                color = SaltTheme.colors.text,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.skin_default_desc),
                color = SaltTheme.colors.subText,
                fontSize = 12.sp,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.skin_in_use),
                color = accents.primary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

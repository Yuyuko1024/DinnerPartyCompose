package cc.dinnerparty.compose.ui.view.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cc.dinnerparty.compose.R
import cc.dinnerparty.compose.ui.theme.hexText
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import com.moriafly.salt.ui.Button
import com.moriafly.salt.ui.ButtonAppearance
import com.moriafly.salt.ui.ItemEdit
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.UnstableSaltUiApi
import com.moriafly.salt.ui.dialog.DialogTitle
import com.moriafly.salt.ui.outerPadding

/** 取色轮直径 */
private val WheelSize = 200.dp

/** 取色器色带高度 */
private val BandHeight = 28.dp

/** 取色器与色带的间距 */
private val BandGap = 12.dp

/** 颜色预览色块直径 */
private val PreviewSize = 20.dp

/** 主题色取色弹框 传空表示恢复默认 */
@OptIn(UnstableSaltUiApi::class)
@Composable
fun AccentDialog(
    title: String,
    accent: Color,
    onDismiss: () -> Unit,
    onPick: (Color?) -> Unit,
) {
    val controller = rememberColorPickerController()
    var picked by remember { mutableStateOf(accent) }
    var hex by remember { mutableStateOf(accent.hexText()) }

    GlassDialog(onDismissRequest = onDismiss) {
        DialogTitle(text = title)
        ItemEdit(
            text = hex,
            onChange = { text ->
                hex = text
                val color = text.parseHex()
                if (color != null) {
                    picked = color
                    controller.selectByColor(color, false)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            hint = stringResource(R.string.theme_color_hint),
            actionContent = {
                Box(
                    modifier = Modifier
                        .size(PreviewSize)
                        .clip(CircleShape)
                        .background(picked)
                )
            },
        )
        Column(
            modifier = Modifier.outerPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HsvColorPicker(
                modifier = Modifier.size(WheelSize),
                controller = controller,
                initialColor = accent,
                onColorChanged = { envelope ->
                    picked = envelope.color
                    if (envelope.fromUser) hex = envelope.color.hexText()
                },
            )
            Spacer(modifier = Modifier.height(BandGap))
            BrightnessSlider(
                controller = controller,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(BandHeight),
                initialColor = accent,
            )
        }
        Row(modifier = Modifier.outerPadding()) {
            Button(
                onClick = { onPick(null) },
                text = stringResource(R.string.theme_color_reset),
                modifier = Modifier.weight(1f),
                appearance = ButtonAppearance.Plain,
            )
        }
        Row(modifier = Modifier.outerPadding()) {
            Button(
                onClick = onDismiss,
                text = stringResource(R.string.action_cancel),
                modifier = Modifier.weight(1f),
                appearance = ButtonAppearance.Subtle,
            )
            Spacer(modifier = Modifier.width(SaltTheme.dimens.padding))
            Button(
                onClick = { onPick(picked) },
                text = stringResource(R.string.action_save),
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )
        }
    }
}

/** 解析 #RRGGBB 文本 非法返回空 */
private fun String.parseHex(): Color? {
    val body = trim().removePrefix("#")
    if (body.length != 6) return null
    val value = body.toLongOrNull(16) ?: return null
    return Color(0xFF000000L or value)
}

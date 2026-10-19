package cc.dinnerparty.compose.ui.view.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import cc.dinnerparty.compose.R
import com.moriafly.salt.ui.Button
import com.moriafly.salt.ui.ButtonType
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.UnstableSaltUiApi
import com.moriafly.salt.ui.dialog.BasicDialog
import com.moriafly.salt.ui.dialog.DialogTitle
import com.moriafly.salt.ui.outerPadding

/** 标题加自定义内容的双向确认弹框 */
@OptIn(UnstableSaltUiApi::class)
@Composable
fun YesNoContentDialog(
    title: String,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    enabled: Boolean = true,
    properties: DialogProperties = DialogProperties(),
    cancelText: String = stringResource(R.string.action_cancel),
    confirmText: String = stringResource(R.string.action_save),
    content: (@Composable () -> Unit)? = null,
) {
    BasicDialog(
        onDismissRequest = onDismissRequest,
        properties = properties,
    ) {
        DialogTitle(text = title)
        if (content != null) {
            Column(modifier = Modifier.outerPadding()) {
                content()
            }
        }
        Row(modifier = Modifier.outerPadding()) {
            Button(
                onClick = onDismissRequest,
                text = cancelText,
                modifier = Modifier.weight(1f),
                enabled = enabled,
                type = ButtonType.Sub,
            )
            Spacer(modifier = Modifier.width(SaltTheme.dimens.padding))
            Button(
                onClick = onConfirm,
                text = confirmText,
                modifier = Modifier.weight(1f),
                enabled = enabled,
                maxLines = 1,
            )
        }
    }
}

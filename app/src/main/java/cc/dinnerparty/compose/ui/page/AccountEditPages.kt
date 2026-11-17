package cc.dinnerparty.compose.ui.page

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cc.dinnerparty.compose.R
import cc.dinnerparty.compose.api.ApiException
import com.moriafly.salt.ui.Button
import com.moriafly.salt.ui.ButtonAppearance
import com.moriafly.salt.ui.ItemEdit
import com.moriafly.salt.ui.ItemEditPassword
import com.moriafly.salt.ui.RoundedColumn
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.Text
import com.moriafly.salt.ui.UnstableSaltUiApi
import kotlinx.coroutines.launch

/** 修改住民名页的数据 */
class UsernamePageUi(val currentName: String = "")

/** 修改住民名页的操作 */
class UsernamePageOps(
    val onBack: () -> Unit = {},
    val onMessage: (String) -> Unit = {},
    val onSave: suspend (String) -> Unit = {},
)

/** 修改通行密码页的操作 */
class PasswordPageOps(
    val onBack: () -> Unit = {},
    val onMessage: (String) -> Unit = {},
    val onSave: suspend (String, String) -> Unit = { _, _ -> },
)

/** 修改住民名全屏页 */
@UnstableSaltUiApi
@Composable
fun UsernamePage(
    ui: UsernamePageUi,
    ops: UsernamePageOps,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(ui.currentName) }
    var saving by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    PageScaffold(
        info = PageInfo(
            path = stringResource(R.string.username_page_path),
            backText = stringResource(R.string.back_to_settings),
        ),
        onBack = ops.onBack,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(bottom = PagePadding),
        ) {
            CardLabel(text = stringResource(R.string.account_edit_username_hint))
            FieldBox {
                ItemEdit(text = name, onChange = { name = it })
            }
            ErrorText(text = errorText)
            EditButtons(
                saving = saving,
                onSave = {
                    if (saving) return@EditButtons
                    val trimmed = name.trim()
                    if (trimmed.isEmpty()) {
                        errorText = context.getString(R.string.account_edit_username_empty)
                        return@EditButtons
                    }
                    saving = true
                    errorText = null
                    scope.launch {
                        runCatching { ops.onSave(trimmed) }
                            .onSuccess {
                                saving = false
                                ops.onMessage(
                                    context.getString(R.string.account_edit_username_updated)
                                )
                                ops.onBack()
                            }
                            .onFailure { error ->
                                saving = false
                                errorText = (error as? ApiException)?.serverMessage
                                    ?: context.getString(R.string.account_edit_failed)
                            }
                    }
                },
            )
        }
    }
}

/** 修改通行密码全屏页 */
@UnstableSaltUiApi
@Composable
fun PasswordPage(
    ops: PasswordPageOps,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var current by remember { mutableStateOf("") }
    var next by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    // 返回首个校验错误 全部通过时为空
    fun errorOf(): String? = when {
        current.isBlank() -> context.getString(R.string.account_edit_password_current_empty)
        next.length < PasswordMinLength ->
            context.getString(R.string.account_edit_password_too_short)

        next != confirm -> context.getString(R.string.account_edit_password_mismatch)
        else -> null
    }

    PageScaffold(
        info = PageInfo(
            path = stringResource(R.string.password_page_path),
            backText = stringResource(R.string.back_to_settings),
        ),
        onBack = ops.onBack,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(bottom = PagePadding)
        ) {
            PasswordField(
                label = stringResource(R.string.account_edit_password_current_hint),
                text = current,
                onChange = { current = it },
            )
            PasswordField(
                label = stringResource(R.string.account_edit_password_new_hint),
                text = next,
                onChange = { next = it },
            )
            PasswordField(
                label = stringResource(R.string.account_edit_password_confirm_hint),
                text = confirm,
                onChange = { confirm = it },
            )
            ErrorText(text = errorText)
            EditButtons(
                saving = saving,
                onSave = {
                    if (saving) return@EditButtons
                    val hint = errorOf()
                    errorText = hint
                    if (hint == null) {
                        saving = true
                        scope.launch {
                            runCatching { ops.onSave(current, next) }
                                .onSuccess {
                                    saving = false
                                    ops.onMessage(
                                        context.getString(R.string.account_edit_password_updated)
                                    )
                                    ops.onBack()
                                }
                                .onFailure { error ->
                                    saving = false
                                    errorText = (error as? ApiException)?.serverMessage
                                        ?: context.getString(R.string.account_edit_failed)
                                }
                        }
                    }
                },
            )
        }
    }
}

/** 输入项上方的标题 */
@UnstableSaltUiApi
@Composable
private fun CardLabel(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 6.dp),
        color = SaltTheme.colors.subText,
        style = SaltTheme.textStyles.sub,
    )
}

/** 底色与卡片区分的输入框 */
@UnstableSaltUiApi
@Composable
private fun FieldBox(content: @Composable ColumnScope.() -> Unit) {
    RoundedColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        paddingValues = PaddingValues(0.dp),
        color = SaltTheme.colors.background,
        content = content,
    )
}

/** 通行密码输入项 */
@UnstableSaltUiApi
@Composable
private fun PasswordField(
    label: String,
    text: String,
    onChange: (String) -> Unit,
) {
    CardLabel(text = label)
    FieldBox {
        ItemEditPassword(text = text, onChange = onChange)
    }
}

/** 校验失败提示 没有错误时不占位 */
@UnstableSaltUiApi
@Composable
private fun ErrorText(text: String?) {
    if (text == null) return
    Text(
        text = text,
        modifier = Modifier.padding(bottom = 8.dp),
        color = SaltTheme.colors.error,
        style = SaltTheme.textStyles.sub,
    )
}

/** 保存与取消 */
@UnstableSaltUiApi
@Composable
private fun EditButtons(
    saving: Boolean,
    onSave: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
    ) {
        Button(
            onClick = onSave,
            modifier = Modifier.weight(1f),
            enabled = !saving,
            text = stringResource(
                if (saving) R.string.account_saving else R.string.action_save
            ),
        )
    }
}

private const val PasswordMinLength = 6

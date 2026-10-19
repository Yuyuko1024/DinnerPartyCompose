package cc.dinnerparty.compose.ui.view.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.dinnerparty.compose.R
import cc.dinnerparty.compose.api.ApiException
import cc.dinnerparty.compose.ui.viewmodel.AuthViewModel
import com.moriafly.salt.ui.ItemEdit
import com.moriafly.salt.ui.ItemEditPassword
import com.moriafly.salt.ui.RoundedColumn
import com.moriafly.salt.ui.Text
import com.moriafly.salt.ui.UnstableSaltUiApi
import kotlinx.coroutines.launch

/** 修改住民名弹框 */
@OptIn(UnstableSaltUiApi::class)
@Composable
fun EditUsernameDialog(
    currentUsername: String,
    onDismiss: () -> Unit,
    authViewModel: AuthViewModel,
    onMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var username by remember { mutableStateOf(currentUsername) }
    var submitting by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    YesNoContentDialog(
        title = stringResource(R.string.account_edit_username_title),
        onDismissRequest = {
            if (!submitting) onDismiss()
        },
        onConfirm = {
            if (!submitting) {
                val name = username.trim()
                if (name.isEmpty()) {
                    errorText = context.getString(R.string.account_edit_username_empty)
                } else {
                    submitting = true
                    errorText = null
                    scope.launch {
                        runCatching { authViewModel.updateUsername(name) }
                            .onSuccess {
                                submitting = false
                                onMessage(context.getString(R.string.account_edit_username_updated))
                                onDismiss()
                            }
                            .onFailure { error ->
                                submitting = false
                                errorText = (error as? ApiException)?.serverMessage
                                    ?: context.getString(R.string.account_edit_failed)
                            }
                    }
                }
            }
        },
        enabled = !submitting,
        confirmText = stringResource(
            if (submitting) R.string.account_saving else R.string.action_save
        ),
        content = {
            DialogField(
                hint = stringResource(R.string.account_edit_username_hint),
                text = username,
                onChange = { username = it },
                enabled = !submitting,
            )
            errorText?.let { message ->
                Text(
                    text = message,
                    modifier = Modifier.padding(top = 8.dp),
                    fontSize = 13.sp,
                    color = ErrorColor,
                )
            }
        },
    )
}

/** 修改通行密码弹框 */
@OptIn(UnstableSaltUiApi::class)
@Composable
fun EditPasswordDialog(
    onDismiss: () -> Unit,
    authViewModel: AuthViewModel,
    onMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    YesNoContentDialog(
        title = stringResource(R.string.account_change_password),
        onDismissRequest = {
            if (!submitting) onDismiss()
        },
        onConfirm = {
            if (!submitting) {
                errorText = when {
                    currentPassword.isBlank() ->
                        context.getString(R.string.account_edit_password_current_empty)

                    newPassword.length < PasswordMinLength ->
                        context.getString(R.string.account_edit_password_too_short)

                    newPassword != confirmPassword ->
                        context.getString(R.string.account_edit_password_mismatch)

                    else -> null
                }
                if (errorText == null) {
                    submitting = true
                    scope.launch {
                        runCatching {
                            authViewModel.updatePassword(currentPassword, newPassword)
                        }
                            .onSuccess {
                                submitting = false
                                onMessage(context.getString(R.string.account_edit_password_updated))
                                onDismiss()
                            }
                            .onFailure { error ->
                                submitting = false
                                errorText = (error as? ApiException)?.serverMessage
                                    ?: context.getString(R.string.account_edit_failed)
                            }
                    }
                }
            }
        },
        enabled = !submitting,
        confirmText = stringResource(
            if (submitting) R.string.account_saving else R.string.action_save
        ),
        content = {
            DialogField(
                hint = stringResource(R.string.account_edit_password_current_hint),
                text = currentPassword,
                onChange = { currentPassword = it },
                enabled = !submitting,
                password = true,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            DialogField(
                hint = stringResource(R.string.account_edit_password_new_hint),
                text = newPassword,
                onChange = { newPassword = it },
                enabled = !submitting,
                password = true,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            DialogField(
                hint = stringResource(R.string.account_edit_password_confirm_hint),
                text = confirmPassword,
                onChange = { confirmPassword = it },
                enabled = !submitting,
                password = true,
            )
            errorText?.let { message ->
                Text(
                    text = message,
                    modifier = Modifier.padding(top = 8.dp),
                    fontSize = 13.sp,
                    color = ErrorColor,
                )
            }
        },
    )
}

/** 弹框内的单个输入项 */
@OptIn(UnstableSaltUiApi::class)
@Composable
private fun DialogField(
    hint: String,
    text: String,
    onChange: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    password: Boolean = false,
) {
    RoundedColumn(
        modifier = modifier.fillMaxWidth(),
        paddingValues = PaddingValues(0.dp),
    ) {
        if (password) {
            ItemEditPassword(
                text = text,
                onChange = onChange,
                hint = hint,
                readOnly = !enabled,
            )
        } else {
            ItemEdit(
                text = text,
                onChange = onChange,
                hint = hint,
                readOnly = !enabled,
            )
        }
    }
}

private val ErrorColor = Color(0xFFD9534F)

private const val PasswordMinLength = 6

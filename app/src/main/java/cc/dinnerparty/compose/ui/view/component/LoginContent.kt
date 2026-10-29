package cc.dinnerparty.compose.ui.view.component

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.dinnerparty.compose.R
import cc.dinnerparty.compose.ui.viewmodel.AuthUiState
import com.moriafly.salt.ui.Button
import com.moriafly.salt.ui.Icon
import com.moriafly.salt.ui.ItemEdit
import com.moriafly.salt.ui.ItemEditPassword
import com.moriafly.salt.ui.RoundedColumn
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.Text
import com.moriafly.salt.ui.UnstableSaltUiApi
import io.github.nadeemiqbal.segmentedcontrol.SegmentItem
import io.github.nadeemiqbal.segmentedcontrol.SegmentedControl
import io.github.nadeemiqbal.segmentedcontrol.SegmentedControlDefaults
import io.github.nadeemiqbal.segmentedcontrol.SegmentedControlStyle

enum class LoginType {
    LOGIN,
    REGISTER,
    REGISTER_FORM,
    FORGET_PASSWORD
}

@UnstableSaltUiApi
@Composable
fun LoginContent(
    modifier: Modifier = Modifier,
    state: AuthUiState = AuthUiState(),
    onClose: () -> Unit = {},
    onLogin: (String, String) -> Unit = { _, _ -> },
    onRegister: (String, String, String, String) -> Unit = { _, _, _, _ -> },
    onResetPassword: (String, String, String) -> Unit = { _, _, _ -> },
    onRequestRegisterCode: (String) -> Unit = {},
    onRequestResetCode: (String) -> Unit = {},
    onClearError: () -> Unit = {}
) {
    var selectedTabs by remember { mutableStateOf(LoginType.LOGIN) }

    // 切换页面时清掉上一个表单的错误
    val switchTo: (LoginType) -> Unit = {
        onClearError()
        selectedTabs = it
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(DialogPadding)
            .wrapContentHeight()
            .animateContentSize()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.login_title),
                fontSize = 20.sp,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .size(CloseButtonSize)
                    .clip(CircleShape)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.about_close),
                    modifier = Modifier.size(18.dp),
                    tint = SaltTheme.colors.subText
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        SegmentedControl(
            items = listOf(
                SegmentItem(stringResource(R.string.login_tab_resident)),
                SegmentItem(stringResource(R.string.login_tab_newcomer))
            ),
            selectedIndex = selectedTabs.segmentIndex,
            onSelectionChange = {
                switchTo(if (it == 0) LoginType.LOGIN else LoginType.REGISTER)
            },
            style = SegmentedControlStyle.Pill,
            colors = SegmentedControlDefaults.colors(SegmentedControlStyle.Pill).copy(
                selectedContentColor = Color.White,
                thumbColor = Color(0xFFE2789F)
            ),
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        )

        state.errorMessage?.let { message ->
            Text(
                text = message,
                color = ErrorColor,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        when (selectedTabs) {
            LoginType.LOGIN -> LoginView(
                state = state,
                onForgetPassword = { switchTo(LoginType.FORGET_PASSWORD) },
                onLogin = onLogin
            )
            LoginType.REGISTER -> RegisterView(
                onEnterRegisterForm = { switchTo(LoginType.REGISTER_FORM) }
            )
            LoginType.REGISTER_FORM -> RegisterFormView(
                state = state,
                onRegister = onRegister,
                onRequestCode = onRequestRegisterCode
            )
            LoginType.FORGET_PASSWORD -> ForgetPasswordView(
                state = state,
                onResetPassword = onResetPassword,
                onRequestCode = onRequestResetCode,
                onBackLogin = { switchTo(LoginType.LOGIN) }
            )
        }
    }

}

@UnstableSaltUiApi
@Composable
fun LoginView(
    state: AuthUiState = AuthUiState(),
    onLogin: (String, String) -> Unit = { _, _ -> },
    onForgetPassword: () -> Unit = {}
) {
    val uriHandler = LocalUriHandler.current
    var name by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .padding(0.dp, 16.dp)
            .fillMaxWidth()
    ) {
        RoundedColumn(
            modifier = Modifier
                .padding(0.dp, 0.dp, 0.dp, 8.dp)
                .fillMaxWidth(),
            paddingValues = PaddingValues(
                horizontal = 0.dp,
                vertical = 0.dp
            )
        ) {
            ItemEdit(
                hint = stringResource(R.string.login_hint_username),
                text = name,
                onChange = { name = it },
            )
        }
        RoundedColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(0.dp, 0.dp, 0.dp, 8.dp),
            paddingValues = PaddingValues(
                horizontal = 0.dp,
                vertical = 0.dp
            )
        ) {
            ItemEditPassword(
                hint = stringResource(R.string.login_hint_password),
                text = password,
                onChange = { password = it },
            )
        }
        Text(
            text = stringResource(R.string.login_forgot_password),
            modifier = Modifier
                .align(Alignment.End)
                .padding(0.dp, 0.dp, 0.dp, 8.dp)
                .clickable(onClick = onForgetPassword),
            color = Color(0xFFE2789F),
        )
        Button(
            modifier = Modifier.fillMaxWidth(),
            text = if (state.isBusy) stringResource(R.string.login_submitting) else stringResource(R.string.login_submit),
            enabled = !state.isBusy && name.isNotBlank() && password.isNotBlank(),
            onClick = { onLogin(name, password) }
        )
        Text(
            text = stringResource(R.string.login_privacy_policy),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(0.dp, 8.dp, 0.dp, 0.dp)
                .clickable {
                    uriHandler.openUriSafely(PRIVACY_POLICY_URL)
                },
            style = SaltTheme.textStyles.sub
        )
    }
}

@UnstableSaltUiApi
@Composable
fun RegisterView(
    onEnterRegisterForm: () -> Unit = {}
) {
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier.padding(0.dp, 16.dp)
            .fillMaxWidth()
    ) {
        Text(
            text = stringResource(R.string.login_register_join_hint),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally)
                .padding(0.dp, 8.dp)
        )
        Image(
            painter = painterResource(R.drawable.qrcode),
            contentDescription = null,
            modifier = Modifier.clip(SaltTheme.shapes.small)
                .clickable {
                    uriHandler.openUriSafely("https://qm.qq.com/q/DP6mpdX0oo")
                }
        )
        Text(
            text = stringResource(R.string.login_register_group),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally)
                .padding(0.dp, 8.dp)
        )
        Button(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.login_register_joined),
            onClick = onEnterRegisterForm
        )
        Text(
            text = stringResource(R.string.login_privacy_policy),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(0.dp, 8.dp, 0.dp, 0.dp)
                .clickable {
                    uriHandler.openUriSafely(PRIVACY_POLICY_URL)
                },
            style = SaltTheme.textStyles.sub
        )
    }
}

@UnstableSaltUiApi
@Composable
fun RegisterFormView(
    state: AuthUiState = AuthUiState(),
    onRegister: (String, String, String, String) -> Unit = { _, _, _, _ -> },
    onRequestCode: (String) -> Unit = {}
) {
    val uriHandler = LocalUriHandler.current
    var name by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var qq by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    val passwordsMatched = password == confirmPassword

    Column(
        modifier = Modifier.padding(0.dp, 16.dp)
            .fillMaxWidth()
    ) {
        OperationHint(
            operation = stringResource(R.string.login_register_operation),
            detail = stringResource(R.string.login_register_operation_detail)
        )
        FormField(
            label = stringResource(R.string.login_field_username),
            hint = stringResource(R.string.login_field_username_hint),
            text = name,
            onChange = { name = it }
        )
        FormField(
            label = stringResource(R.string.login_field_password),
            hint = stringResource(R.string.login_field_password_hint),
            text = password,
            onChange = { password = it },
            password = true
        )
        FormField(
            label = stringResource(R.string.login_field_password_confirm),
            hint = stringResource(R.string.login_field_password_confirm_hint),
            text = confirmPassword,
            onChange = { confirmPassword = it },
            password = true,
            errorText = if (passwordsMatched) null else stringResource(R.string.login_field_password_mismatch)
        )
        QqField(
            text = qq,
            onChange = { qq = it },
            onRequestCode = { onRequestCode(qq) },
            codeCooldown = state.codeCooldown,
            isBusy = state.isBusy
        )
        FormField(
            label = stringResource(R.string.login_field_code),
            hint = stringResource(R.string.login_field_code_hint),
            text = code,
            onChange = { code = it }
        )
        Button(
            modifier = Modifier.fillMaxWidth(),
            text = if (state.isBusy) stringResource(R.string.login_register_submitting) else stringResource(R.string.login_register_submit),
            enabled = !state.isBusy && name.isNotBlank() && password.isNotBlank() &&
                passwordsMatched && qq.isNotBlank() && code.isNotBlank(),
            onClick = { onRegister(name, password, qq, code) }
        )
        Text(
            text = stringResource(R.string.login_privacy_policy),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(0.dp, 8.dp, 0.dp, 0.dp)
                .clickable {
                    uriHandler.openUriSafely(PRIVACY_POLICY_URL)
                },
            style = SaltTheme.textStyles.sub
        )
    }
}

@UnstableSaltUiApi
@Composable
fun ForgetPasswordView(
    state: AuthUiState = AuthUiState(),
    onResetPassword: (String, String, String) -> Unit = { _, _, _ -> },
    onRequestCode: (String) -> Unit = {},
    onBackLogin: () -> Unit = {}
) {
    val uriHandler = LocalUriHandler.current
    var qq by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    val passwordsMatched = password == confirmPassword

    Column(
        modifier = Modifier.padding(0.dp, 16.dp)
            .fillMaxWidth()
    ) {
        OperationHint(
            operation = stringResource(R.string.login_reset_operation),
            detail = stringResource(R.string.login_reset_operation_detail)
        )
        QqField(
            text = qq,
            onChange = { qq = it },
            onRequestCode = { onRequestCode(qq) },
            codeCooldown = state.codeCooldown,
            isBusy = state.isBusy
        )
        FormField(
            label = stringResource(R.string.login_field_code),
            hint = stringResource(R.string.login_field_code_hint),
            text = code,
            onChange = { code = it }
        )
        FormField(
            label = stringResource(R.string.login_field_password_new),
            hint = stringResource(R.string.login_field_password_hint),
            text = password,
            onChange = { password = it },
            password = true
        )
        FormField(
            label = stringResource(R.string.login_field_password_confirm),
            hint = stringResource(R.string.login_field_password_confirm_hint),
            text = confirmPassword,
            onChange = { confirmPassword = it },
            password = true,
            errorText = if (passwordsMatched) null else stringResource(R.string.login_field_password_mismatch)
        )
        Button(
            modifier = Modifier.fillMaxWidth(),
            text = if (state.isBusy) stringResource(R.string.login_reset_submitting) else stringResource(R.string.login_reset_submit),
            enabled = !state.isBusy && qq.isNotBlank() && code.isNotBlank() &&
                password.isNotBlank() && passwordsMatched,
            onClick = { onResetPassword(qq, code, password) }
        )
        Text(
            text = stringResource(R.string.login_back),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 8.dp)
                .clickable(onClick = onBackLogin),
            color = SaltTheme.colors.highlight
        )
        Text(
            text = stringResource(R.string.login_privacy_policy),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(0.dp, 8.dp, 0.dp, 0.dp)
                .clickable {
                    uriHandler.openUriSafely(PRIVACY_POLICY_URL)
                },
            style = SaltTheme.textStyles.sub
        )
    }
}

@Composable
private fun OperationHint(
    operation: String,
    detail: String
) {
    val prefix = stringResource(R.string.login_operation_prefix)
    val suffix = stringResource(R.string.login_operation_suffix)

    Text(
        text = buildAnnotatedString {
            append(prefix)
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append(operation)
            }
            append(suffix)
            append(detail)
        },
        fontSize = 14.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    )
}

@UnstableSaltUiApi
@Composable
private fun FormField(
    label: String,
    hint: String,
    text: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    password: Boolean = false,
    errorText: String? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = FieldSpacing)
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        RoundedColumn(
            modifier = Modifier.fillMaxWidth(),
            paddingValues = FieldContentPadding
        ) {
            if (password) {
                ItemEditPassword(
                    text = text,
                    onChange = onChange,
                    hint = hint
                )
            } else {
                ItemEdit(
                    text = text,
                    onChange = onChange,
                    hint = hint
                )
            }
        }
        if (errorText != null) {
            Text(
                text = errorText,
                fontSize = 13.sp,
                color = ErrorColor,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

@UnstableSaltUiApi
@Composable
private fun QqField(
    text: String,
    onChange: (String) -> Unit,
    onRequestCode: () -> Unit,
    modifier: Modifier = Modifier,
    codeCooldown: Int = 0,
    isBusy: Boolean = false
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = FieldSpacing)
    ) {
        Text(
            text = stringResource(R.string.login_field_qq),
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            RoundedColumn(
                modifier = Modifier.weight(1f),
                paddingValues = FieldContentPadding
            ) {
                ItemEdit(
                    text = text,
                    onChange = onChange,
                    hint = stringResource(R.string.login_field_qq_hint)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                text = if (codeCooldown > 0) stringResource(R.string.login_cooldown, codeCooldown) else stringResource(R.string.login_request_code),
                enabled = !isBusy && codeCooldown == 0 && text.isNotBlank(),
                onClick = onRequestCode
            )
        }
    }
}

private val LoginType.segmentIndex: Int
    get() = when (this) {
        LoginType.LOGIN, LoginType.FORGET_PASSWORD -> 0
        LoginType.REGISTER, LoginType.REGISTER_FORM -> 1
    }

private val FieldContentPadding = PaddingValues(0.dp)
private val FieldSpacing = 8.dp
private val ErrorColor = Color(0xFFD9534F)

private val DialogPadding = 16.dp

private val CloseButtonSize = 24.dp
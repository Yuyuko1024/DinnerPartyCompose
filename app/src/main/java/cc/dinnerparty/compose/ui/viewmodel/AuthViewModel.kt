package cc.dinnerparty.compose.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cc.dinnerparty.compose.api.model.VerificationCodeResponse
import cc.dinnerparty.compose.data.AuthSession
import cc.dinnerparty.compose.data.repo.AuthRepo
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 登录页状态。 */
data class AuthUiState(
    /** 当前登录态，未登录为 null。 */
    val session: AuthSession? = null,
    val isBusy: Boolean = false,
    /** 验证码重发倒计时，单位为秒。 */
    val codeCooldown: Int = 0,
    /** 错误文案，含后端给的中文提示。 */
    val errorMessage: String? = null,
)

/** 维护登录态并处理登录、注册与密码重设。 */
class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = AuthRepo(application)

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repo.session.collect { session ->
                _uiState.update { it.copy(session = session) }
            }
        }
        // 冷启动时拉取一次最新住民资料
        viewModelScope.launch {
            runCatching { repo.refreshUser() }
        }
    }

    /** 住民名与通行密码登录。 */
    fun login(username: String, password: String) {
        submit { repo.login(username.trim(), password) }
    }

    /** 凭 QQ 验证码登记住民。 */
    fun register(username: String, password: String, qq: String, code: String) {
        submit { repo.register(username.trim(), password, qq.trim(), code.trim()) }
    }

    /** 凭 QQ 验证码重设通行密码。 */
    fun resetPassword(qq: String, code: String, newPassword: String) {
        submit { repo.resetPassword(qq.trim(), code.trim(), newPassword) }
    }

    /** 请求注册验证码。 */
    fun requestRegisterCode(qq: String) {
        requestCode { repo.requestRegisterCode(qq.trim()) }
    }

    /** 请求重设密码验证码。 */
    fun requestResetCode(qq: String) {
        requestCode { repo.requestResetCode(qq.trim()) }
    }

    /** 上传裁剪后的头像 成功后登录态自动刷新 */
    suspend fun uploadAvatar(bytes: ByteArray) {
        repo.uploadAvatar(bytes)
    }

    /** 修改住民名 成功后登录态自动刷新 */
    suspend fun updateUsername(username: String) {
        repo.updateUsername(username)
    }

    /** 修改通行密码 成功后登录态自动刷新 */
    suspend fun updatePassword(currentPassword: String, newPassword: String) {
        repo.updatePassword(currentPassword, newPassword)
    }

    /** 退出全部设备。 */
    fun logoutAll() {
        submit { repo.logoutAll() }
    }

    /** 删除当前账户。 */
    fun deleteAccount() {
        submit { repo.deleteAccount() }
    }

    /** 清空错误文案。 */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /** 执行登录类请求，成功后登录态由数据层推送。 */
    private fun submit(action: suspend () -> Unit) {
        if (_uiState.value.isBusy) return
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, errorMessage = null) }
            runCatching { action() }
                .onFailure { error ->
                    _uiState.update { it.copy(errorMessage = error.message) }
                }
            _uiState.update { it.copy(isBusy = false) }
        }
    }

    /** 请求验证码，成功后开始倒计时。 */
    private fun requestCode(action: suspend () -> VerificationCodeResponse) {
        val state = _uiState.value
        if (state.isBusy || state.codeCooldown > 0) return
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, errorMessage = null) }
            runCatching { action() }
                .onSuccess { response ->
                    startCooldown(response.resendAfter ?: DEFAULT_RESEND_SECONDS)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(errorMessage = error.message) }
                }
            _uiState.update { it.copy(isBusy = false) }
        }
    }

    /** 每秒递减一次重发倒计时。 */
    private fun startCooldown(seconds: Int) {
        viewModelScope.launch {
            for (remaining in seconds downTo 1) {
                _uiState.update { it.copy(codeCooldown = remaining) }
                delay(1000)
            }
            _uiState.update { it.copy(codeCooldown = 0) }
        }
    }
}

private const val DEFAULT_RESEND_SECONDS = 60

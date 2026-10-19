package cc.dinnerparty.compose.data.repo

import android.content.Context
import cc.dinnerparty.compose.BuildConfig
import cc.dinnerparty.compose.api.ApiClient
import cc.dinnerparty.compose.api.ApiException
import cc.dinnerparty.compose.api.AuthTokenHolder
import cc.dinnerparty.compose.api.apiCall
import cc.dinnerparty.compose.api.apis.UserApi
import cc.dinnerparty.compose.api.model.AuthResponse
import cc.dinnerparty.compose.api.model.UpdatePasswordRequest
import cc.dinnerparty.compose.api.model.UpdateUsernameRequest
import cc.dinnerparty.compose.api.model.User
import cc.dinnerparty.compose.api.model.VerificationCodeResponse
import cc.dinnerparty.compose.data.AuthDataStore
import cc.dinnerparty.compose.data.AuthSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

/** 认证与住民数据入口，登录态写入本地。 */
class AuthRepo(context: Context) {

    private val userApi: UserApi by lazy { ApiClient.createService(UserApi::class.java) }
    private val authDataStore = AuthDataStore(context)

    /** 本地保存的登录态，未登录为 null。 */
    val session: Flow<AuthSession?> = authDataStore.session

    /** 住民名与通行密码登录。 */
    suspend fun login(username: String, password: String): AuthResponse = apiCall {
        userApi.login(
            username = username,
            password = password,
            platform = PLATFORM,
            appVersion = BuildConfig.VERSION_NAME,
        )
    }.also { save(it) }

    /** 凭 QQ 私聊验证码登记住民。 */
    suspend fun register(
        username: String,
        password: String,
        qq: String,
        verificationCode: String,
    ): AuthResponse = apiCall {
        userApi.register(
            username = username,
            password = password,
            qq = qq,
            verificationCode = verificationCode,
            platform = PLATFORM,
            appVersion = BuildConfig.VERSION_NAME,
        )
    }.also { save(it) }

    /** 凭 QQ 私聊验证码重设通行密码。 */
    suspend fun resetPassword(
        qq: String,
        verificationCode: String,
        newPassword: String,
    ): AuthResponse = apiCall {
        userApi.resetPassword(
            qq = qq,
            verificationCode = verificationCode,
            newPassword = newPassword,
            platform = PLATFORM,
            appVersion = BuildConfig.VERSION_NAME,
        )
    }.also { save(it) }

    /** 请求注册验证码。 */
    suspend fun requestRegisterCode(qq: String): VerificationCodeResponse = apiCall {
        userApi.requestVerificationCode(qq = qq, purpose = PURPOSE_REGISTER)
    }

    /** 请求重设密码验证码。 */
    suspend fun requestResetCode(qq: String): VerificationCodeResponse = apiCall {
        userApi.requestVerificationCode(qq = qq, purpose = PURPOSE_RESET)
    }

    /** 拉取一次当前住民资料并回写本地。 */
    suspend fun refreshUser() {
        val current = authDataStore.session.first() ?: return
        AuthTokenHolder.token = current.token
        val response = try {
            apiCall { userApi.me() }
        } catch (error: ApiException) {
            // 凭证失效时退回未登录
            if (error.httpCode == 401) clearSession()
            throw error
        }
        response.user?.let { authDataStore.saveUser(it) }
    }

    /** 上传裁剪后的头像 成功后回写本地资料 */
    suspend fun uploadAvatar(bytes: ByteArray): User {
        val body = bytes.toRequestBody("image/jpeg".toMediaType())
        val part = MultipartBody.Part.createFormData("avatar", "avatar.jpg", body)
        val user = apiCall { userApi.uploadAvatar(part) }.user
            ?: error("avatar response missing user")
        authDataStore.saveAvatar(user)
        return user
    }

    /** 修改住民名 成功后保存新凭证 */
    suspend fun updateUsername(username: String): User {
        val auth = apiCall { userApi.updateUsername(UpdateUsernameRequest(username)) }
        save(auth)
        return auth.user ?: error("username response missing user")
    }

    /** 修改通行密码 成功后保存新凭证 */
    suspend fun updatePassword(currentPassword: String, newPassword: String): User {
        val auth = apiCall {
            userApi.updatePassword(UpdatePasswordRequest(currentPassword, newPassword))
        }
        save(auth)
        return auth.user ?: error("password response missing user")
    }

    /** 退出全部设备，成功后清空本地登录态。 */
    suspend fun logoutAll() {
        apiCall { userApi.logoutAll() }
        clearSession()
    }

    /** 删除账户，成功后清空本地登录态。 */
    suspend fun deleteAccount() {
        apiCall { userApi.deleteAccount() }
        clearSession()
    }

    /** 保存接口返回的凭证与住民资料。 */
    private suspend fun save(auth: AuthResponse) {
        val token = auth.token ?: return
        val user = auth.user ?: return
        authDataStore.save(token, user)
        AuthTokenHolder.token = token
    }

    /** 清空内存与本地登录态。 */
    private suspend fun clearSession() {
        AuthTokenHolder.token = null
        authDataStore.clear()
    }
}

private const val PLATFORM = "android"
private const val PURPOSE_REGISTER = "register"
private const val PURPOSE_RESET = "reset_password"

package cc.dinnerparty.compose.api.model

import com.google.gson.annotations.SerializedName

/** 住民资料。 */
data class User(
    val id: String? = null,
    val username: String? = null,
    @SerializedName("admin_level") val adminLevel: String? = null,
    @SerializedName("has_avatar") val hasAvatar: Boolean = false,
)

/** 登录、注册成功后的住民资料与凭证。 */
data class AuthResponse(
    val user: User? = null,
    val token: String? = null,
)

/** 当前住民资料响应。 */
data class UserResponse(
    val user: User? = null,
)

/** 头像上传结果 */
data class AvatarResponse(
    val success: Boolean = false,
    val user: User? = null,
)

/** 修改住民名请求 */
data class UpdateUsernameRequest(
    val username: String,
)

/** 修改通行密码请求 */
data class UpdatePasswordRequest(
    val currentPassword: String,
    val newPassword: String,
)

/** 只表示成功与否的响应。 */
data class SimpleResponse(
    val success: Boolean = false,
)

/** 验证码发送结果，[resendAfter] 为秒。 */
data class VerificationCodeResponse(
    val success: Boolean = false,
    @SerializedName("expires_in") val expiresIn: Int? = null,
    @SerializedName("resend_after") val resendAfter: Int? = null,
)

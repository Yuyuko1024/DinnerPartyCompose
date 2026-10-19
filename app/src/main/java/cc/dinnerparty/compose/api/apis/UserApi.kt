package cc.dinnerparty.compose.api.apis

import cc.dinnerparty.compose.api.model.AuthResponse
import cc.dinnerparty.compose.api.model.AvatarResponse
import cc.dinnerparty.compose.api.model.SimpleResponse
import cc.dinnerparty.compose.api.model.UpdatePasswordRequest
import cc.dinnerparty.compose.api.model.UpdateUsernameRequest
import cc.dinnerparty.compose.api.model.UserResponse
import cc.dinnerparty.compose.api.model.VerificationCodeResponse
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part

/** 住民认证接口。 */
interface UserApi {

    /** 获取当前登录住民的最新资料。 */
    @GET("users/me")
    suspend fun me(): UserResponse

    /** 退出所有设备上的登录。 */
    @POST("users/logout-all")
    suspend fun logoutAll(): SimpleResponse

    /** 删除当前住民的账户。 */
    @DELETE("users/me")
    suspend fun deleteAccount(): SimpleResponse

    /** 上传裁剪后的头像 */
    @Multipart
    @PUT("users/me/avatar")
    suspend fun uploadAvatar(
        @Part avatar: MultipartBody.Part,
    ): AvatarResponse

    /** 修改住民名 */
    @PATCH("users/me")
    suspend fun updateUsername(
        @Body body: UpdateUsernameRequest,
    ): AuthResponse

    /** 修改通行密码 */
    @PATCH("users/me/password")
    suspend fun updatePassword(
        @Body body: UpdatePasswordRequest,
    ): AuthResponse

    /** 住民名与通行密码登录。 */
    @FormUrlEncoded
    @POST("users/login")
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String,
        @Field("client[platform]") platform: String,
        @Field("client[app_version]") appVersion: String,
    ): AuthResponse

    /** 凭 QQ 私聊验证码登记住民。 */
    @FormUrlEncoded
    @POST("users/register")
    suspend fun register(
        @Field("username") username: String,
        @Field("password") password: String,
        @Field("qq") qq: String,
        @Field("verificationCode") verificationCode: String,
        @Field("client[platform]") platform: String,
        @Field("client[app_version]") appVersion: String,
    ): AuthResponse

    /** 凭 QQ 私聊验证码重设通行密码，成功后即为登录态。 */
    @FormUrlEncoded
    @POST("users/password-reset")
    suspend fun resetPassword(
        @Field("qq") qq: String,
        @Field("verificationCode") verificationCode: String,
        @Field("newPassword") newPassword: String,
        @Field("client[platform]") platform: String,
        @Field("client[app_version]") appVersion: String,
    ): AuthResponse

    /** 让机器人向指定 QQ 私聊发送验证码。 */
    @FormUrlEncoded
    @POST("users/verification-code")
    suspend fun requestVerificationCode(
        @Field("qq") qq: String,
        @Field("purpose") purpose: String,
    ): VerificationCodeResponse
}

package cc.dinnerparty.compose.api.apis

import cc.dinnerparty.compose.api.model.TasteResponse
import cc.dinnerparty.compose.api.model.UserProfile
import cc.dinnerparty.compose.api.model.VisibilityRequest
import cc.dinnerparty.compose.api.model.VisibilityResponse
import cc.dinnerparty.compose.api.model.WidgetsRequest
import cc.dinnerparty.compose.api.model.WidgetsResponse
import com.google.gson.JsonObject
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

/** 住民主页接口 */
interface ProfileApi {

    /** 住民主页快照 */
    @GET("users/{id}/profile")
    suspend fun getProfile(@Path("id") id: String): UserProfile

    /** 自己填某一类餐饮的喜好 */
    @PUT("users/me/taste")
    suspend fun saveTaste(@Body body: JsonObject): TasteResponse

    /** 保存小组件设置 */
    @PUT("users/me/profile")
    suspend fun saveWidgets(@Body body: WidgetsRequest): WidgetsResponse

    /** 保存收藏公开设置 */
    @PUT("api/favorites/visibility")
    suspend fun saveVisibility(@Body body: VisibilityRequest): VisibilityResponse
}

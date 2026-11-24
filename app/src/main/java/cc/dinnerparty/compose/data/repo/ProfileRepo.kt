package cc.dinnerparty.compose.data.repo

import cc.dinnerparty.compose.api.ApiClient
import cc.dinnerparty.compose.api.apiCall
import cc.dinnerparty.compose.api.apis.ProfileApi
import cc.dinnerparty.compose.api.model.TasteAxis
import cc.dinnerparty.compose.api.model.UserProfile
import cc.dinnerparty.compose.api.model.VisibilityRequest
import cc.dinnerparty.compose.api.model.VisibilityResponse
import cc.dinnerparty.compose.api.model.WidgetsRequest
import cc.dinnerparty.compose.api.model.WidgetsResponse
import com.google.gson.JsonNull
import com.google.gson.JsonObject

/** 住民主页数据入口 直接返回接口 DTO */
class ProfileRepo {

    private val profileApi: ProfileApi by lazy { ApiClient.createService(ProfileApi::class.java) }

    /** 住民主页快照 */
    suspend fun getProfile(id: String): UserProfile = apiCall { profileApi.getProfile(id) }

    /** 保存某一轴的喜好 传 null 回到自动推断 */
    suspend fun saveTaste(axis: String, level: Int?): List<TasteAxis> = apiCall {
        // 后端靠字段存在区分自动与缺省，null 必须显式发出
        val body = JsonObject().apply {
            addProperty("axis", axis)
            if (level == null) add("level", JsonNull.INSTANCE) else addProperty("level", level)
        }
        profileApi.saveTaste(body).taste.orEmpty()
    }

    /** 保存小组件设置 */
    suspend fun saveWidgets(widgets: List<String>, hidden: List<String>): WidgetsResponse =
        apiCall { profileApi.saveWidgets(WidgetsRequest(widgets, hidden)) }

    /** 保存收藏公开设置 */
    suspend fun saveVisibility(isPublic: Boolean): VisibilityResponse =
        apiCall { profileApi.saveVisibility(VisibilityRequest(isPublic)) }
}

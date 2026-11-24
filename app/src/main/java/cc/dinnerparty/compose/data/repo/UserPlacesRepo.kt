package cc.dinnerparty.compose.data.repo

import cc.dinnerparty.compose.api.ApiClient
import cc.dinnerparty.compose.api.apiCall
import cc.dinnerparty.compose.api.apis.UserPlacesApi
import cc.dinnerparty.compose.api.model.UserPlacesResponse

/** 住民创建的地点数据入口 */
class UserPlacesRepo {

    private val api: UserPlacesApi by lazy {
        ApiClient.createService(UserPlacesApi::class.java)
    }

    /** 某位住民创建的地点 最近创建的在前 */
    suspend fun getUserPlaces(id: String): UserPlacesResponse =
        apiCall { api.getUserPlaces(id) }
}

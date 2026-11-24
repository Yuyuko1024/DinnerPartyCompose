package cc.dinnerparty.compose.data.repo

import cc.dinnerparty.compose.api.ApiClient
import cc.dinnerparty.compose.api.apiCall
import cc.dinnerparty.compose.api.apis.UserFavoritesApi
import cc.dinnerparty.compose.api.model.UserFavoritesResponse

/** 住民收藏夹数据入口 */
class UserFavoritesRepo {

    private val api: UserFavoritesApi by lazy {
        ApiClient.createService(UserFavoritesApi::class.java)
    }

    /** 某位住民的收藏夹 最近收藏的在前 */
    suspend fun getUserFavorites(id: String): UserFavoritesResponse =
        apiCall { api.getUserFavorites(id) }
}

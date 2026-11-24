package cc.dinnerparty.compose.api.apis

import cc.dinnerparty.compose.api.model.UserFavoritesResponse
import retrofit2.http.GET
import retrofit2.http.Path

/** 住民收藏夹接口 */
interface UserFavoritesApi {

    /** 某位住民的收藏夹 只有本人或公开收藏看得到 */
    @GET("users/{id}/favorites")
    suspend fun getUserFavorites(@Path("id") id: String): UserFavoritesResponse
}

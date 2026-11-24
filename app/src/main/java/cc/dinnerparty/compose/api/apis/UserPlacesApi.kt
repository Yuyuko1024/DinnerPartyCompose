package cc.dinnerparty.compose.api.apis

import cc.dinnerparty.compose.api.model.UserPlacesResponse
import retrofit2.http.GET
import retrofit2.http.Path

/** 住民创建的地点接口 */
interface UserPlacesApi {

    /** 某位住民创建的地点 游客只看得到公开的 */
    @GET("users/{id}/places")
    suspend fun getUserPlaces(@Path("id") id: String): UserPlacesResponse
}

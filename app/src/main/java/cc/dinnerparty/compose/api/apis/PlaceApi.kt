package cc.dinnerparty.compose.api.apis

import cc.dinnerparty.compose.api.model.Place
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** 地点接口。 */
interface PlaceApi {

    /** 视野范围内的地点，附带用户名、评价人数与加权平均分。 */
    @GET("places/nearby")
    suspend fun getNearbyPlaces(
        @Query("minLng") minLng: Double,
        @Query("minLat") minLat: Double,
        @Query("maxLng") maxLng: Double,
        @Query("maxLat") maxLat: Double,
    ): List<Place>

    /** 单个地点。 */
    @GET("places/{id}")
    suspend fun getPlace(@Path("id") id: Long): Place

    /** 按关键词搜索站内地点。 */
    @GET("api/places/search/fast")
    suspend fun searchPlaces(
        @Query("q") query: String,
        @Query("limit") limit: Int,
        @Query("lat") lat: Double?,
        @Query("lng") lng: Double?,
    ): List<Place>
}

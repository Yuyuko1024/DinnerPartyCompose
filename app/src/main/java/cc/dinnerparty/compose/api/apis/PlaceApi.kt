package cc.dinnerparty.compose.api.apis

import cc.dinnerparty.compose.api.model.Place
import cc.dinnerparty.compose.api.model.PlaceDraft
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
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

    /** 新建地点。 */
    @POST("places")
    suspend fun createPlace(@Body body: PlaceDraft): Place

    /** 修改地点，服务端不接受 address 与 rating。 */
    @PUT("places/{id}")
    suspend fun updatePlace(@Path("id") id: Long, @Body body: PlaceDraft): Place
}

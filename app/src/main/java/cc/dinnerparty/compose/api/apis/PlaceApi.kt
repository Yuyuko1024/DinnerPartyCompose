package cc.dinnerparty.compose.api.apis

import cc.dinnerparty.compose.api.model.Place
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 地点接口。
 *
 * 路径一律不带前导斜杠，原因见 [cc.dinnerparty.compose.api.ApiClient]。
 * 注意后端前缀不统一：地点是 `/places`，而评价是 `/api/ratings`，只能逐条照抄。
 */
interface PlaceApi {

    /**
     * 视野范围内的地点，附带创建者 / 最后修改者用户名、评价人数与加权平均分。
     *
     * 四个角必须齐全，少一个后端就 400 `{"error":"缺少范围参数"}`。
     * 返回的是裸数组、没有分页，所以只适合按视野查，别拿它拉全表。
     */
    @GET("places/nearby")
    suspend fun getNearbyPlaces(
        @Query("minLng") minLng: Double,
        @Query("minLat") minLat: Double,
        @Query("maxLng") maxLng: Double,
        @Query("maxLat") maxLat: Double,
    ): List<Place>

    /** 单个地点，字段与列表项完全一致。不存在时后端返回 404。 */
    @GET("places/{id}")
    suspend fun getPlace(@Path("id") id: Long): Place
}

package cc.dinnerparty.compose.data.repo

import cc.dinnerparty.compose.api.ApiClient
import cc.dinnerparty.compose.api.apiCall
import cc.dinnerparty.compose.api.apis.CategoryApi
import cc.dinnerparty.compose.api.apis.PlaceApi
import cc.dinnerparty.compose.api.model.Category
import cc.dinnerparty.compose.api.model.Place
import cc.dinnerparty.compose.data.model.MapBounds

/**
 * 地点数据入口。
 *
 * 直接对外返回接口 DTO：目前接口字段就是界面要用的字段，
 * 中间再套一层领域模型只是白拷一遍，等真出现裁剪 / 聚合需求时再引。
 *
 * 不需要 `withContext(Dispatchers.IO)`——Retrofit 的 `suspend` 方法自己在 IO 线程上执行。
 */
class PlaceRepo {

    private val placeApi: PlaceApi by lazy { ApiClient.createService(PlaceApi::class.java) }
    private val categoryApi: CategoryApi by lazy { ApiClient.createService(CategoryApi::class.java) }

    /** 视野范围内的地点。 */
    suspend fun getNearbyPlaces(bounds: MapBounds): List<Place> = apiCall {
        placeApi.getNearbyPlaces(
            minLng = bounds.minLng,
            minLat = bounds.minLat,
            maxLng = bounds.maxLng,
            maxLat = bounds.maxLat,
        )
    }

    /** 单个地点详情。 */
    suspend fun getPlace(id: Long): Place = apiCall { placeApi.getPlace(id) }

    /** 全部分类，用于筛选。 */
    suspend fun getCategories(): List<Category> = apiCall { categoryApi.getCategories() }
}

package cc.dinnerparty.compose.data.repo

import cc.dinnerparty.compose.api.ApiClient
import cc.dinnerparty.compose.api.apiCall
import cc.dinnerparty.compose.api.apis.CategoryApi
import cc.dinnerparty.compose.api.apis.PlaceApi
import cc.dinnerparty.compose.api.model.Category
import cc.dinnerparty.compose.api.model.Place
import cc.dinnerparty.compose.api.model.PlaceDraft
import cc.dinnerparty.compose.data.model.MapBounds

/** 地点数据入口，直接返回接口 DTO。 */
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

    /** 全部分类。 */
    suspend fun getCategories(): List<Category> = apiCall { categoryApi.getCategories() }

    /** 新建地点。 */
    suspend fun createPlace(draft: PlaceDraft): Place = apiCall { placeApi.createPlace(draft) }

    /** 修改地点。 */
    suspend fun updatePlace(id: Long, draft: PlaceDraft): Place =
        apiCall { placeApi.updatePlace(id, draft) }
}

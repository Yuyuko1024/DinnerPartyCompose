package cc.dinnerparty.compose.api.apis

import cc.dinnerparty.compose.api.model.Category
import retrofit2.http.GET

/** 地点分类。 */
interface CategoryApi {

    /** 全部分类，裸数组。后端已经排好序。 */
    @GET("categories")
    suspend fun getCategories(): List<Category>
}

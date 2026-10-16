package cc.dinnerparty.compose.api.apis

import cc.dinnerparty.compose.api.model.Category
import retrofit2.http.GET

/** 地点分类。 */
interface CategoryApi {

    /** 全部分类 */
    @GET("categories")
    suspend fun getCategories(): List<Category>
}

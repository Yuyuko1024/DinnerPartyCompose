package cc.dinnerparty.compose.api.model

import com.google.gson.annotations.SerializedName

/**
 * 地点分类。后端已按「常用优先 → sort_order → 名称」排好序，客户端不要再排一遍。
 */
data class Category(
    val id: Long? = null,
    val name: String? = null,
    /** 后端在 SQL 里存 0/1，返回前会强制转成布尔。 */
    @SerializedName("is_common") val isCommon: Boolean = false,
    @SerializedName("sort_order") val sortOrder: Int? = null,
    @SerializedName("created_by") val createdBy: String? = null,
    @SerializedName("created_time") val createdTime: String? = null,
)

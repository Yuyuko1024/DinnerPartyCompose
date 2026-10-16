package cc.dinnerparty.compose.api.model

import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName

/**
 * 地点，与后端 `Place` 表一一对应，字段全部可空。
 *
 * @param category 分类名。
 * @param perPersonCost 人均，单位元。
 * @param ratingCount / ratingAverage 评价人数与加权平均分（1~4 分制）。
 */
data class Place(
    val id: Long? = null,
    val name: String? = null,
    val description: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val category: String? = null,
    @SerializedName("per_person_cost") val perPersonCost: Int? = null,
    @SerializedName("creator_id") val creatorId: String? = null,
    @SerializedName("created_time") val createdTime: String? = null,
    @SerializedName("updated_time") val updatedTime: String? = null,
    @SerializedName("updated_by") val updatedBy: String? = null,
    // 后端在 TEXT 列里存 JSON 数组字符串，用 TypeAdapter 解析
    @JsonAdapter(StringListTypeAdapter::class)
    @SerializedName("exterior_images") val exteriorImages: List<String>? = null,
    @JsonAdapter(StringListTypeAdapter::class)
    @SerializedName("menu_images") val menuImages: List<String>? = null,
    @SerializedName("has_vector") val hasVector: Int? = null,
    @SerializedName("vector_updated_at") val vectorUpdatedAt: String? = null,
    val address: String? = null,
    // 以下为联表带出来的展示字段
    @SerializedName("creator_name") val creatorName: String? = null,
    @SerializedName("updated_by_name") val updatedByName: String? = null,
    @SerializedName("rating_count") val ratingCount: Int? = null,
    @SerializedName("rating_average") val ratingAverage: Double? = null,
)

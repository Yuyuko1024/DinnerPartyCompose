package cc.dinnerparty.compose.api.model

import com.google.gson.annotations.SerializedName

/**
 * 地点分类。
 */
data class Category(
    val id: Long? = null,
    val name: String? = null,
    @SerializedName("is_common") val isCommon: Boolean = false,
    @SerializedName("sort_order") val sortOrder: Int? = null,
    @SerializedName("created_by") val createdBy: String? = null,
    @SerializedName("created_time") val createdTime: String? = null,
)

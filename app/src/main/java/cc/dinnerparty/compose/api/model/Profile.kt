package cc.dinnerparty.compose.api.model

import com.google.gson.annotations.SerializedName

/** 住民主页快照 */
data class UserProfile(
    val user: User? = null,
    val relation: ProfileRelation? = null,
    val city: String? = null,
    val taste: List<TasteAxis>? = null,
    val widgets: List<String>? = null,
    @SerializedName("hidden_widgets") val hiddenWidgets: List<String>? = null,
    val content: ProfileContent? = null,
    @SerializedName("collection_visibility") val collectionVisibility: Boolean? = null,
)

/** 观看者与这位住民的关系 */
data class ProfileRelation(
    val self: Boolean = false,
    @SerializedName("can_block") val canBlock: Boolean = false,
    val blocked: Boolean = false,
)

/** 六类餐饮偏好中的一类 */
data class TasteAxis(
    val key: String? = null,
    val value: Double? = null,
    val basis: String? = null,
    @SerializedName("self_level") val selfLevel: Int? = null,
    @SerializedName("unavailable_reason") val unavailableReason: String? = null,
)

/** 住民主页的三个小组件 */
data class ProfileContent(
    val collection: List<ProfileCollection>? = null,
    val recent: List<ProfilePlace>? = null,
    val favorite: List<ProfilePlace>? = null,
)

/** 收藏夹卡片 */
data class ProfileCollection(
    val id: String? = null,
    val title: String? = null,
    val total: Int? = null,
    val places: List<ProfilePlace>? = null,
)

/** 小组件里的地点预览 */
data class ProfilePlace(
    val id: Long? = null,
    val name: String? = null,
    val category: String? = null,
    @SerializedName("per_person_cost") val perPersonCost: Int? = null,
    val image: String? = null,
    @SerializedName("rating_count") val ratingCount: Int? = null,
    @SerializedName("rating_average") val ratingAverage: Double? = null,
    @SerializedName("own_score") val ownScore: Double? = null,
)

/** 保存喜好后的六轴 */
data class TasteResponse(
    val taste: List<TasteAxis>? = null,
)

/** 保存小组件设置 */
data class WidgetsRequest(
    val widgets: List<String>,
    @SerializedName("hidden_widgets") val hiddenWidgets: List<String>,
)

/** 小组件设置结果 */
data class WidgetsResponse(
    val widgets: List<String>? = null,
    @SerializedName("hidden_widgets") val hiddenWidgets: List<String>? = null,
)

/** 收藏公开设置 */
data class VisibilityRequest(
    @SerializedName("is_public") val isPublic: Boolean,
)

/** 收藏公开设置结果 */
data class VisibilityResponse(
    @SerializedName("is_public") val isPublic: Boolean = false,
)

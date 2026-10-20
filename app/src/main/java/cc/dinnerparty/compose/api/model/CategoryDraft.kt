package cc.dinnerparty.compose.api.model

import com.google.gson.annotations.SerializedName

/** 新建分类的请求体。 */
data class CategoryDraft(
    val name: String,
    @SerializedName("allow_similar")
    val allowSimilar: Boolean = false,
)

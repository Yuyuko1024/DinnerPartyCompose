package cc.dinnerparty.compose.api.model

import com.google.gson.annotations.SerializedName

/** 站点公告 */
data class SiteNotice(
    val id: Long? = null,
    val title: String? = null,
    val content: String? = null,
    @SerializedName("color_key") val colorKey: String? = null,
    @SerializedName("created_by") val createdBy: String? = null,
    @SerializedName("created_time") val createdTime: String? = null,
)

data class NoticeResponse(
    val notice: SiteNotice? = null,
)

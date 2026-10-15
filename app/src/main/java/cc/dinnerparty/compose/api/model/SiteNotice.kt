package cc.dinnerparty.compose.api.model

import com.google.gson.annotations.SerializedName

/** 站点公告，后端只会返回当前生效的那一条。 */
data class SiteNotice(
    val id: Long? = null,
    val title: String? = null,
    val content: String? = null,
    /** 前端用来挑配色的键，取值由后端决定，客户端不要硬编码配色表。 */
    @SerializedName("color_key") val colorKey: String? = null,
    @SerializedName("created_by") val createdBy: String? = null,
    @SerializedName("created_time") val createdTime: String? = null,
)

/** `GET /notices/current` 的响应外壳：当前没有生效公告时 [notice] 为 null。 */
data class NoticeResponse(
    val notice: SiteNotice? = null,
)

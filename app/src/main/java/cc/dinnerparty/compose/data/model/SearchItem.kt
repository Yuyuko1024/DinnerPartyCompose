package cc.dinnerparty.compose.data.model

/** 一条搜索结果 站内地点与高德地点共用 */
data class SearchItem(
    val id: String,
    /** 站内地点主键 高德结果为空 */
    val placeId: Long?,
    val name: String,
    val address: String?,
    val latitude: Double,
    val longitude: Double,
    val distanceMeters: Double?,
    val fromAmap: Boolean,
)

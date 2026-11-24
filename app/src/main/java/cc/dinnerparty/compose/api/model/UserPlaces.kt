package cc.dinnerparty.compose.api.model

/** 某位住民创建的地点 最近创建的在前 */
data class UserPlacesResponse(
    val user: User? = null,
    val places: List<Place>? = null,
    /** 这位住民一共创建了多少个可见地点 */
    val total: Int? = null,
    /** 单次最多返回多少个 */
    val limit: Int? = null,
)

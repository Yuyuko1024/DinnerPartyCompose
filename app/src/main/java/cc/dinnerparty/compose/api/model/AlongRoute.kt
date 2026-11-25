package cc.dinnerparty.compose.api.model

import com.google.gson.annotations.SerializedName

/**
 * 顺路吃的检索请求
 *
 * @param paths 每条路线抽样后的坐标串 内层依次是经度与纬度
 */
data class AlongRouteRequest(
    val paths: List<List<List<Double>>>,
    val query: String = "",
    val categories: List<String> = emptyList(),
    @SerializedName("corridor_meters") val corridorMeters: Int,
    val limit: Int = 60,
)

/** 地点与某条路线的对应关系 */
data class AlongRouteMatch(
    @SerializedName("route_index") val routeIndex: Int? = null,
    @SerializedName("distance_to_route") val distanceToRoute: Double? = null,
    @SerializedName("route_distance_from_start") val routeDistanceFromStart: Double? = null,
    @SerializedName("route_progress") val routeProgress: Double? = null,
)

/** 顺路吃的检索结果 */
data class AlongRouteResult(
    val places: List<Place>? = null,
    val meta: AlongRouteMeta? = null,
)

/** 检索结果的统计信息 */
data class AlongRouteMeta(
    val count: Int? = null,
    @SerializedName("route_count") val routeCount: Int? = null,
    @SerializedName("corridor_meters") val corridorMeters: Int? = null,
    val truncated: Boolean? = null,
)

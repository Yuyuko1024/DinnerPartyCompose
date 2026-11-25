package cc.dinnerparty.compose.data.model

/** 顺路吃的出行方式 */
enum class AlongRouteMode {
    Driving,
    Transit,
    Walking,
    Riding,
}

/** 一个经纬度点 */
data class GeoPoint(val latitude: Double, val longitude: Double)

/** 路线的起点或终点 */
data class RouteEndpoint(val point: GeoPoint, val name: String)

/** 高德给出的一条路线方案 */
data class AlongRoutePlan(
    val index: Int,
    val points: List<GeoPoint>,
    val distanceMeters: Float,
    val durationSeconds: Long,
)

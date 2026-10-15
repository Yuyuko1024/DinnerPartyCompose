package cc.dinnerparty.compose.data.model

/**
 * 地图视野的经纬度范围，对应 `/places/nearby` 的四个查询参数。
 *
 * 单独抽一个类型出来，是为了别让 ViewModel 依赖高德的 `LatLngBounds`——
 * 换成别的定位 / 地图方案时这层不用动。
 */
data class MapBounds(
    val minLng: Double,
    val minLat: Double,
    val maxLng: Double,
    val maxLat: Double,
)

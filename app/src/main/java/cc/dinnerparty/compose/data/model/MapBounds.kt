package cc.dinnerparty.compose.data.model

/** 地图视野的经纬度范围。 */
data class MapBounds(
    val minLng: Double,
    val minLat: Double,
    val maxLng: Double,
    val maxLat: Double,
)

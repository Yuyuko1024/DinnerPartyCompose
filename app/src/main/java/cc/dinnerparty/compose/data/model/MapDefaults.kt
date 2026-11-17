package cc.dinnerparty.compose.data.model

/** 地图默认视角与缩放范围 */
object MapDefaults {
    /** 默认中心纬度 广州 */
    const val Latitude = 23.016485

    /** 默认中心经度 */
    const val Longitude = 113.394405

    /** 默认缩放等级 */
    const val Zoom = 15f

    /** 高德支持的最小缩放等级 */
    const val MinZoom = 3f

    /** 高德支持的最大缩放等级 */
    const val MaxZoom = 20f
}

/** 地图视角 */
data class MapCamera(
    val latitude: Double,
    val longitude: Double,
    val zoom: Float,
)

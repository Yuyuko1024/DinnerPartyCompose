package cc.dinnerparty.compose.data.cluster

import cc.dinnerparty.compose.data.model.MapBounds
import kotlin.math.abs
import kotlin.math.roundToInt

data class ClusterViewport(
    val bounds: MapBounds,
    val zoom: Float,
    val mapMaxZoom: Int,
    val radiusPx: Double,
)

internal fun logicalZoom(zoom: Float, previous: Int?, maxZoom: Int): Int {
    if (!zoom.isFinite()) return previous?.coerceIn(0, maxZoom) ?: 0
    val nearest = zoom.roundToInt().coerceIn(0, maxZoom)
    if (previous == null || abs(zoom - previous) > 0.65f) return nearest
    return previous.coerceIn(0, maxZoom)
}

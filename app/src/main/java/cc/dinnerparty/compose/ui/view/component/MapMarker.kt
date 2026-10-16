package cc.dinnerparty.compose.ui.view.component

import cc.dinnerparty.compose.data.cluster.MapClusterNode
import cc.dinnerparty.compose.data.model.isAvoided
import com.amap.api.maps.model.LatLng

/** 地图上的一个标记，可能是一个地点或一个聚合气泡。 */
data class MapMarker(
    /** 地点 id 或聚合 id，可直接当键用。 */
    val id: Long,
    val position: LatLng,
    /** 名称胶囊的内容，空字符串表示不画胶囊。聚合气泡恒为空。 */
    val name: String,
    /** 避雷地点，图形与名称胶囊都换成危险色。聚合气泡恒为 false。 */
    val isAvoided: Boolean,
    /** 这个气泡里聚合了多少个地点；null 表示这就是一个地点。 */
    val clusterCount: Int? = null,
)

/** 聚合节点转标记，单点没有地点 id 时返回 null。 */
fun MapClusterNode.toMapMarker(): MapMarker? = when (this) {
    is MapClusterNode.Single -> {
        val placeId = place.id ?: return null
        MapMarker(
            id = placeId,
            position = LatLng(latitude, longitude),
            name = place.name.orEmpty(),
            isAvoided = place.isAvoided(),
        )
    }

    is MapClusterNode.Group -> MapMarker(
        id = id,
        position = LatLng(latitude, longitude),
        // 聚合气泡不带名称
        name = "",
        isAvoided = false,
        clusterCount = count,
    )
}

package cc.dinnerparty.compose.ui.view.component

import cc.dinnerparty.compose.data.cluster.MapClusterNode
import cc.dinnerparty.compose.data.model.isAvoided
import com.amap.api.maps.model.LatLng

/**
 * 地图上的一个标记：要么是一个地点，要么是一个聚合气泡。
 *
 * 只带画点必需的信息；点击之后要展示什么、进哪个页面由上层决定，
 * 所以这里不塞整个 `Place`。
 */
data class MapMarker(
    /**
     * 单个地点时是地点 id，聚合气泡时是聚合 id。
     *
     * 两套 id 各自从不相交的区间取值（见索引里的 `CLUSTER_ID_BASE`），所以可以直接当键用。
     * 靠 [clusterCount] 区分这是哪种。
     */
    val id: Long,
    val position: LatLng,
    /** 名称胶囊的内容，空字符串表示不画胶囊。聚合气泡恒为空。 */
    val name: String,
    /** 避雷地点，图形与名称胶囊都换成危险色。聚合气泡恒为 false。 */
    val isAvoided: Boolean,
    /** 这个气泡里聚合了多少个地点；null 表示这就是一个地点。 */
    val clusterCount: Int? = null,
)

/**
 * 聚合节点 → 标记。
 *
 * 单点连地点 id 都没有的话既点不开、也当不了标记的键，返回 null 交给调用方跳过。
 */
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
        // 聚合气泡上只有数字，没有名称
        name = "",
        isAvoided = false,
        clusterCount = count,
    )
}

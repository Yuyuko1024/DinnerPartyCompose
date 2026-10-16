package cc.dinnerparty.compose.data.cluster

import cc.dinnerparty.compose.api.model.Place

/** 聚合后落在地图上的一个节点，可能是一个地点或一组地点聚成的气泡。 */
sealed interface MapClusterNode {

    /** 节点的地理坐标。 */
    val longitude: Double
    val latitude: Double

    /** 单个地点，独立显示。 */
    data class Single(
        val place: Place,
        override val longitude: Double,
        override val latitude: Double,
    ) : MapClusterNode

    /** 一组地点聚合成的一个气泡。 */
    data class Group(
        /** 聚合 id，只在本次索引内有效。 */
        val id: Long,
        override val longitude: Double,
        override val latitude: Double,
        /** 这个气泡里包含多少个地点。 */
        val count: Int,
    ) : MapClusterNode
}

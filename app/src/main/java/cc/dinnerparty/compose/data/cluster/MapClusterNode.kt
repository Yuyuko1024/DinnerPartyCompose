package cc.dinnerparty.compose.data.cluster

import cc.dinnerparty.compose.api.model.Place

/**
 * 聚合后落在地图上的一个节点。
 *
 * 地图上的标记不总是「一个地点」：缩放级别小的时候，挨得近的一批地点会被合并成一个气泡，
 * 气泡里写着这堆地点有几个。卡片、导航、收藏这些只对单个地点有意义，所以两种形态分开表达，
 * 让调用方在编译期就必须想清楚自己面对的是哪一种。
 *
 * 怎么聚合见 [PlaceClusterIndex]。
 */
sealed interface MapClusterNode {

    /** 节点的地理坐标。聚合点取的是成员按地点数加权的重心。 */
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
        /** 聚合 id，用来问「放大到几级才散开」和取成员列表。只在本次索引内有效。 */
        val id: Long,
        override val longitude: Double,
        override val latitude: Double,
        /** 这个气泡里包含多少个地点。 */
        val count: Int,
    ) : MapClusterNode
}

package cc.dinnerparty.compose.data.cluster

class ClusterSnapshot internal constructor(
    internal val index: PlaceClusterIndex?,
    val nodes: List<MapClusterNode>,
    val zoom: Int,
) {
    internal val byId = nodes.associateBy { it.nodeId }

    internal fun memberIds(node: MapClusterNode): List<Long> = when (node) {
        is MapClusterNode.Single -> listOf(node.place.id!!)
        is MapClusterNode.Group -> index?.members(node.id).orEmpty().map { it.id!! }
    }
}

internal val MapClusterNode.nodeId: Long
    get() = when (this) {
        is MapClusterNode.Single -> place.id!!
        is MapClusterNode.Group -> id
    }

internal val MapClusterNode.pointCount: Int
    get() = when (this) {
        is MapClusterNode.Single -> 1
        is MapClusterNode.Group -> count
    }

internal class NodeBudget(var remaining: Int)

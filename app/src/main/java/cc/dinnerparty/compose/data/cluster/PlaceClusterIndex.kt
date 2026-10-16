package cc.dinnerparty.compose.data.cluster

import cc.dinnerparty.compose.api.model.Place
import cc.dinnerparty.compose.data.model.MapBounds
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.sinh
import kotlin.math.tan

/**
 * 地图点聚合索引。
 *
 * 把地点投到 Web 墨卡托平面，从最细的一级往上逐级按网格合并，得到一棵层级树。
 * 只索引有 id 又有经纬度的地点。
 */
class PlaceClusterIndex(
    places: List<Place>,
    /** 地图能达到的最大缩放级别，聚合只做到它的上一级。 */
    private val mapMaxZoom: Int,
    /** 聚合半径，单位屏幕像素，同时是格子边长。 */
    radiusPx: Double,
    private val minZoom: Int = DEFAULT_MIN_ZOOM,
) {

    /** 参与索引的地点。 */
    private val source: List<Place>

    /** 单点，也就是 [exitZoom] 及以上的节点。 */
    private val leaves: List<Node>

    /** `levels[zoom]` 是这一级下应该显示的全部节点，只有 `minZoom..exitZoom - 1` 有值。 */
    private val levels: Array<List<Node>>

    /** 从这一级起不再聚合，直接出单点。 */
    private val exitZoom = mapMaxZoom - 1

    /** 按聚合 id 反查节点。 */
    private val clustersById: Map<Long, Node>

    /** 聚合半径换算到世界坐标下的长度。 */
    private val worldRadius = radiusPx / TILE_SIZE

    init {
        source = places
        val projected = ArrayList<Node>(places.size)
        places.forEachIndexed { index, place ->
            val id = place.id
            val longitude = place.longitude
            val latitude = place.latitude
            if (id == null || longitude == null || latitude == null) return@forEachIndexed
            projected += Node(worldX(longitude), worldY(latitude), 1, index)
        }
        leaves = projected

        val built = Array(mapMaxZoom + 1) { emptyList<Node>() }
        val byId = HashMap<Long, Node>()
        var nextId = CLUSTER_ID_BASE

        // 退出聚合的那一级和它以上都是单点
        built[exitZoom] = projected
        built[mapMaxZoom] = projected

        // 从退出聚合的下一级开始往粗合并
        var current: List<Node> = projected
        for (zoom in (exitZoom - 1) downTo minZoom) {
            val cell = cellSizeAt(zoom)
            val buckets = LinkedHashMap<Long, MutableList<Node>>()
            for (node in current) {
                buckets.getOrPut(cellKey(node, cell)) { ArrayList(1) } += node
            }

            val merged = ArrayList<Node>(buckets.size)
            for (bucket in buckets.values) {
                if (bucket.size == 1) {
                    // 这一格里只有自己，直接升到粗一级
                    merged += bucket[0]
                    continue
                }
                var sumX = 0.0
                var sumY = 0.0
                var count = 0
                for (node in bucket) {
                    // 按地点数加权求中心
                    sumX += node.x * node.count
                    sumY += node.y * node.count
                    count += node.count
                }
                val cluster = Node(
                    x = sumX / count,
                    y = sumY / count,
                    count = count,
                    placeIndex = -1,
                    id = nextId++,
                    zoom = zoom,
                )
                byId[cluster.id] = cluster
                merged += cluster
            }

            built[zoom] = merged
            current = merged
        }

        levels = built
        clustersById = byId
    }

    /**
     * 当前视野与缩放级别下应该显示哪些节点。
     *
     * @param zoom 高德的缩放级别，小数直接四舍五入。超出索引范围时按边界那一级处理。
     */
    fun query(bounds: MapBounds, zoom: Float): List<MapClusterNode> {
        if (leaves.isEmpty()) return emptyList()

        val level = zoom.roundToInt().let { z ->
            // 到了退出聚合的级别就直接展开成单点
            if (z >= exitZoom) leaves else levels[z.coerceAtLeast(minZoom)]
        }

        val minX = worldX(bounds.minLng)
        val maxX = worldX(bounds.maxLng)
        // 墨卡托 y 轴朝下，北边对应 y 的下界
        val minY = worldY(bounds.maxLat)
        val maxY = worldY(bounds.minLat)

        return level.asSequence()
            .filter { it.x in minX..maxX && it.y in minY..maxY }
            .map { it.toClusterNode() }
            .toList()
    }

    /** 聚合散开所需的缩放级别，即它被合并出来时的级别加一。 */
    fun expansionZoom(clusterId: Long): Int =
        clustersById[clusterId]?.let { it.zoom + 1 } ?: minZoom

    private fun Node.toClusterNode(): MapClusterNode {
        val longitude = toLongitude(x)
        val latitude = toLatitude(y)
        return if (placeIndex >= 0) {
            MapClusterNode.Single(source[placeIndex], longitude, latitude)
        } else {
            MapClusterNode.Group(id, longitude, latitude, count)
        }
    }

    /** 网格边长（世界坐标单位）。 */
    private fun cellSizeAt(zoom: Int): Double = worldRadius / (1 shl zoom)

    private fun cellKey(node: Node, cell: Double): Long {
        val column = floor(node.x / cell).toLong()
        val row = floor(node.y / cell).toLong()
        return (column shl 32) or (row and 0xffffffffL)
    }

    /** 层级树上的一个节点，坐标为世界坐标（0~1 的墨卡托平面）。 */
    private class Node(
        val x: Double,
        val y: Double,
        /** 这个节点代表多少个地点；叶子恒为 1。 */
        val count: Int,
        /** 叶子在 [source] 里的下标；聚合点为 -1。 */
        val placeIndex: Int,
        /** 聚合 id；叶子为 -1。 */
        val id: Long = -1L,
        /** 这个聚合被合并出来时的级别。 */
        val zoom: Int = 0,
    )
}

/** 世界墨卡托的瓦片边长，和高德一致。 */
private const val TILE_SIZE = 256.0

/** 墨卡托能表示的最大纬度。 */
private const val MAX_MERCATOR_LAT = 85.05112877

/** 索引的起点级别。 */
private const val DEFAULT_MIN_ZOOM = 3

/** 聚合 id 的起点，大于所有地点 id。 */
private const val CLUSTER_ID_BASE = 1L shl 40

/** 经度 → 世界坐标 x，范围 0~1。 */
private fun worldX(longitude: Double): Double = (longitude + 180.0) / 360.0

/** 纬度 → 世界坐标 y，范围 0~1（北为 0，南为 1）。 */
private fun worldY(latitude: Double): Double {
    val radians = latitude.coerceIn(-MAX_MERCATOR_LAT, MAX_MERCATOR_LAT) * PI / 180.0
    return (1.0 - ln(tan(radians) + 1.0 / cos(radians)) / PI) / 2.0
}

private fun toLongitude(x: Double): Double = x * 360.0 - 180.0

private fun toLatitude(y: Double): Double = atan(sinh(PI * (1.0 - 2.0 * y))) * 180.0 / PI

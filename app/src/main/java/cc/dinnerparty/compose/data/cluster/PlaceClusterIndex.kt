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
 * 缩小时地图上几百个标记会糊成一片，谁也点不着。做法和 Web 端一致
 * （`map/cluster/engine.js`，那边用的是 supercluster）：把地点投到 Web 墨卡托平面，
 * 然后**从最细的一级往上**逐级按网格合并——每一级的格子边长是下一级的两倍，
 * 于是粗一级的气泡必定完整包含细一级的若干气泡，形成一棵层级树。
 *
 * 这棵层级树是「点一下该放到哪一级」的依据：聚合气泡记得自己是在哪一级被合并出来的，
 * 再细一级的网格里它就散了（见 [expansionZoom]）。只按当前屏幕做单层聚合给不出这个信息。
 *
 * 用网格而不是两两比距离：网格是 O(n)、边界稳定；两两比是 O(n²)，几百个点就开始吃力。
 * 代价是正好落在相邻格子里的两个点不会合并——Web 端同样如此，观感上没人分得出来。
 *
 * 几点约定：
 * - **坐标是 GCJ-02**（高德给的、后端存的都是），全程不做坐标系转换，投影只为了算格子。
 * - **只索引有 id 又有经纬度的地点**。没有 id 的点点击了也打不开详情，没有坐标的点根本画不出来。
 * - 本类只做纯计算，不碰任何高德对象，所以和在什么地图上画没有关系。
 */
class PlaceClusterIndex(
    places: List<Place>,
    radiusPx: Double = DEFAULT_RADIUS_PX,
    private val minZoom: Int = DEFAULT_MIN_ZOOM,
    private val maxZoom: Int = DEFAULT_MAX_ZOOM,
) {

    /** 参与索引的地点。节点的 [Node.placeIndex] 指向这个列表。 */
    private val source: List<Place>

    /** 最细一级的节点。超过 [maxZoom] 时直接用它。 */
    private val leaves: List<Node>

    /** `levels[zoom]` 是这一级下地图上应该显示的全部节点。 */
    private val levels: Array<List<Node>>

    /** 按聚合 id 反查节点，用来回答 [expansionZoom] 和 [members]。 */
    private val clustersById: Map<Long, Node>

    /** 世界坐标单位下的聚合半径，见 [Node] 与 [cellSizeAt] 的说明。 */
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

        val built = Array(maxZoom + 1) { emptyList<Node>() }
        val byId = HashMap<Long, Node>()
        var current: List<Node> = projected
        var nextId = CLUSTER_ID_BASE

        // 从最细的一级往粗合并：格子边长每级翻倍，某个气泡是在哪一级被合并出来的就记在
        // Node.zoom 上，那就是「点开它该放到哪一级」的依据。
        for (zoom in maxZoom downTo minZoom) {
            val cell = cellSizeAt(zoom)
            val buckets = LinkedHashMap<Long, MutableList<Node>>()
            for (node in current) {
                buckets.getOrPut(cellKey(node, cell)) { ArrayList(1) } += node
            }

            val merged = ArrayList<Node>(buckets.size)
            for (bucket in buckets.values) {
                if (bucket.size == 1) {
                    // 这一格里只有自己，位置不动、直接升到粗一级，仍然代表原来那些地点
                    merged += bucket[0]
                    continue
                }
                var sumX = 0.0
                var sumY = 0.0
                var count = 0
                for (node in bucket) {
                    // 按地点数加权，让气泡落在「人多」的那一侧，而不是被一个孤立点拽偏
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
            // 放到最细一级之上就不该再有聚合了，直接给叶子
            if (z > maxZoom) leaves else levels[z.coerceIn(minZoom, maxZoom)]
        }

        val minX = worldX(bounds.minLng)
        val maxX = worldX(bounds.maxLng)
        // 墨卡托的 y 轴朝下：纬度越高 y 越小，所以视野北边（maxLat）给的是下界
        val minY = worldY(bounds.maxLat)
        val maxY = worldY(bounds.minLat)

        return level.asSequence()
            .filter { it.x in minX..maxX && it.y in minY..maxY }
            .map { it.toClusterNode() }
            .toList()
    }

    /**
     * 点开这个聚合之后，相机应该放到哪一级。
     *
     * 就是它**被合并出来时的级别 + 1**：再细一级的网格里这批地点不再同格，气泡会被它的
     * 下一层取代。不要理解成「一路拆到单点」——那样几十个地点会一次点击直接冲到最大级别
     * （米级），而 Web 端是一次点击展开下一层，再点再展开。
     *
     * 返回值可能大于 [maxZoom]，表示放到最细一级也拆不开（几个地点坐标重合或贴得太近），
     * 调用方自己决定怎么办。
     */
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

    /** 网格边长（世界坐标单位）。世界宽度是 1，所以这里也等于「占整个世界宽度的几分之几」。 */
    private fun cellSizeAt(zoom: Int): Double = worldRadius / (1 shl zoom)

    private fun cellKey(node: Node, cell: Double): Long {
        val column = floor(node.x / cell).toLong()
        val row = floor(node.y / cell).toLong()
        return (column shl 32) or (row and 0xffffffffL)
    }

    /**
     * 层级树上的一个节点：要么是一个地点，要么是一组地点。
     *
     * 坐标是**世界坐标**（0~1 的墨卡托平面），不是经纬度——聚类只在平面距离上有意义。
     */
    private class Node(
        val x: Double,
        val y: Double,
        /** 这个节点代表多少个地点。叶子恒为 1。 */
        val count: Int,
        /** 叶子在 [source] 里的下标；聚合点为 -1。 */
        val placeIndex: Int,
        /** 聚合 id；叶子为 -1。 */
        val id: Long = -1L,
        /**
         * 这个聚合是在哪一级被合并出来的。它决定 [expansionZoom]：
         * 再细一级它就散了，所以点开时相机应该放到这一级 + 1。
         */
        val zoom: Int = 0,
    )
}

/** 世界墨卡托的瓦片边长，和高德一致。 */
private const val TILE_SIZE = 256.0

/** 墨卡托能表示的最大纬度，再往极地方向 y 会发散。 */
private const val MAX_MERCATOR_LAT = 85.05112877

/**
 * 聚合半径，单位是屏幕像素（而不是 `dp`）。
 *
 * 与 Web `map/cluster/engine.js` 的 `radiusPx = 68` 保持一致：那边已经按这个值调过疏密，
 * 改成别的值手机上看起来会不一样。
 */
private const val DEFAULT_RADIUS_PX = 68.0

/** 高德地图的缩放范围就是 3~20，索引跟着它建，多建了也没机会用到。 */
private const val DEFAULT_MIN_ZOOM = 3
private const val DEFAULT_MAX_ZOOM = 20

/**
 * 聚合 id 的起点。
 *
 * 地点 id 是数据库自增主键，很小；聚合 id 只是本地序号。两者各自会被当作标记的键放进同一张表，
 * 所以聚合 id 从一个不可能与地点 id 撞车的大数开始。
 */
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

package cc.dinnerparty.compose.data.cluster

import cc.dinnerparty.compose.api.model.Place
import cc.dinnerparty.compose.data.model.MapBounds
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sinh

class PlaceClusterIndex(
    places: List<Place>,
    mapMaxZoom: Int,
    radiusPx: Double,
    private val minZoom: Int = 0,
) {
    private val maxZoom = mapMaxZoom
    private var source = normalize(places)
    private val clusters = HashMap<Long, Node>()
    private val levels: Array<List<Node>>

    init {
        require(mapMaxZoom in 1..30)
        require(minZoom in 0..maxZoom)
        require(radiusPx.isFinite() && radiusPx > 0)
        val leaves = source.mapIndexed { index, place ->
            Node(worldX(place.longitude!!), worldY(place.latitude!!), 1, index)
        }
        levels = Array(maxZoom + 1) { leaves }
        var current = leaves
        val placeIds = source.mapTo(HashSet()) { it.id!! }
        for (zoom in maxZoom downTo minZoom) {
            val radius = radiusPx / TILE_SIZE / 2.0.pow(zoom)
            val grid = RadiusGrid(current, radius)
            val visited = BooleanArray(current.size)
            val merged = ArrayList<Node>(current.size)
            for (index in current.indices) {
                if (visited[index]) continue
                visited[index] = true
                val seed = current[index]
                val nearby = grid.nearby(seed).filter { !visited[it] }
                if (nearby.isEmpty()) {
                    merged += seed
                    continue
                }
                var sumLng = seed.worldLng * seed.count
                var sumLat = seed.worldLat * seed.count
                var count = seed.count
                val children = ArrayList<Node>(nearby.size + 1)
                children += seed
                for (neighbor in nearby) {
                    visited[neighbor] = true
                    val node = current[neighbor]
                    sumLng += node.worldLng * node.count
                    sumLat += node.worldLat * node.count
                    count += node.count
                    children += node
                }
                var clusterId = clusterIds.getAndIncrement()
                while (clusterId in placeIds) clusterId = clusterIds.getAndIncrement()
                val group = Node(sumLng / count, sumLat / count, count, -1,
                    clusterId, zoom, children)
                clusters[group.id] = group
                merged += group
            }
            levels[zoom] = merged
            current = merged
        }
    }

    fun query(bounds: MapBounds, zoom: Float): List<MapClusterNode> {
        val level = levels[zoom.roundToInt().coerceIn(minZoom, maxZoom)]
        val minLng = worldX(bounds.minLng)
        val maxLng = worldX(bounds.maxLng)
        val minLat = worldY(bounds.maxLat)
        val maxLat = worldY(bounds.minLat)
        return level.asSequence().filter { node ->
            val inLng = if (bounds.minLng > bounds.maxLng) {
                node.worldLng >= minLng || node.worldLng <= maxLng
            } else {
                node.worldLng in minLng..maxLng
            }
            inLng && node.worldLat in minLat..maxLat
        }.map { it.toMapNode() }.toList()
    }

    internal fun updatePlaces(places: List<Place>) {
        source = normalize(places)
    }

    fun expansionZoom(clusterId: Long): Int? = clusters[clusterId]?.let { it.zoom + 1 }

    fun children(clusterId: Long): List<MapClusterNode> =
        clusters[clusterId]?.children.orEmpty().map { it.toMapNode() }

    internal fun descendantsAt(
        clusterId: Long,
        targetZoom: Int,
        budget: NodeBudget,
    ): List<MapClusterNode>? {
        val root = clusters[clusterId] ?: return emptyList()
        val pending = ArrayDeque<Node>()
        val result = ArrayList<MapClusterNode>()
        val level = targetZoom.coerceIn(minZoom, maxZoom)
        pending.add(root)
        while (pending.isNotEmpty()) {
            if (--budget.remaining < 0) return null
            val node = pending.removeLast()
            if (node.placeIndex >= 0 || level <= node.zoom) result += node.toMapNode()
            else pending.addAll(node.children.asReversed())
        }
        return result
    }

    fun members(clusterId: Long): List<Place> {
        val root = clusters[clusterId] ?: return emptyList()
        val pending = ArrayDeque<Node>()
        val places = ArrayList<Place>(root.count)
        pending.add(root)
        while (pending.isNotEmpty()) {
            val node = pending.removeLast()
            if (node.placeIndex >= 0) places += source[node.placeIndex]
            else pending.addAll(node.children)
        }
        return places.sortedBy { "place:${it.id}" }
    }

    private fun Node.toMapNode(): MapClusterNode = if (placeIndex >= 0) {
        val place = source[placeIndex]
        MapClusterNode.Single(place, place.longitude!!, place.latitude!!)
    } else {
        MapClusterNode.Group(id, worldLng * 360.0 - 180.0,
            atan(sinh(PI * (1.0 - 2.0 * worldLat))) * 180.0 / PI, count)
    }

    private class Node(
        val worldLng: Double,
        val worldLat: Double,
        val count: Int,
        val placeIndex: Int,
        val id: Long = -1,
        val zoom: Int = 0,
        val children: List<Node> = emptyList(),
    )

    private class RadiusGrid(private val nodes: List<Node>, private val radius: Double) {
        private val buckets = HashMap<Long, MutableList<Int>>()

        init {
            nodes.forEachIndexed { index, node ->
                val column = floor(node.worldLng / radius).toLong()
                val row = floor(node.worldLat / radius).toLong()
                buckets.getOrPut(cellKey(column, row)) { ArrayList() }.add(index)
            }
        }

        fun nearby(seed: Node): List<Int> {
            val column = floor(seed.worldLng / radius).toLong()
            val row = floor(seed.worldLat / radius).toLong()
            val result = ArrayList<Int>()
            for (offsetLng in -1L..1L) {
                for (offsetLat in -1L..1L) {
                    val bucket = buckets[cellKey(column + offsetLng, row + offsetLat)].orEmpty()
                    for (index in bucket) {
                        val deltaLng = nodes[index].worldLng - seed.worldLng
                        val deltaLat = nodes[index].worldLat - seed.worldLat
                        if (deltaLng * deltaLng + deltaLat * deltaLat <= radius * radius) {
                            result += index
                        }
                    }
                }
            }
            result.sort()
            return result
        }
    }
}

internal fun normalize(places: List<Place>): List<Place> = places.asSequence()
    .filter { place ->
        val longitude = place.longitude
        val latitude = place.latitude
        place.id != null && longitude != null && latitude != null &&
            longitude.isFinite() && latitude.isFinite() &&
            longitude in -180.0..180.0 && latitude in -MAX_MAP_LAT..MAX_MAP_LAT
    }
    .distinctBy { it.id }
    .sortedBy { "place:${it.id}" }
    .map { it.copy(longitude = floor(it.longitude!! * 1e7 + 0.5) / 1e7,
        latitude = floor(it.latitude!! * 1e7 + 0.5) / 1e7) }
    .toList()

private const val TILE_SIZE = 256.0
internal const val MAX_MAP_LAT = 85.0511
private val clusterIds = AtomicLong(1L shl 40)

private fun cellKey(column: Long, row: Long): Long =
    (column shl 32) or (row and 0xffffffffL)

private fun worldX(longitude: Double): Double = longitude / 360.0 + 0.5

private fun worldY(latitude: Double): Double {
    val sine = sin(latitude.coerceIn(-MAX_MAP_LAT, MAX_MAP_LAT) * PI / 180.0)
    return (0.5 - 0.25 * ln((1.0 + sine) / (1.0 - sine)) / PI).coerceIn(0.0, 1.0)
}

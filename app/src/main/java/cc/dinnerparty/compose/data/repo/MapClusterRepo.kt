package cc.dinnerparty.compose.data.repo

import cc.dinnerparty.compose.api.model.Place
import cc.dinnerparty.compose.data.cluster.ClusterClick
import cc.dinnerparty.compose.data.cluster.ClusterViewport
import cc.dinnerparty.compose.data.cluster.ClusterSnapshot
import cc.dinnerparty.compose.data.cluster.MapClusterNode
import cc.dinnerparty.compose.data.cluster.PlaceClusterIndex
import cc.dinnerparty.compose.data.cluster.expanded
import cc.dinnerparty.compose.data.cluster.normalize
import cc.dinnerparty.compose.data.tiles.TileBatch
import cc.dinnerparty.compose.data.tiles.TileLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

class MapClusterRepo(
    private val fetchPlaces: suspend (ClusterViewport, Boolean) -> TileBatch,
) {
    private var index: PlaceClusterIndex? = null
    private var indexed: List<Place> = emptyList()
    private var maxZoom = 0
    private var radiusPx = 0.0
    private var revision = 0L
    private var sourceLevel: TileLevel? = null
    private var sourceZoom = 0f
    var warning: String? = null
        private set

    fun query(view: ClusterViewport): List<MapClusterNode> =
        index?.query(view.bounds.expanded(0.35), view.zoom).orEmpty()

    fun snapshot(view: ClusterViewport): ClusterSnapshot {
        val pending = sourceLevel?.let { view.zoom < it.minZoom || view.zoom > it.maxZoom } == true
        val shown = if (pending) view.copy(zoom = sourceZoom) else view
        return ClusterSnapshot(index, query(shown), shown.zoom.toInt())
    }

    fun expansionZoom(clusterId: Long): Int? = index?.expansionZoom(clusterId)

    fun clusterClick(clusterId: Long, zoom: Float, maxZoom: Float): ClusterClick? {
        val anonymous = indexed.firstOrNull { it.id == clusterId }?.aggregateZoom
        if (anonymous != null) {
            return ClusterClick.Zoom(min(maxZoom, max(anonymous.toFloat(), ceil(zoom + 0.01f))))
        }
        val current = index ?: return null
        val members = current.members(clusterId)
        val nextBand = members.mapNotNull { it.aggregateZoom }.minOrNull()
        if (nextBand != null) {
            return ClusterClick.Zoom(min(maxZoom, max(nextBand.toFloat(), ceil(zoom + 0.01f))))
        }
        val expansion = current.expansionZoom(clusterId) ?: return null
        return if (expansion > maxZoom || zoom >= maxZoom - 0.05f) {
            ClusterClick.Members(current.members(clusterId))
        } else {
            ClusterClick.Zoom(min(maxZoom, max(expansion.toFloat(), ceil(zoom + 0.01f))))
        }
    }

    suspend fun update(view: ClusterViewport, force: Boolean = false): List<Place> {
        val ticket = ++revision
        val incoming = fetchPlaces(view, force)
        currentCoroutineContext().ensureActive()
        if (ticket != revision) return indexed
        warning = incoming.warning
        val places = normalize(incoming.places)
        val topology = places.map { Triple(it.id, it.longitude, it.latitude) }
        val previous = indexed.map { Triple(it.id, it.longitude, it.latitude) }
        val changed = index == null || topology != previous ||
            maxZoom != view.mapMaxZoom || radiusPx != view.radiusPx
        if (!changed) {
            index?.updatePlaces(places)
            indexed = places
            sourceLevel = incoming.level
            sourceZoom = view.zoom
            return places
        }
        val rebuilt = withContext(Dispatchers.Default) {
            PlaceClusterIndex(places, view.mapMaxZoom, view.radiusPx)
        }
        currentCoroutineContext().ensureActive()
        if (ticket != revision) return indexed
        index = rebuilt
        indexed = places
        maxZoom = view.mapMaxZoom
        radiusPx = view.radiusPx
        sourceLevel = incoming.level
        sourceZoom = view.zoom
        return places
    }
}

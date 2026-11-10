package cc.dinnerparty.compose.data.repo

import cc.dinnerparty.compose.api.model.Place
import cc.dinnerparty.compose.data.cluster.ClusterClick
import cc.dinnerparty.compose.data.cluster.ClusterViewport
import cc.dinnerparty.compose.data.cluster.ClusterSnapshot
import cc.dinnerparty.compose.data.cluster.MapClusterNode
import cc.dinnerparty.compose.data.cluster.PlaceClusterIndex
import cc.dinnerparty.compose.data.cluster.PlaceRangeCache
import cc.dinnerparty.compose.data.cluster.expanded
import cc.dinnerparty.compose.data.cluster.normalize
import cc.dinnerparty.compose.data.model.MapBounds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

class MapClusterRepo(
    private val fetchPlaces: suspend (MapBounds) -> List<Place>,
    private val cache: PlaceRangeCache = PlaceRangeCache(),
) {
    private var index: PlaceClusterIndex? = null
    private var indexed: List<Place> = emptyList()
    private var maxZoom = 0
    private var radiusPx = 0.0
    private var revision = 0L

    fun query(view: ClusterViewport): List<MapClusterNode> =
        index?.query(view.bounds.expanded(0.35), view.zoom).orEmpty()

    fun snapshot(view: ClusterViewport): ClusterSnapshot =
        ClusterSnapshot(index, query(view), view.zoom.toInt())

    fun expansionZoom(clusterId: Long): Int? = index?.expansionZoom(clusterId)

    fun clusterClick(clusterId: Long, zoom: Float, maxZoom: Float): ClusterClick? {
        val current = index ?: return null
        val expansion = current.expansionZoom(clusterId) ?: return null
        return if (expansion > maxZoom || zoom >= maxZoom - 0.05f) {
            ClusterClick.Members(current.members(clusterId))
        } else {
            ClusterClick.Zoom(min(maxZoom, max(expansion.toFloat(), ceil(zoom + 0.01f))))
        }
    }

    suspend fun update(view: ClusterViewport, force: Boolean = false): List<Place> {
        val ticket = ++revision
        if (force) cache.invalidate()
        if (cache.needsFetch(view.bounds)) {
            val target = view.bounds.expanded(1.0)
            val incoming = fetchPlaces(target)
            currentCoroutineContext().ensureActive()
            if (ticket != revision) return indexed
            cache.apply(target, incoming)
        }
        val places = normalize(cache.all())
        val topology = places.map { Triple(it.id, it.longitude, it.latitude) }
        val previous = indexed.map { Triple(it.id, it.longitude, it.latitude) }
        val changed = index == null || topology != previous ||
            maxZoom != view.mapMaxZoom || radiusPx != view.radiusPx
        if (!changed) {
            index?.updatePlaces(places)
            indexed = places
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
        return places
    }
}

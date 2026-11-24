package cc.dinnerparty.compose.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cc.dinnerparty.compose.api.model.Place
import cc.dinnerparty.compose.data.cluster.MapClusterNode
import cc.dinnerparty.compose.data.cluster.ClusterClick
import cc.dinnerparty.compose.data.cluster.ClusterViewport
import cc.dinnerparty.compose.data.cluster.ClusterSnapshot
import cc.dinnerparty.compose.data.cluster.logicalZoom
import cc.dinnerparty.compose.data.model.MapBounds
import cc.dinnerparty.compose.data.repo.PlaceRepo
import cc.dinnerparty.compose.data.repo.MapClusterRepo
import cc.dinnerparty.compose.data.repo.MapTileRepo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 地图页状态。 */
data class MapUiState(
    val places: List<Place> = emptyList(),
    /** 当前视野里应该显示哪些节点，已按缩放级别聚合。 */
    val visibleNodes: List<MapClusterNode> = emptyList(),
    val clusterSnapshot: ClusterSnapshot? = null,
    val isLoading: Boolean = false,
    /** 错误文案，含后端给的中文提示。 */
    val errorMessage: String? = null,
    /** 被点开的地点，null 表示没有卡片。 */
    val selectedPlace: Place? = null,
    val clusterPlaces: List<Place>? = null,
)

/** 维护地图页的地点、聚合节点与选中状态。 */
class MapViewModel(application: Application) : AndroidViewModel(application) {

    private val placeRepo = PlaceRepo()
    private val tileRepo = MapTileRepo(application.cacheDir)

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private val clusterRepo = MapClusterRepo(tileRepo::update)
    private var viewport: ClusterViewport? = null
    private var loadJob: Job? = null
    private var logicalLevel: Int? = null
    private var detailJob: Job? = null
    private var selectionId: Long? = null

    fun updateViewport(
        bounds: MapBounds,
        zoom: Float,
        mapMaxZoom: Int,
        clusterRadiusPx: Double,
    ) {
        val level = logicalZoom(zoom, logicalLevel, mapMaxZoom)
        logicalLevel = level
        val next = ClusterViewport(bounds, level.toFloat(), mapMaxZoom, clusterRadiusPx)
        if (next == viewport && loadJob?.isActive == true) return
        viewport = next
        publishNodes()
        loadPlaces(next)
    }

    fun selectPlace(placeId: Long) {
        val place = _uiState.value.places.first { it.id == placeId }
        check(place.aggregateZoom == null)
        _uiState.update { it.copy(selectedPlace = place, clusterPlaces = null) }
        openPlace(placeId)
    }

    /** 收起卡片。 */
    fun clearSelection() {
        selectionId = null
        detailJob?.cancel()
        _uiState.update { it.copy(selectedPlace = null, clusterPlaces = null) }
    }

    fun openPlace(placeId: Long) {
        selectionId = placeId
        detailJob?.cancel()
        detailJob = viewModelScope.launch {
            try {
                val place = placeRepo.getPlace(placeId)
                if (selectionId == placeId) {
                    _uiState.update { it.copy(selectedPlace = place, clusterPlaces = null) }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                currentCoroutineContext().ensureActive()
                _uiState.update { it.copy(errorMessage = error.message) }
            }
        }
    }

    fun clusterClick(clusterId: Long, zoom: Float, maxZoom: Float): ClusterClick? {
        val action = clusterRepo.clusterClick(clusterId, zoom, maxZoom) ?: return null
        val members = (action as? ClusterClick.Members)?.places
        clearSelection()
        _uiState.update { it.copy(clusterPlaces = members) }
        return action
    }

    fun clearCluster() {
        _uiState.update { it.copy(clusterPlaces = null) }
    }

    fun selectMember(placeId: Long): Place? {
        val place = _uiState.value.clusterPlaces?.firstOrNull { it.id == placeId } ?: return null
        _uiState.update { it.copy(selectedPlace = place, clusterPlaces = null) }
        openPlace(placeId)
        return place
    }

    fun reload(place: Place) {
        tileRepo.patch(place)
        val current = viewport ?: return
        loadPlaces(current, force = true)
    }

    private fun loadPlaces(view: ClusterViewport, force: Boolean = false) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val places = clusterRepo.update(view, force)
                _uiState.update {
                    it.copy(places = places, isLoading = false, errorMessage = clusterRepo.warning)
                }
                publishNodes()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                currentCoroutineContext().ensureActive()
                _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
            }
        }
    }

    private fun publishNodes() {
        val current = viewport ?: return
        val snapshot = clusterRepo.snapshot(current)
        _uiState.update { it.copy(visibleNodes = snapshot.nodes, clusterSnapshot = snapshot) }
    }
}

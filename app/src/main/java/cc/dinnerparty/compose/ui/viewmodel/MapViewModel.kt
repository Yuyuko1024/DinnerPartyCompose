package cc.dinnerparty.compose.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cc.dinnerparty.compose.api.model.Place
import cc.dinnerparty.compose.data.cluster.MapClusterNode
import cc.dinnerparty.compose.data.cluster.PlaceClusterIndex
import cc.dinnerparty.compose.data.model.MapBounds
import cc.dinnerparty.compose.data.repo.PlaceRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 地图页状态。 */
data class MapUiState(
    val places: List<Place> = emptyList(),
    /** 当前视野里应该显示哪些节点，已按缩放级别聚合。 */
    val visibleNodes: List<MapClusterNode> = emptyList(),
    val isLoading: Boolean = false,
    /** 错误文案，含后端给的中文提示。 */
    val errorMessage: String? = null,
    /** 被点开的地点，null 表示没有卡片。 */
    val selectedPlace: Place? = null,
)

/** 维护地图页的地点、聚合节点与选中状态。 */
class MapViewModel(
    private val placeRepo: PlaceRepo = PlaceRepo(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    /** 上一次请求过的范围。 */
    private var requestedBounds: MapBounds? = null

    /** 聚合索引，地点列表变化时重建。 */
    private var clusterIndex: PlaceClusterIndex? = null

    /** 最近一次视野。 */
    private var viewport: Viewport? = null

    /** 地图能达到的最大缩放级别，由界面层带上来。 */
    private var mapMaxZoom: Int = 0

    /** 聚合半径（屏幕像素），同样由界面层带上来。 */
    private var clusterRadiusPx: Double = 0.0

    /**
     * 视野变化时重算可见节点，必要时拉取这一带的地点。
     *
     * @param mapMaxZoom 地图能达到的最大缩放级别。
     * @param clusterRadiusPx 聚合半径（屏幕像素）。
     */
    fun updateViewport(
        bounds: MapBounds,
        zoom: Float,
        mapMaxZoom: Int,
        clusterRadiusPx: Double,
    ) {
        viewport = Viewport(bounds, zoom)
        this.mapMaxZoom = mapMaxZoom
        this.clusterRadiusPx = clusterRadiusPx
        publishVisibleNodes()
        if (bounds != requestedBounds) loadPlaces(bounds)
    }

    /** 选中某个地点。 */
    fun selectPlace(placeId: Long) {
        _uiState.update { state ->
            state.copy(selectedPlace = state.places.firstOrNull { it.id == placeId })
        }
    }

    /** 收起卡片。 */
    fun clearSelection() {
        _uiState.update { it.copy(selectedPlace = null) }
    }

    /** 聚合散开所需的缩放级别，不会超过 [mapMaxZoom]。 */
    fun expansionZoomFor(clusterId: Long): Int? = clusterIndex?.expansionZoom(clusterId)

    /** 拉取指定范围内的地点。 */
    private fun loadPlaces(bounds: MapBounds) {
        requestedBounds = bounds

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching { placeRepo.getNearbyPlaces(bounds) }
                .onSuccess { places ->
                    // 建索引放到 Default 线程
                    val index = withContext(Dispatchers.Default) {
                        PlaceClusterIndex(
                            places = places,
                            mapMaxZoom = mapMaxZoom,
                            radiusPx = clusterRadiusPx,
                        )
                    }
                    clusterIndex = index
                    _uiState.update {
                        it.copy(places = places, isLoading = false, errorMessage = null)
                    }
                    publishVisibleNodes()
                }
                .onFailure { error ->
                    // 清掉记录的范围
                    requestedBounds = null
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = error.message)
                    }
                }
        }
    }

    /** 按当前视野筛出该显示的节点。 */
    private fun publishVisibleNodes() {
        val current = viewport ?: return
        val nodes = clusterIndex?.query(current.bounds, current.zoom).orEmpty()
        _uiState.update { it.copy(visibleNodes = nodes) }
    }
}

/** 一次视野快照，含范围和缩放级别。 */
private data class Viewport(val bounds: MapBounds, val zoom: Float)

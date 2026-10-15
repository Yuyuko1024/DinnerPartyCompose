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
    /**
     * 当前视野里应该显示哪些节点。原始地点见 [places]，这里已经按缩放级别聚合过：
     * 缩小时一批挨得近的地点会被合并成一个气泡。
     */
    val visibleNodes: List<MapClusterNode> = emptyList(),
    val isLoading: Boolean = false,
    /** 网络或后端错误文案，已含后端给的中文提示。 */
    val errorMessage: String? = null,
    /** 被点开的地点，卡片跟着它显示；null 表示没有卡片。 */
    val selectedPlace: Place? = null,
)

/**
 * 地图页 ViewModel。只管三件事：当前视野里有哪些地点、按缩放级别把它们聚成什么样、
 * 用户点开了哪一个。
 *
 * 聚合是纯计算，全部交给 [PlaceClusterIndex]；卡片长什么样、按钮点了干什么，
 * 都是界面层的决定，不要往这里堆，否则迟早变成上帝类。
 */
class MapViewModel(
    private val placeRepo: PlaceRepo = PlaceRepo(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    /**
     * 上一次请求过的范围。视野会随着拖动持续变化，范围没变就该直接吞掉，
     * 不然拖一下地图能打出十几条重复请求。
     */
    private var requestedBounds: MapBounds? = null

    /** 聚合索引。地点列表变了重建，光视野变只重新查。 */
    private var clusterIndex: PlaceClusterIndex? = null

    /** 最近一次视野。数据回来时要用它算出该显示哪些节点。 */
    private var viewport: Viewport? = null

    /**
     * 视野变了。两个后果：重算该显示哪些节点；这一带的地点还没拉过的话去拉一次。
     *
     * 拖动和缩放过程中就会反复调到这里，所以每一步都必须轻——查索引是 O(n) 的纯内存操作，
     * 而重拉数据有范围去重挡着，不会真的发出去。
     */
    fun updateViewport(bounds: MapBounds, zoom: Float) {
        viewport = Viewport(bounds, zoom)
        publishVisibleNodes()
        if (bounds != requestedBounds) loadPlaces(bounds)
    }

    /**
     * 选中某个地点，地图上随之弹出卡片。
     *
     * 地点是从已经拉到的那份列表里按 id 取的，所以不用再发一次详情请求——
     * `places/nearby` 返回的字段和 `places/{id}` 完全一致，卡片要用的都在里面。
     */
    fun selectPlace(placeId: Long) {
        _uiState.update { state ->
            state.copy(selectedPlace = state.places.firstOrNull { it.id == placeId })
        }
    }

    /** 收起卡片。点地图空白处、点卡片右上角都会走这里。 */
    fun clearSelection() {
        _uiState.update { it.copy(selectedPlace = null) }
    }

    /**
     * 点击聚合气泡时问：放大到哪一级它才彻底散开？
     *
     * 返回值大于 20 说明放到最大也散不开（几个地点坐标重合或贴得太近），
     * 调用方应该改用别的办法展示，而不是傻乎乎地放大。
     */
    fun expansionZoomFor(clusterId: Long): Int? = clusterIndex?.expansionZoom(clusterId)

    /** 拉取指定范围内的地点。 */
    private fun loadPlaces(bounds: MapBounds) {
        requestedBounds = bounds

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching { placeRepo.getNearbyPlaces(bounds) }
                .onSuccess { places ->
                    // 建索引要跑满 18 个聚合层级，点数多了是实打实的 CPU 活，别占着主线程
                    val index = withContext(Dispatchers.Default) { PlaceClusterIndex(places) }
                    clusterIndex = index
                    _uiState.update {
                        it.copy(places = places, isLoading = false, errorMessage = null)
                    }
                    publishVisibleNodes()
                }
                .onFailure { error ->
                    // 失败不算数：同一个范围要允许重试，否则用户来回拖一次就没法重新加载了
                    requestedBounds = null
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = error.message)
                    }
                }
        }
    }

    /**
     * 按当前视野把该显示的节点挑出来。
     *
     * 聚合结果不变时 [MapUiState] 整体相等，StateFlow 会自己吞掉，不会白白触发一轮重组。
     */
    private fun publishVisibleNodes() {
        val current = viewport ?: return
        val nodes = clusterIndex?.query(current.bounds, current.zoom).orEmpty()
        _uiState.update { it.copy(visibleNodes = nodes) }
    }
}

/** 一次视野快照：看得见哪一块、缩放到哪一级。 */
private data class Viewport(val bounds: MapBounds, val zoom: Float)

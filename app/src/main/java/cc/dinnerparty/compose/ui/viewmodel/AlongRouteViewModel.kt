package cc.dinnerparty.compose.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cc.dinnerparty.compose.R
import cc.dinnerparty.compose.api.model.Category
import cc.dinnerparty.compose.api.model.Place
import cc.dinnerparty.compose.data.model.AlongRouteMode
import cc.dinnerparty.compose.data.model.AlongRoutePlan
import cc.dinnerparty.compose.data.model.GeoPoint
import cc.dinnerparty.compose.data.model.RouteEndpoint
import cc.dinnerparty.compose.data.repo.AlongRouteLogTag
import cc.dinnerparty.compose.data.repo.AlongRouteRepo
import cc.dinnerparty.compose.data.repo.PlaceRepo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 顺路吃里正在选择的端点 */
enum class RouteTarget {
    Origin,
    Destination,
}

/** 顺路吃的三步 起终点 筛选条件 方案与结果 */
enum class AlongRouteStep {
    Endpoints,
    Options,
    Routes,
}

/** 顺路吃的状态 */
data class AlongRouteUiState(
    val step: AlongRouteStep = AlongRouteStep.Endpoints,
    val origin: RouteEndpoint? = null,
    val destination: RouteEndpoint? = null,
    /** 非空表示正在地图上取点 */
    val picking: RouteTarget? = null,
    /** 非空表示正在输入端点名称 */
    val editing: RouteTarget? = null,
    val input: String = "",
    val tips: List<RouteEndpoint> = emptyList(),
    val mode: AlongRouteMode = AlongRouteMode.Driving,
    val query: String = "",
    val tagInput: String = "",
    val categories: List<Category> = emptyList(),
    val chosenCategories: Set<String> = emptySet(),
    val corridorMeters: Int = DefaultCorridorMeters,
    val routes: List<AlongRoutePlan> = emptyList(),
    val chosenRoute: Int? = null,
    /** 已检索到的沿线地点 null 表示还没检索 */
    val places: List<Place>? = null,
    val busy: Boolean = false,
    val error: String? = null,
)

/** 顺路吃的状态与操作 */
class AlongRouteViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = AlongRouteRepo(application)
    private val placeRepo = PlaceRepo()

    private val _uiState = MutableStateFlow(AlongRouteUiState())
    val uiState: StateFlow<AlongRouteUiState> = _uiState.asStateFlow()

    private var planJob: Job? = null
    private var categoryJob: Job? = null
    private var tipJob: Job? = null

    /** 打开面板 从选起终点开始 */
    fun open() {
        stopEdit()
        _uiState.update { it.copy(step = AlongRouteStep.Endpoints, error = null) }
        if (_uiState.value.categories.isNotEmpty()) return
        categoryJob?.cancel()
        categoryJob = viewModelScope.launch {
            try {
                val categories = placeRepo.getCategories()
                _uiState.update { it.copy(categories = categories) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _uiState.update { it.copy(error = error.message) }
            }
        }
    }

    /** 收起面板并放弃尚未完成的请求 */
    fun close() {
        planJob?.cancel()
        stopEdit()
        _uiState.update { it.copy(picking = null, busy = false) }
    }

    /** 起终点齐了就进入筛选步骤 */
    fun next() {
        val state = _uiState.value
        if (state.origin == null || state.destination == null) {
            _uiState.update { it.copy(error = text(R.string.along_route_missing_points)) }
            return
        }
        stopEdit()
        _uiState.update { it.copy(step = AlongRouteStep.Options, picking = null, error = null) }
    }

    /** 回到上一步 方案页回筛选页 其余回起终点页 */
    fun back() {
        stopEdit()
        _uiState.update {
            val prev = if (it.step == AlongRouteStep.Routes) {
                AlongRouteStep.Options
            } else {
                AlongRouteStep.Endpoints
            }
            it.copy(step = prev)
        }
    }

    /** 开始在地图上取起点或终点 */
    fun pick(target: RouteTarget) {
        stopEdit()
        _uiState.update { it.copy(picking = target, error = null) }
    }

    /** 点输入框开始输入端点名称 */
    fun edit(target: RouteTarget) {
        if (_uiState.value.editing == target) return
        tipJob?.cancel()
        _uiState.update {
            it.copy(editing = target, picking = null, input = "", tips = emptyList())
        }
    }

    /** 输入框失焦 放弃输入的内容 指定端点时只在它仍是输入目标时才清 */
    fun stopEdit(target: RouteTarget? = null) {
        if (target != null && _uiState.value.editing != target) return
        tipJob?.cancel()
        _uiState.update { it.copy(editing = null, input = "", tips = emptyList()) }
    }

    /** 输入名称时按停顿拉取联想 */
    fun setInput(text: String) {
        _uiState.update { it.copy(input = text, error = null) }
        tipJob?.cancel()
        if (text.isBlank()) {
            _uiState.update { it.copy(tips = emptyList()) }
            return
        }
        tipJob = viewModelScope.launch {
            delay(TipDelayMs)
            try {
                val tips = repo.suggest(text)
                _uiState.update { state ->
                    if (state.input == text) state.copy(tips = tips) else state
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _uiState.update { it.copy(tips = emptyList(), error = error.message) }
            }
        }
    }

    /** 选中联想结果作为端点 */
    fun chooseTip(endpoint: RouteEndpoint) {
        val target = _uiState.value.editing ?: return
        applyEndpoint(target, endpoint)
    }

    /** 地图上点到的位置作为当前选点 */
    fun pickAt(point: GeoPoint, name: String) {
        val target = _uiState.value.picking ?: return
        applyEndpoint(target, RouteEndpoint(point, name))
    }

    /** 取消取点或输入 */
    fun cancelPick() {
        stopEdit()
        _uiState.update { it.copy(picking = null) }
    }

    /** 端点变化后之前的方案与结果都作废 */
    private fun applyEndpoint(target: RouteTarget, endpoint: RouteEndpoint) {
        planJob?.cancel()
        stopEdit()
        _uiState.update {
            it.copy(
                origin = if (target == RouteTarget.Origin) endpoint else it.origin,
                destination = if (target == RouteTarget.Destination) {
                    endpoint
                } else {
                    it.destination
                },
                picking = null,
                routes = emptyList(),
                chosenRoute = null,
                places = null,
                busy = false,
            )
        }
    }

    fun setMode(mode: AlongRouteMode) {
        _uiState.update {
            it.copy(mode = mode, routes = emptyList(), chosenRoute = null, places = null)
        }
    }

    fun setQuery(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    fun setTagInput(text: String) {
        _uiState.update { it.copy(tagInput = text) }
    }

    fun setCorridor(meters: Int) {
        _uiState.update { it.copy(corridorMeters = meters) }
    }

    fun toggleCategory(name: String) {
        _uiState.update {
            val next = if (name in it.chosenCategories) {
                it.chosenCategories - name
            } else {
                it.chosenCategories + name
            }
            it.copy(chosenCategories = next)
        }
    }

    /** 已选方案时只重新检索 否则先规划路线 */
    fun plan() {
        val state = _uiState.value
        state.chosenRoute?.let { index ->
            if (index in state.routes.indices) {
                choose(index)
                return
            }
        }
        val origin = state.origin
        val destination = state.destination
        if (origin == null || destination == null) {
            _uiState.update { it.copy(error = text(R.string.along_route_missing_points)) }
            return
        }
        planJob?.cancel()
        planJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    busy = true,
                    error = null,
                    routes = emptyList(),
                    chosenRoute = null,
                    places = null,
                )
            }
            try {
                val routes = repo.plan(state.mode, origin.point, destination.point)
                if (routes.isEmpty()) {
                    _uiState.update {
                        it.copy(busy = false, error = text(R.string.along_route_no_route))
                    }
                    return@launch
                }
                _uiState.update {
                    it.copy(routes = routes, busy = false, step = AlongRouteStep.Routes)
                }
                if (routes.size == 1) {
                    search(routes.first(), 0)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.e(AlongRouteLogTag, "plan failed", error)
                _uiState.update { it.copy(busy = false, error = error.message) }
            }
        }
    }

    /** 选定一个方案并检索沿线地点 */
    fun choose(index: Int) {
        val route = _uiState.value.routes.getOrNull(index) ?: return
        planJob?.cancel()
        planJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    chosenRoute = index,
                    busy = true,
                    error = null,
                    step = AlongRouteStep.Routes,
                )
            }
            try {
                search(route, index)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.e(AlongRouteLogTag, "search failed", error)
                _uiState.update { it.copy(busy = false, error = error.message) }
            }
        }
    }

    /** 失败后按当前阶段重试 */
    fun retry() {
        val chosen = _uiState.value.chosenRoute
        if (chosen == null) {
            plan()
        } else {
            choose(chosen)
        }
    }

    private suspend fun search(route: AlongRoutePlan, index: Int) {
        val state = _uiState.value
        val places = repo.search(
            paths = listOf(route.points),
            query = state.query.trim(),
            categories = state.chosenCategories.toList(),
            corridorMeters = state.corridorMeters,
        )
        _uiState.update {
            it.copy(places = places, chosenRoute = index, busy = false)
        }
    }

    private fun text(id: Int): String = getApplication<Application>().getString(id)
}

/** 离路线距离的默认值 单位米 */
private const val DefaultCorridorMeters = 1000

/** 输入联想到发出请求的停顿 单位毫秒 */
private const val TipDelayMs = 300L

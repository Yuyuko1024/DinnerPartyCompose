package cc.dinnerparty.compose.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cc.dinnerparty.compose.R
import cc.dinnerparty.compose.api.model.Place
import cc.dinnerparty.compose.data.repo.UserPlacesRepo
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 住民地点筛选状态 */
data class UserPlacesUiState(
    /** 正在筛选的住民 null 表示没有筛选 */
    val userId: String? = null,
    /** 已加载的地点 null 表示还没有可用结果 */
    val places: List<Place>? = null,
    /** 这位住民一共创建了多少个地点 */
    val total: Int = 0,
    /** 只列出了最近的一部分时 已列出的条数 */
    val limit: Int? = null,
    val loading: Boolean = false,
    val error: String? = null,
) {
    /** 地图是否按住民筛选 */
    val active: Boolean get() = userId != null

    /** 筛选已生效 地图只显示这些地点 */
    val ready: Boolean get() = places != null && error == null
}

/** 按住民筛选地图的数据与操作 */
class UserPlacesViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = UserPlacesRepo()

    private val _uiState = MutableStateFlow(UserPlacesUiState())
    val uiState: StateFlow<UserPlacesUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    /** 进入筛选并读取这位住民的地点 */
    fun open(userId: String) {
        if (userId.isBlank()) return
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = UserPlacesUiState(userId = userId, loading = true)
            runCatching { repo.getUserPlaces(userId) }
                .onSuccess { response ->
                    val places = response.places.orEmpty()
                    _uiState.value = UserPlacesUiState(
                        userId = userId,
                        places = places,
                        total = response.total ?: places.size,
                        limit = response.limit,
                    )
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            loading = false,
                            error = error.message ?: text(R.string.user_places_failed),
                        )
                    }
                }
        }
    }

    /** 加载失败后重试 */
    fun retry() {
        _uiState.value.userId?.let(::open)
    }

    /** 退出筛选 地图恢复显示附近地点 */
    fun exit() {
        loadJob?.cancel()
        _uiState.value = UserPlacesUiState()
    }

    private fun text(id: Int): String = getApplication<Application>().getString(id)
}

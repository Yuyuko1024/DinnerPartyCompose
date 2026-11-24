package cc.dinnerparty.compose.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cc.dinnerparty.compose.R
import cc.dinnerparty.compose.api.model.ProfilePlace
import cc.dinnerparty.compose.data.repo.UserFavoritesRepo
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 收藏夹状态 */
data class FavoritesUiState(
    /** 正在查看谁的收藏 null 表示还没有打开 */
    val userId: String? = null,
    /** 已加载的收藏 null 表示还没有可用结果 */
    val places: List<ProfilePlace>? = null,
    val loading: Boolean = false,
    val error: String? = null,
)

/** 收藏夹的数据与操作 */
class FavoritesViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = UserFavoritesRepo()

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    /** 打开这位住民的收藏 每次都重新读一次 */
    fun open(userId: String) {
        if (userId.isBlank()) return
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = FavoritesUiState(userId = userId, loading = true)
            runCatching { repo.getUserFavorites(userId) }
                .onSuccess { response ->
                    _uiState.value = FavoritesUiState(
                        userId = userId,
                        places = response.places.orEmpty(),
                    )
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            loading = false,
                            error = error.message ?: text(R.string.favorites_failed),
                        )
                    }
                }
        }
    }

    /** 加载失败后重试 */
    fun retry() {
        _uiState.value.userId?.let(::open)
    }

    /** 收起收藏夹 */
    fun close() {
        loadJob?.cancel()
        _uiState.value = FavoritesUiState()
    }

    private fun text(id: Int): String = getApplication<Application>().getString(id)
}

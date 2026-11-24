package cc.dinnerparty.compose.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cc.dinnerparty.compose.R
import cc.dinnerparty.compose.api.model.UserProfile
import cc.dinnerparty.compose.data.repo.ProfileRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 住民主页状态 */
data class ProfileUiState(
    val profile: UserProfile? = null,
    /** 加载或保存进行中 */
    val busy: Boolean = false,
    /** 首次加载失败时的内联文案 */
    val error: String? = null,
    /** 保存失败时的提示 */
    val message: String? = null,
)

/** 住民主页的数据与保存操作 */
class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = ProfileRepo()

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private var userId: String? = null

    /** 打开主页时加载一次 */
    fun load(id: String) {
        userId = id
        if (_uiState.value.busy) return
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true, error = null) }
            runCatching { repo.getProfile(id) }
                .onSuccess { profile ->
                    _uiState.update { it.copy(profile = profile, busy = false) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(busy = false, error = error.message ?: loadFailed())
                    }
                }
        }
    }

    /** 加载失败后重试 */
    fun retry() {
        userId?.let(::load)
    }

    /** 保存某一轴的喜好，传 null 回到自动推断 */
    fun saveTasteLevel(axis: String, level: Int?) = submit(R.string.profile_taste_failed) {
        val taste = repo.saveTaste(axis, level)
        _uiState.update { state ->
            state.copy(profile = state.profile?.copy(taste = taste))
        }
    }

    /** 保存可隐藏的小组件 */
    fun saveHiddenWidgets(hidden: List<String>) = submit(R.string.profile_widgets_failed) {
        val profile = _uiState.value.profile ?: return@submit
        val widgets = profile.widgets ?: DefaultWidgets
        val result = repo.saveWidgets(widgets, hidden)
        _uiState.update { state ->
            state.copy(
                profile = state.profile?.copy(
                    widgets = result.widgets ?: widgets,
                    hiddenWidgets = result.hiddenWidgets ?: hidden,
                )
            )
        }
    }

    /** 保存收藏公开设置 */
    fun saveVisibility(isPublic: Boolean) = submit(R.string.profile_widget_visibility_failed) {
        val result = repo.saveVisibility(isPublic)
        _uiState.update { state ->
            state.copy(profile = state.profile?.copy(collectionVisibility = result.isPublic))
        }
    }

    /** 清空保存失败的提示 */
    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun submit(fallback: Int, action: suspend () -> Unit) {
        if (_uiState.value.busy) return
        viewModelScope.launch {
            _uiState.update { it.copy(busy = true) }
            runCatching { action() }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(busy = false, message = error.message ?: text(fallback))
                    }
                }
            _uiState.update { it.copy(busy = false) }
        }
    }

    private fun loadFailed(): String = text(R.string.profile_load_failed)

    private fun text(id: Int): String = getApplication<Application>().getString(id)
}

private val DefaultWidgets = listOf("collection", "recent")

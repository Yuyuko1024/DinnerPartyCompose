package cc.dinnerparty.compose.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cc.dinnerparty.compose.data.model.SearchItem
import cc.dinnerparty.compose.data.repo.SearchRepo
import com.amap.api.maps.model.LatLng
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.CancellationException

/** 地图搜索状态 */
data class SearchUiState(
    val query: String = "",
    val siteResults: List<SearchItem> = emptyList(),
    /** 是否已点击搜索按钮执行全站检索 */
    val submitted: Boolean = false,
    val resultsVisible: Boolean = false,
    val isLoading: Boolean = false,
    val amapResults: List<SearchItem> = emptyList(),
    val amapSearched: Boolean = false,
    val isAmapLoading: Boolean = false,
    val errorMessage: String? = null,
    /** 等待地图定位到该结果 */
    val focusedItem: SearchItem? = null,
)

/** 维护搜索联想 全站检索与未收录地点 */
class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = SearchRepo(application)

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var amapJob: Job? = null
    private var mapCenter: LatLng? = null

    /** 记录地图中心 用于距离排序与未收录地点搜索 */
    fun updateCenter(center: LatLng) {
        mapCenter = center
    }

    /** 输入变化时重置检索状态并开始联想 */
    fun queryChange(query: String) {
        searchJob?.cancel()
        amapJob?.cancel()
        _uiState.update {
            it.copy(
                query = query,
                submitted = false,
                resultsVisible = query.isNotBlank(),
                siteResults = emptyList(),
                isLoading = query.isNotBlank(),
                amapResults = emptyList(),
                amapSearched = false,
                isAmapLoading = false,
                errorMessage = null,
                focusedItem = null,
            )
        }
        if (query.isBlank()) return
        searchJob = viewModelScope.launch {
            delay(DebounceMs)
            loadSite(query, LiveLimit)
        }
    }

    /** 搜索按钮 未检索时提交 已检索时展开或收起 */
    fun searchClick() {
        if (_uiState.value.submitted) {
            toggleResults()
        } else {
            submitSearch()
        }
    }

    /** 执行全站站内检索 */
    fun submitSearch() {
        val query = _uiState.value.query.trim()
        if (query.isEmpty()) return
        searchJob?.cancel()
        _uiState.update {
            it.copy(
                submitted = true,
                resultsVisible = true,
                isLoading = true,
                errorMessage = null,
            )
        }
        searchJob = viewModelScope.launch { loadSite(query, FullLimit) }
    }

    /** 查找高德未收录地点 */
    fun searchAmap() {
        val state = _uiState.value
        val query = state.query.trim()
        if (query.isEmpty() || state.amapSearched || state.isAmapLoading) return
        amapJob?.cancel()
        _uiState.update { it.copy(isAmapLoading = true, errorMessage = null) }
        amapJob = viewModelScope.launch {
            runCatching {
                repo.searchAmap(query, mapCenter, state.siteResults)
            }.onSuccess { results ->
                if (_uiState.value.query.trim() != query) return@onSuccess
                _uiState.update {
                    it.copy(
                        amapResults = results,
                        amapSearched = true,
                        isAmapLoading = false,
                        resultsVisible = true,
                    )
                }
            }.onFailure { error ->
                if (error is CancellationException) throw error
                _uiState.update {
                    it.copy(
                        isAmapLoading = false,
                        amapSearched = false,
                        errorMessage = error.message,
                    )
                }
            }
        }
    }

    /** 选中结果 收起列表并请求地图居中 */
    fun selectItem(item: SearchItem) {
        _uiState.update { it.copy(focusedItem = item, resultsVisible = false) }
    }

    /** 地图完成居中后清除定位请求 */
    fun consumeFocus() {
        _uiState.update { it.copy(focusedItem = null) }
    }

    /** 收起结果列表 */
    fun hideResults() {
        _uiState.update { it.copy(resultsVisible = false) }
    }

    /** 清空搜索框与所有结果 */
    fun clearSearch() {
        searchJob?.cancel()
        amapJob?.cancel()
        _uiState.value = SearchUiState()
    }

    private fun toggleResults() {
        _uiState.update { it.copy(resultsVisible = !it.resultsVisible) }
    }

    private suspend fun loadSite(query: String, limit: Int) {
        runCatching { repo.searchSite(query, mapCenter, limit) }
            .onSuccess { results ->
                if (_uiState.value.query.trim() != query) return@onSuccess
                _uiState.update {
                    it.copy(siteResults = results, isLoading = false, errorMessage = null)
                }
            }
            .onFailure { error ->
                if (error is CancellationException) throw error
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = error.message)
                }
            }
    }
}

/** 联想结果数量 */
private const val LiveLimit = 8

/** 全站检索结果数量 */
private const val FullLimit = 50

/** 输入防抖时长 */
private const val DebounceMs = 300L

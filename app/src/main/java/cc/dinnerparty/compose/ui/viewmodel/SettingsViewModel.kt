package cc.dinnerparty.compose.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cc.dinnerparty.compose.data.SettingsDataStore
import cc.dinnerparty.compose.data.model.MapCamera
import cc.dinnerparty.compose.data.model.MapDefaults
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 设置页状态 */
data class SettingsUiState(
    /** 是否启用液态玻璃 */
    val liquidGlass: Boolean = true,
    /** 亮暗是否跟随系统 */
    val followSystem: Boolean = true,
    /** 是否强制暗色 仅在未跟随系统时生效 */
    val darkTheme: Boolean = false,
    /** 主色 ARGB 为空表示使用品牌色 */
    val primary: Int? = null,
    /** 辅色 ARGB 为空表示使用品牌色 */
    val secondary: Int? = null,
    /** 默认中心纬度 */
    val centerLat: Double = MapDefaults.Latitude,
    /** 默认中心经度 */
    val centerLng: Double = MapDefaults.Longitude,
    /** 默认缩放等级 */
    val mapZoom: Float = MapDefaults.Zoom,
    /** 是否启用聚合标记动效 */
    val clusterMotion: Boolean = true,
    /** 是否启用弹窗与页面动效 */
    val overlayMotion: Boolean = true,
)

/** 读取并写入本地设置 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val store = SettingsDataStore(application)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            store.liquidGlass.collect { enabled ->
                _uiState.update { it.copy(liquidGlass = enabled) }
            }
        }
        viewModelScope.launch {
            store.followSystem.collect { enabled ->
                _uiState.update { it.copy(followSystem = enabled) }
            }
        }
        viewModelScope.launch {
            store.darkTheme.collect { enabled ->
                _uiState.update { it.copy(darkTheme = enabled) }
            }
        }
        viewModelScope.launch {
            store.primary.collect { argb ->
                _uiState.update { it.copy(primary = argb) }
            }
        }
        viewModelScope.launch {
            store.secondary.collect { argb ->
                _uiState.update { it.copy(secondary = argb) }
            }
        }
        viewModelScope.launch {
            store.centerLat.collect { value ->
                _uiState.update { it.copy(centerLat = value) }
            }
        }
        viewModelScope.launch {
            store.centerLng.collect { value ->
                _uiState.update { it.copy(centerLng = value) }
            }
        }
        viewModelScope.launch {
            store.mapZoom.collect { value ->
                _uiState.update { it.copy(mapZoom = value) }
            }
        }
        viewModelScope.launch {
            store.clusterMotion.collect { enabled ->
                _uiState.update { it.copy(clusterMotion = enabled) }
            }
        }
        viewModelScope.launch {
            store.overlayMotion.collect { enabled ->
                _uiState.update { it.copy(overlayMotion = enabled) }
            }
        }
    }

    /** 写入液态玻璃开关 */
    fun setGlass(enabled: Boolean) {
        viewModelScope.launch { store.setLiquidGlass(enabled) }
    }

    /** 写入跟随系统开关 */
    fun setFollowSystem(enabled: Boolean) {
        viewModelScope.launch { store.setFollowSystem(enabled) }
    }

    /** 写入强制暗色开关 */
    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch { store.setDarkTheme(enabled) }
    }

    /** 写入主色 */
    fun setPrimary(argb: Int?) {
        viewModelScope.launch { store.setPrimary(argb) }
    }

    /** 写入辅色 */
    fun setSecondary(argb: Int?) {
        viewModelScope.launch { store.setSecondary(argb) }
    }

    /** 写入地图默认视角 */
    fun setMapView(camera: MapCamera) {
        viewModelScope.launch { store.setMapView(camera) }
    }

    /** 读取地图默认视角 */
    suspend fun mapCamera(): MapCamera = store.mapCamera()

    /** 写入聚合标记动效开关 */
    fun setClusterMotion(enabled: Boolean) {
        viewModelScope.launch { store.setClusterMotion(enabled) }
    }

    /** 写入弹窗与页面动效开关 */
    fun setOverlayMotion(enabled: Boolean) {
        viewModelScope.launch { store.setOverlayMotion(enabled) }
    }
}

package cc.dinnerparty.compose.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cc.dinnerparty.compose.data.SettingsDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 设置页状态 */
data class SettingsUiState(
    /** 是否启用液态玻璃 */
    val liquidGlass: Boolean = true,
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
    }

    /** 写入液态玻璃开关 */
    fun setGlass(enabled: Boolean) {
        viewModelScope.launch { store.setLiquidGlass(enabled) }
    }
}

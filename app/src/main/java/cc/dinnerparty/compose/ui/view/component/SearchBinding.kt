package cc.dinnerparty.compose.ui.view.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cc.dinnerparty.compose.ui.viewmodel.SearchUiState
import cc.dinnerparty.compose.ui.viewmodel.SearchViewModel

/** 搜索状态与事件的组合 */
@Immutable
class MapSearchBinding(
    val state: SearchUiState,
    val actions: MapSearchActions,
)

/** 收集搜索状态并绑定事件 */
@Composable
fun rememberSearch(viewModel: SearchViewModel): MapSearchBinding {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val actions = remember(viewModel) {
        MapSearchActions(
            queryChange = viewModel::queryChange,
            searchClick = viewModel::searchClick,
            clearClick = viewModel::clearSearch,
            amapClick = viewModel::searchAmap,
            selectItem = viewModel::selectItem,
        )
    }
    return remember(state, actions) { MapSearchBinding(state, actions) }
}

package cc.dinnerparty.compose.ui.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/** 地图浮层，同一时刻只显示一个 */
enum class MapOverlay {
    /** 地点详情 */
    Place,

    /** 登录 */
    Login,

    /** 管理账户 */
    Account,

    /** 更多菜单 */
    Menu,

    /** 关于 */
    About
}

/** 浮层状态，null 表示什么都不显示 */
@Stable
class MapOverlayManager {
    var current by mutableStateOf<MapOverlay?>(null)
        private set

    /** 切到指定浮层 */
    fun show(overlay: MapOverlay) {
        current = overlay
    }

    /** 收起当前浮层 */
    fun hide() {
        current = null
    }

    /** 已展开则收起，否则展开 */
    fun toggle(overlay: MapOverlay) {
        current = if (current == overlay) null else overlay
    }
}

/** 创建横跨手机与平板布局的浮层状态 */
@Composable
fun rememberMapOverlayManager(): MapOverlayManager = remember { MapOverlayManager() }

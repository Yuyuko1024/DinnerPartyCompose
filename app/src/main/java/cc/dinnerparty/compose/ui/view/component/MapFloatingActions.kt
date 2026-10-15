package cc.dinnerparty.compose.ui.view.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cc.dinnerparty.compose.R

/**
 * 地图页右侧悬浮操作按钮组：定位 / 顺路吃。
 *
 * 定位按钮已经在用（申请位置权限），顺路吃还是空实现，等路线接口接入后再填逻辑。
 *
 * @param locating 正在定位。此时按钮换成实心图标、把「定位」读成「正在定位」，
 *   让用户知道那一下点击有反应——定位本身要等好几秒。
 */
@Composable
fun MapFloatingActions(
    modifier: Modifier = Modifier,
    locating: Boolean = false,
    onLocateClick: () -> Unit = {},
    onAlongRouteClick: () -> Unit = {}
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.End
    ) {
        CircleIconButton(
            imageVector = if (locating) Icons.Filled.MyLocation else Icons.Outlined.MyLocation,
            contentDescription = stringResource(
                if (locating) R.string.location_locating else R.string.action_locate
            ),
            onClick = onLocateClick
        )
        CircleIconButton(
            imageVector = Icons.AutoMirrored.Outlined.DirectionsWalk,
            contentDescription = stringResource(R.string.action_along_route),
            onClick = onAlongRouteClick
        )
    }
}

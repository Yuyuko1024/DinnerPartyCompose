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
 * 地图页右侧悬浮操作按钮组，含定位与顺路吃。
 *
 * @param locating 正在定位，按钮换成实心图标、无障碍文案改为「正在定位」。
 */
@Composable
fun MapFloatingActions(
    modifier: Modifier = Modifier,
    locating: Boolean = false,
    onLocateClick: () -> Unit = {},
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
    }
}

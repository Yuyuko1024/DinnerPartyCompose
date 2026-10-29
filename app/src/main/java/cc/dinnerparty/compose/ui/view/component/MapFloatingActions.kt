package cc.dinnerparty.compose.ui.view.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cc.dinnerparty.compose.R
import cc.dinnerparty.compose.ui.adaptive.LocalIsTablet
import cc.dinnerparty.compose.ui.theme.dinnerPartySemanticColors
import com.moriafly.salt.ui.SaltTheme

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
    addMode: Boolean = false,
    onAddClick: () -> Unit = {},
) {
    val isTablet = LocalIsTablet.current
    val danger = dinnerPartySemanticColors().danger

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.End
    ) {
        if (isTablet) {
            CircleIconButton(
                painterRes = R.drawable.ic_route_track_outline,
                contentDescription = stringResource(R.string.action_along_route),
                onClick = {  }
            )
            CircleIconButton(
                imageVector = if (addMode) Icons.Outlined.Close else Icons.Outlined.Add,
                contentDescription = stringResource(R.string.action_add),
                onClick = onAddClick,
                containerColor = if (addMode) {
                    danger
                } else {
                    Color.Transparent
                },
                contentColor = if (addMode) Color.White else SaltTheme.colors.text
            )
            CircleIconButton(
                painterRes = R.drawable.ic_heart_outline,
                contentDescription = stringResource(R.string.action_favorites),
                onClick = {  }
            )
        }
        CircleIconButton(
            imageVector = if (locating) Icons.Filled.MyLocation else Icons.Outlined.MyLocation,
            contentDescription = stringResource(
                if (locating) R.string.location_locating else R.string.action_locate
            ),
            onClick = onLocateClick
        )
    }

}

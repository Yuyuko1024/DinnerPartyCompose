package cc.dinnerparty.compose.ui.view.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cc.dinnerparty.compose.R

/** 一个地图操作项。 */
internal class MapAction(
    val imageVector: ImageVector? = null,
    val iconRes: Int? = null,
    val label: String,
    val onClick: () -> Unit,
    val iconSize: Dp = 24.dp,
    val highlighted: Boolean = false
)

/** 手机底栏组件 */
@Composable
internal fun phoneMapActions(
    onMoreClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onAddClick: () -> Unit,
    onAlongRouteClick: () -> Unit
): List<MapAction> = listOf(
    MapAction(
        imageVector = Icons.Outlined.MoreHoriz,
        label = stringResource(R.string.action_more),
        onClick = onMoreClick
    ),
    MapAction(
        iconRes = R.drawable.ic_heart_outline,
        label = stringResource(R.string.action_favorites),
        onClick = onFavoritesClick
    ),
    MapAction(
        imageVector = Icons.Outlined.AddCircleOutline,
        label = stringResource(R.string.action_add),
        onClick = onAddClick,
        iconSize = 27.dp,
        highlighted = true
    ),
    MapAction(
        iconRes = R.drawable.ic_route_track_outline,
        label = stringResource(R.string.action_along_route),
        onClick = onAlongRouteClick
    )
)

/** 平板菜单组件 */
@Composable
internal fun padMenuActions(
    onRandomClick: () -> Unit,
    onDiningClick: () -> Unit,
    onExportPostClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAboutClick: () -> Unit
): List<MapAction> = listOf(
    MapAction(
        label = stringResource(R.string.menu_random),
        onClick = onRandomClick
    ),
    MapAction(
        label = stringResource(R.string.menu_dining),
        onClick = onDiningClick
    ),
    MapAction(
        label = stringResource(R.string.menu_export_post),
        onClick = onExportPostClick,
    ),
    MapAction(
        label = stringResource(R.string.menu_settings),
        onClick = onSettingsClick
    ),
    MapAction(
        label = stringResource(R.string.menu_about),
        onClick = onAboutClick
    )
)

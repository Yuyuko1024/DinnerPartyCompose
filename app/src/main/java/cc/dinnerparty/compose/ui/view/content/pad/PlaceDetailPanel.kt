package cc.dinnerparty.compose.ui.view.content.pad

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cc.dinnerparty.compose.api.model.Place
import cc.dinnerparty.compose.ui.view.content.PlaceCardContent
import cc.dinnerparty.compose.ui.view.content.PlaceCardOps

/** 地点详情面板，从右侧滑入滑出。 */
@Composable
fun PlaceDetailPanel(
    modifier: Modifier = Modifier,
    place: Place? = null,
    onDismissRequest: () -> Unit = {},
    ops: PlaceCardOps = PlaceCardOps(),
) {
    PadPanel(
        value = place,
        modifier = modifier
    ) { shown ->
        PlaceCardContent(place = shown, onClose = onDismissRequest, ops = ops)
    }
}

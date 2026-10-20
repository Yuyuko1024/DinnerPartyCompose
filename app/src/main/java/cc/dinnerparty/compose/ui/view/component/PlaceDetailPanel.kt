package cc.dinnerparty.compose.ui.view.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cc.dinnerparty.compose.api.model.Place

/** 地点详情面板，从右侧滑入滑出。 */
@Composable
fun PlaceDetailPanel(
    modifier: Modifier = Modifier,
    place: Place? = null,
    onDismissRequest: () -> Unit = {},
    external: Boolean = false
) {
    PadPanel(
        value = place,
        modifier = modifier
    ) { shown ->
        PlaceCardContent(place = shown, onClose = onDismissRequest, external = external)
    }
}

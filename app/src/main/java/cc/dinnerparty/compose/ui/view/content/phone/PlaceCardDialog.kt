package cc.dinnerparty.compose.ui.view.content.phone

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cc.dinnerparty.compose.api.model.Place
import cc.dinnerparty.compose.ui.view.content.PlaceCardContent
import cc.dinnerparty.compose.ui.view.content.PlaceCardOps

/** 地点卡片浮层，从底部滑入滑出。 */
@Composable
fun PlaceCardDialog(
    modifier: Modifier = Modifier,
    place: Place?,
    onDismissRequest: () -> Unit,
    ops: PlaceCardOps = PlaceCardOps(),
) {
    PhoneSheet(
        value = place,
        onDismissRequest = onDismissRequest,
        modifier = modifier
    ) { shown ->
        PlaceCardContent(place = shown, onClose = onDismissRequest, ops = ops)
    }
}

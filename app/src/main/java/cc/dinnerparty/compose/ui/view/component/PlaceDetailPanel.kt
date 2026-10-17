package cc.dinnerparty.compose.ui.view.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import cc.dinnerparty.compose.api.model.Place
import com.moriafly.salt.ui.SaltTheme

/** 面板宽度。 */
private val PanelWidth = 360.dp

private val PanelTopPadding = 12.dp

private val PanelShape = RoundedCornerShape(16.dp)

/** 地点详情面板，从右侧滑入滑出。 */
@Composable
fun PlaceDetailPanel(
    modifier: Modifier = Modifier,
    place: Place? = null,
    onDismissRequest: () -> Unit = {}
) {
    // 滑入滑出动画期间保留面板内容
    var content by remember { mutableStateOf(place) }
    if (place != null) content = place

    AnimatedVisibility(
        visible = place != null,
        enter = slideInHorizontally { it },
        exit = slideOutHorizontally { it },
        modifier = modifier
    ) {
        content?.let {
            PlaceCardContent(
                place = it,
                onClose = onDismissRequest,
                modifier = Modifier
                    .width(PanelWidth)
                    .padding(PanelTopPadding)
                    .fillMaxHeight()
                    .consumeTouchEvents()
                    .clip(PanelShape)
                    .background(SaltTheme.colors.subBackground)
                    .border(1.dp, SaltTheme.colors.stroke, PanelShape)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .verticalScroll(rememberScrollState())
            )
        }
    }
}

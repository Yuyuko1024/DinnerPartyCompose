package cc.dinnerparty.compose.ui.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cc.dinnerparty.compose.api.model.Place
import cc.dinnerparty.compose.ui.view.component.AboutContent
import cc.dinnerparty.compose.ui.view.component.MapBottomBar
import cc.dinnerparty.compose.ui.view.component.MapFloatingActions
import cc.dinnerparty.compose.ui.view.component.MapMenuList
import cc.dinnerparty.compose.ui.view.component.MapSearchBar
import cc.dinnerparty.compose.ui.view.component.PhoneSheet
import cc.dinnerparty.compose.ui.view.component.PlaceCardDialog
import com.moriafly.salt.ui.UnstableSaltUiApi

/** 手机端浮层总高。 */
internal val PhoneOverlayHeight = 136.dp

/** 手机端地图浮层，底部依次是悬浮钮、搜索条、底栏，地点卡片从底部滑入。 */
@UnstableSaltUiApi
@Composable
fun MainPhoneUI(
    modifier: Modifier = Modifier,
    overlay: MapOverlayManager = rememberMapOverlayManager(),
    locating: Boolean = false,
    onLocateClick: () -> Unit = {},
    place: Place? = null,
    onDismissPlaceCard: () -> Unit = {}
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            MapFloatingActions(
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(end = 12.dp, bottom = 12.dp),
                locating = locating,
                onLocateClick = onLocateClick
            )
            MapSearchBar(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
            MapBottomBar(
                modifier = Modifier.fillMaxWidth(),
                onMoreClick = { overlay.show(MapOverlay.Menu) }
            )
        }

        PlaceCardDialog(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            place = place.takeIf { overlay.current == MapOverlay.Place },
            onDismissRequest = onDismissPlaceCard
        )

        PhoneSheet(
            visible = overlay.current == MapOverlay.Menu,
            onDismissRequest = overlay::hide,
            modal = true
        ) {
            MapMenuList(
                modifier = Modifier.padding(vertical = 8.dp),
                onDismiss = overlay::hide,
                onAboutClick = { overlay.show(MapOverlay.About) }
            )
        }

        PhoneSheet(
            visible = overlay.current == MapOverlay.About,
            onDismissRequest = overlay::hide,
            modal = true
        ) {
            AboutContent(onClose = overlay::hide)
        }
    }
}


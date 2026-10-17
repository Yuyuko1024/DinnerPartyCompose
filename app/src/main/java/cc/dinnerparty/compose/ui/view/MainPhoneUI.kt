package cc.dinnerparty.compose.ui.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    locating: Boolean = false,
    onLocateClick: () -> Unit = {},
    place: Place? = null,
    onDismissPlaceCard: () -> Unit = {}
) {
    // 菜单浮层是否展开
    var menuExpanded by remember { mutableStateOf(false) }

    // 关于浮层是否展开
    var aboutExpanded by remember { mutableStateOf(false) }

    // 打开地点卡片时收起菜单
    LaunchedEffect(place) {
        if (place != null) menuExpanded = false
    }

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
                onMoreClick = { menuExpanded = true }
            )
        }

        PlaceCardDialog(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            place = place,
            onDismissRequest = onDismissPlaceCard
        )

        PhoneSheet(
            visible = menuExpanded,
            onDismissRequest = { menuExpanded = false },
            modal = true
        ) {
            MapMenuList(
                modifier = Modifier.padding(vertical = 8.dp),
                onDismiss = { menuExpanded = false },
                onAboutClick = { aboutExpanded = true }
            )
        }

        PhoneSheet(
            visible = aboutExpanded,
            onDismissRequest = { aboutExpanded = false },
            modal = true
        ) {
            AboutContent(onClose = { aboutExpanded = false })
        }
    }
}


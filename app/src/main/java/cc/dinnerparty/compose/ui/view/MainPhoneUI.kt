package cc.dinnerparty.compose.ui.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cc.dinnerparty.compose.api.model.Place
import cc.dinnerparty.compose.ui.view.content.AboutContent
import cc.dinnerparty.compose.ui.view.content.ClusterMembersContent
import cc.dinnerparty.compose.ui.view.content.AccountContent
import cc.dinnerparty.compose.ui.view.content.AccountOps
import cc.dinnerparty.compose.ui.view.content.LoginContent
import cc.dinnerparty.compose.ui.view.content.phone.MapBottomBar
import cc.dinnerparty.compose.ui.view.component.MapFloatingActions
import cc.dinnerparty.compose.ui.view.content.MapMenuList
import cc.dinnerparty.compose.ui.view.component.MapSearchBar
import cc.dinnerparty.compose.ui.view.component.MapSearchBinding
import cc.dinnerparty.compose.ui.view.content.PanelMaxHeight
import cc.dinnerparty.compose.ui.view.content.phone.PhoneSheet
import cc.dinnerparty.compose.ui.view.content.phone.PlaceCardDialog
import cc.dinnerparty.compose.ui.view.content.PlaceCardOps
import cc.dinnerparty.compose.ui.view.content.PlaceFormContent
import cc.dinnerparty.compose.ui.view.content.PlaceFormUi
import cc.dinnerparty.compose.ui.view.content.SearchPanel
import cc.dinnerparty.compose.ui.viewmodel.AuthViewModel
import com.moriafly.salt.ui.UnstableSaltUiApi

/** 手机端浮层总高。 */
internal val PhoneOverlayHeight = 148.dp

/** 手机端地图浮层，底部依次是悬浮钮、搜索条、底栏，地点卡片从底部滑入。 */
@UnstableSaltUiApi
@Composable
fun MainPhoneUI(
    modifier: Modifier = Modifier,
    overlay: MapOverlayManager = rememberMapOverlayManager(),
    authViewModel: AuthViewModel,
    search: MapSearchBinding,
    locating: Boolean = false,
    onLocateClick: () -> Unit = {},
    place: Place? = null,
    cardOps: PlaceCardOps = PlaceCardOps(),
    clusterPlaces: List<Place>? = null,
    onSelectCluster: (Place) -> Unit = {},
    onDismissCluster: () -> Unit = {},
    form: PlaceFormUi = PlaceFormUi(),
    addMode: Boolean = false,
    onAddClick: () -> Unit = {},
    onDismissPlaceCard: () -> Unit = {},
    account: AccountOps = AccountOps(),
    onSettingsClick: () -> Unit = {},
) {
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()

    // 登录成功或退出登录后收起对应面板
    LaunchedEffect(authState.session, overlay.current) {
        when {
            overlay.current == MapOverlay.Login && authState.session != null -> overlay.hide()
            overlay.current == MapOverlay.Account && authState.session == null -> overlay.hide()
        }
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
                onLocateClick = onLocateClick,
                addMode = addMode,
                onAddClick = onAddClick,
            )
            if (search.state.resultsVisible) {
                SearchPanel(
                    state = search.state,
                    actions = search.actions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = PanelMaxHeight)
                        .padding(horizontal = 12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            MapSearchBar(
                state = search.state,
                actions = search.actions,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
            MapBottomBar(
                modifier = Modifier.fillMaxWidth(),
                user = authState.session?.user,
                avatarVersion = authState.session?.avatarVersion ?: 0L,
                onMoreClick = { overlay.show(MapOverlay.Menu) },
                onAddClick = onAddClick,
                adding = addMode,
                onAccountClick = {
                    if (authState.session == null) {
                        overlay.show(MapOverlay.Login)
                    }
                    else {
                        overlay.show(MapOverlay.Account)
                    }
                }
            )
        }

        PlaceCardDialog(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            place = place.takeIf { overlay.current == MapOverlay.Place },
            onDismissRequest = onDismissPlaceCard,
            ops = cardOps
        )

        PhoneSheet(
            value = clusterPlaces.takeIf { overlay.current == MapOverlay.ClusterMembers },
            onDismissRequest = onDismissCluster,
            modal = true,
        ) { places ->
            ClusterMembersContent(places, onSelectCluster, onDismissCluster)
        }

        // 添加或修改地点的表单
        PhoneSheet(
            visible = overlay.current == MapOverlay.AddPlace,
            onDismissRequest = form.events.onClose,
            modal = true
        ) {
            PlaceFormContent(state = form.state, events = form.events)
        }

        // 登录对话框
        PhoneSheet(
            visible = overlay.current == MapOverlay.Login,
            onDismissRequest = overlay::hide,
            modal = true
        ) {
            LoginContent(
                state = authState,
                onClose = overlay::hide,
                onLogin = authViewModel::login,
                onRegister = authViewModel::register,
                onResetPassword = authViewModel::resetPassword,
                onRequestRegisterCode = authViewModel::requestRegisterCode,
                onRequestResetCode = authViewModel::requestResetCode,
                onClearError = authViewModel::clearError
            )
        }

        // 账户对话框
        PhoneSheet(
            visible = overlay.current == MapOverlay.Account,
            onDismissRequest = overlay::hide,
            modal = true
        ) {
            AccountContent(
                modifier = Modifier.padding(16.dp),
                ops = account,
                user = authState.session?.user,
                avatarVersion = authState.session?.avatarVersion ?: 0L
            )
        }

        // 菜单对话框
        PhoneSheet(
            visible = overlay.current == MapOverlay.Menu,
            onDismissRequest = overlay::hide,
            modal = true
        ) {
            MapMenuList(
                modifier = Modifier.padding(vertical = 8.dp),
                onClose = overlay::hide,
                onAboutClick = { overlay.show(MapOverlay.About) }
            )
        }

        // 关于对话框
        PhoneSheet(
            visible = overlay.current == MapOverlay.About,
            onDismissRequest = overlay::hide,
            modal = true
        ) {
            AboutContent(onClose = overlay::hide)
        }
    }
}


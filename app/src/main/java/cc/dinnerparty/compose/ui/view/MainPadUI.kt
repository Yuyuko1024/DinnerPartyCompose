package cc.dinnerparty.compose.ui.view

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.dinnerparty.compose.R
import cc.dinnerparty.compose.api.model.Place
import cc.dinnerparty.compose.ui.adaptive.WindowSizeClass
import cc.dinnerparty.compose.ui.adaptive.currentWindowSizeClass
import cc.dinnerparty.compose.ui.view.component.AboutContent
import cc.dinnerparty.compose.ui.view.component.CircleIconButton
import cc.dinnerparty.compose.ui.view.component.MapFloatingActions
import cc.dinnerparty.compose.ui.view.component.MapMenuPanel
import cc.dinnerparty.compose.ui.view.component.MapSearchBar
import cc.dinnerparty.compose.ui.view.component.PadPanel
import cc.dinnerparty.compose.ui.view.component.PlaceDetailPanel
import com.moriafly.salt.ui.Button
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.Text
import com.moriafly.salt.ui.UnstableSaltUiApi

/** Header 内容高度，不含状态栏与底部描边。 */
private val HeaderContentHeight = 56.dp

/** Header 底部描边高度。 */
private val HeaderStrokeHeight = 1.dp

/** Header 占位总高，不含状态栏。 */
private val HeaderPlaceholderHeight = HeaderContentHeight + HeaderStrokeHeight

/** 浮层与屏幕左右边缘的距离。 */
private val EdgePadding = 12.dp

/** 搜索框两侧为按钮预留的宽度。 */
private val SearchBarSideSpace = 100.dp

/** 搜索框最大宽度。 */
private val SearchBarMaxWidth = 400.dp

/** 悬浮钮与 Header 的间距。 */
private val FloatingActionTopPadding = 12.dp

/** 悬浮钮横移时长。 */
private const val FloatingActionDurationMs = 300

/** Pad 端地图浮层，顶部 Header，下方右侧依次是详情面板、菜单面板与悬浮钮。 */
@UnstableSaltUiApi
@Composable
fun MainPadUI(
    modifier: Modifier = Modifier,
    locating: Boolean = false,
    onLocateClick: () -> Unit = {},
    place: Place? = null,
    onDismissPlaceCard: () -> Unit = {},
    mapTapCount: Int = 0
) {
    // 右侧当前显示的面板
    var rightPanel by remember { mutableStateOf<RightPanel?>(null) }

    // 选中地点时切到详情面板，点击地图时收起
    LaunchedEffect(mapTapCount, place) {
        rightPanel = if (place != null) RightPanel.Place else null
    }

    // 右侧被面板占住时悬浮钮横移到左侧
    val rightSideOccupied = rightPanel != null
    val horizontalBias by animateFloatAsState(
        targetValue = if (rightSideOccupied) -1f else 1f,
        animationSpec = tween(FloatingActionDurationMs),
        label = "floatingActionsBias"
    )

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(top = HeaderPlaceholderHeight)
                .padding(horizontal = EdgePadding)
        ) {
            PlaceDetailPanel(
                modifier = Modifier.align(Alignment.TopEnd)
                    .padding(top = FloatingActionTopPadding),
                place = place.takeIf { rightPanel == RightPanel.Place },
                onDismissRequest = onDismissPlaceCard
            )
            MapMenuPanel(
                modifier = Modifier.align(Alignment.TopEnd)
                    .padding(top = FloatingActionTopPadding),
                expanded = rightPanel == RightPanel.Menu,
                onAboutClick = { rightPanel = RightPanel.About },
                onDismissRequest = { rightPanel = null }
            )
            PadPanel(
                visible = rightPanel == RightPanel.About,
                modifier = Modifier.align(Alignment.TopEnd)
                    .padding(top = FloatingActionTopPadding)
            ) {
                AboutContent(onClose = { rightPanel = null })
            }
            MapFloatingActions(
                modifier = Modifier
                    .align(BiasAlignment(horizontalBias, -1f))
                    .padding(top = FloatingActionTopPadding),
                locating = locating,
                onLocateClick = onLocateClick
            )
        }

        PadHeader(
            modifier = Modifier.align(Alignment.TopCenter),
            onAccountClick = {},
            onMenuClick = {
                rightPanel = if (rightPanel == RightPanel.Menu) null else RightPanel.Menu
            }
        )
    }
}

/** 右侧面板，同一时刻只显示一个。 */
private enum class RightPanel { Place, Menu, About }

/** 顶部 Header，左侧登录、中间搜索框、右侧菜单。 */
@UnstableSaltUiApi
@Composable
private fun PadHeader(
    modifier: Modifier = Modifier,
    onAccountClick: () -> Unit = {},
    onMenuClick: () -> Unit = {}
) {
    // 窄窗口下隐藏品牌名，给搜索框让位
    val showLabel = currentWindowSizeClass() != WindowSizeClass.Medium

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SaltTheme.colors.subBackground)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .height(HeaderContentHeight),
            contentAlignment = Alignment.Center
        ) {
            MapSearchBar(
                modifier = Modifier
                    .padding(horizontal = SearchBarSideSpace)
                    .widthIn(max = SearchBarMaxWidth)
            )
            Row(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = EdgePadding)
            ) {
                Button(
                    onClick = onAccountClick,
                    text = stringResource(R.string.action_login),
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
                if (showLabel) {
                    Text(
                        text = stringResource(R.string.app_name),
                        fontSize = 20.sp,
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .padding(horizontal = 16.dp)
                    )
                }
            }
            CircleIconButton(
                imageVector = Icons.Outlined.Menu,
                contentDescription = stringResource(R.string.action_menu),
                onClick = onMenuClick,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = EdgePadding)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(HeaderStrokeHeight)
                .background(SaltTheme.colors.stroke)
        )
    }
}


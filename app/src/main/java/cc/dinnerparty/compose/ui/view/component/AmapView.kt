package cc.dinnerparty.compose.ui.view.component

import android.content.Context
import android.graphics.Point
import android.os.Bundle
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import cc.dinnerparty.compose.ui.theme.dinnerPartySemanticColors
import com.amap.api.maps.AMap
import com.amap.api.maps.MapsInitializer
import com.amap.api.maps.TextureMapView
import com.amap.api.maps.model.CameraPosition
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.Marker
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.location.AMapLocationClient
import com.moriafly.salt.ui.SaltTheme

/**
 * 需要锚定在地图坐标上的浮层。
 *
 * 它不是一个地图对象，而是和地图同层叠放的普通 Compose 内容，只是位置跟着 [position] 走。
 * 地点卡片必须这么做：卡片里有按钮、长文本、可能还有滚动，只有当成普通 Compose 内容画
 * 才能有正常的点击与排版，塞进高德的自定义气泡只会到处别扭。
 *
 * @param markerId 对应的标记 id，用来把它加粗并压到最上层。
 * @param position 锚点，也就是标记尖底所在的那个经纬度。
 */
data class MapAnchor(
    val markerId: Long,
    val position: LatLng,
    val content: @Composable () -> Unit,
)

/**
 * 高德地图（3D 版 + TextureMapView）。
 *
 * 用高德的隐私合规接口自行做同意确认（`preferences.xml` 声明 `com.amap.api.v2.apikey`）。
 * 调用方**不必**再单独调 [agreeAmapPrivacy]。
 *
 * @param logoBottomMargin 「高德地图」Logo 需要抬升的高度，通常传底部常驻浮层的总高。
 * @param markers 要画在地图上的地点标记，列表变化时全量重绘。
 * @param anchor 锚定在地图坐标正上方的浮层，用来站点信息卡片。
 * @param userLocation 用户当前位置，非空时在上面画一个「我的位置」小蓝点，传 null 就不画。
 * @param onMarkerClick 标记点击回调。
 * @param onMapClick 点击地图空白处，一般用来收起锚定浮层。
 * @param onMapReady 地图就绪回调，用于设置初始视野、注册视野监听等。
 */
@Composable
fun AmapView(
    modifier: Modifier = Modifier,
    logoBottomMargin: Dp = 0.dp,
    markers: List<MapMarker> = emptyList(),
    userLocation: LatLng? = null,
    anchor: MapAnchor? = null,
    onMarkerClick: (MapMarker) -> Unit = {},
    onMapClick: () -> Unit = {},
    onMapReady: (AMap) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = LocalDensity.current

    // 系统导航栏高度由地图自己让开，调用方只管自己的浮层高度。
    val logoMarginPx = with(density) { logoBottomMargin.roundToPx() } +
        WindowInsets.navigationBars.getBottom(density)

    // factory 里拿到的 AMap 存一份，标记重绘与浮层定位都要用
    var aMap by remember { mutableStateOf<AMap?>(null) }

    // AndroidView 的 factory 只跑一次，回调必须走 State 转发，否则永远停在首次组合时的那一份
    val currentOnMarkerClick by rememberUpdatedState(onMarkerClick)
    val currentOnMapClick by rememberUpdatedState(onMapClick)

    val mapView = remember(context) {
        TextureMapView(context).apply { onCreate(Bundle()) }
    }

    // 深色模式下换成高德内置的夜视图。
    //
    // 用内置的 MAP_TYPE_NIGHT，不走自定义样式文件：它零素材、不用去控制台配置，
    // 代价是夜视图没有 3D 建筑物（官方文档明说）——对「看哪儿有饭吃」这个用途无所谓。
    // 主题一律跟随系统，所以这里直接读系统亮暗，和 DinnerPartyTheme 用的是同一个来源。
    val isDarkTheme = isSystemInDarkTheme()
    LaunchedEffect(aMap, isDarkTheme) {
        aMap?.mapType = if (isDarkTheme) AMap.MAP_TYPE_NIGHT else AMap.MAP_TYPE_NORMAL
    }

    DisposableEffect(mapView, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    val labelColors = MarkerLabelColors(
        border = SaltTheme.colors.highlight.toArgb(),
        text = SaltTheme.colors.text.toArgb(),
        background = SaltTheme.colors.subBackground.toArgb(),
        avoidedBorder = dinnerPartySemanticColors().danger.toArgb(),
        avoidedText = dinnerPartySemanticColors().danger.toArgb(),
        avoidedBackground = SaltTheme.colors.popup.toArgb(),
    )
    val renderer = remember(context, density, labelColors) {
        PlaceMarkerRenderer(context, density.density, labelColors)
    }
    // 蓝点在首次定位之前不用画，但先算出来也就一张 44dp 的位图，不值得为它做延迟初始化
    val userLocationGraphic = remember(context, density) {
        userLocationIcon(context, density.density)
    }

    // 已经挂在地图上的标记，用来做增量更新。地图换了就一定要丢掉重来，
    // 否则会拿着已经随 onDestroy 一起失效的标记去操作新地图。
    val rendered = remember(aMap) { HashMap<Long, RenderedMarker>() }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                mapView.apply {
                    getMap().apply {
                        uiSettings.run {
                            // 缩放按钮 / 指南针 / 定位按钮一律用自绘控件，关掉高德自带的
                            isZoomControlsEnabled = false
                            isCompassEnabled = false
                            isMyLocationButtonEnabled = false
                            setLogoBottomMargin(logoMarginPx)
                        }
                        // 标记点击只用来选地点，弹窗由 Compose 画，所以吃掉高德默认的气泡
                        setOnMarkerClickListener { marker ->
                            (marker?.getObject() as? MapMarker)?.let { currentOnMarkerClick(it) }
                            true
                        }
                        setOnMapClickListener { currentOnMapClick() }
                        aMap = this
                        onMapReady(this)
                    }
                }
            },
            // 地图对象常驻，重绘时不做事，否则会把地图重新挂载导致闪白
            update = {}
        )

        // 标记增量更新。
        //
        // 不能用「清空重加」：缩放和拖动时可见节点会成批地变，全删全加就得在每个变化帧里
        // 重建上百个高德标记，而每个标记都是一次 native 调用，拖动当场变幻灯片。
        // 所以只动真正新来、消失或换了外观（选中状态变了、聚合数量变了）的那些。
        //
        // 首帧数据还没回来时列表是空的，真正的标记一定在响应回来之后才加，那时地图早就加载完了，
        // 所以不需要额外等 onMapLoaded。
        LaunchedEffect(aMap, markers, anchor?.markerId, renderer) {
            val map = aMap ?: return@LaunchedEffect
            val selectedId = anchor?.markerId
            val alive = HashSet<Long>(markers.size)

            for (marker in markers) {
                alive += marker.id
                val selected = marker.id == selectedId
                val shown = rendered[marker.id]
                // 外观没变就一个手指头都不碰它
                if (shown != null && shown.marker == marker && shown.selected == selected) continue
                shown?.overlay?.remove()

                val graphic = renderer.graphic(marker, selected)
                val overlay = map.addMarker(
                    MarkerOptions()
                        .position(marker.position)
                        .icon(graphic.descriptor)
                        // 位图里图形在上、名称胶囊在下，所以锚点不在图片底边而在图形底边
                        .anchor(0.5f, graphic.anchorV)
                        // 选中的那个压在最上层，免得被旁边的名称胶囊盖住
                        .zIndex(if (selected) 1f else 0f)
                )
                if (overlay == null) {
                    rendered.remove(marker.id)
                    continue
                }
                // 名称胶囊我们自己画在图形下面，别再弹高德的气泡
                overlay.setInfoWindowEnable(false)
                overlay.setObject(marker)
                rendered[marker.id] = RenderedMarker(marker, selected, overlay)
            }

            val stale = rendered.entries.iterator()
            while (stale.hasNext()) {
                val entry = stale.next()
                if (entry.key !in alive) {
                    entry.value.overlay.remove()
                    stale.remove()
                }
            }
        }

        // 「我的位置」蓝点。和 POI 标记分开管：它不可点、不参与聚合、永远压在最上面。
        // 这里不用 mutableState 存引用——它在组合期没人读，存了反而白引一次重组。
        val userLocationHolder = remember(aMap) { UserLocationHolder() }

        LaunchedEffect(aMap, userLocation) {
            val map = aMap ?: return@LaunchedEffect
            val shown = userLocationHolder.marker
            if (userLocation == null) {
                shown?.remove()
                userLocationHolder.marker = null
            } else if (shown == null) {
                val overlay = map.addMarker(
                    MarkerOptions()
                        .position(userLocation)
                        .icon(userLocationGraphic.descriptor)
                        .anchor(0.5f, userLocationGraphic.anchorV)
                        .zIndex(UserLocationZIndex)
                ) ?: return@LaunchedEffect
                // 蓝点只是个指示，不做信息气泡
                overlay.setInfoWindowEnable(false)
                userLocationHolder.marker = overlay
            } else {
                // 再次定位：蓝点已经在图上了，挪过去就是，不用重建
                shown.position = userLocation
            }
        }

        val map = aMap
        if (map != null) {
            anchor?.let { AnchoredOverlay(aMap = map, anchor = it) }
        }
    }
}

/**
 * 把 [MapAnchor] 的内容摆到它坐标的正上方。
 *
 * 位置只在**布局阶段**读 [screenPoint]，所以地图拖动时只触发重新布局、不触发重组——
 * 卡片里文字不少，逐帧重组会掉帧。
 *
 * 跟随用的是相机事件的**开始**而不是结束（`onCameraChange`）：结束回调在手指抬起、
 * 地图停稳之后才来，那一下卡片会明显跟不上；开始回调每帧都有，和高德自己的标记同步。
 */
@Composable
private fun AnchoredOverlay(aMap: AMap, anchor: MapAnchor) {
    val screenPoint = remember(aMap) { mutableStateOf<Point?>(null) }
    val gapPx = with(LocalDensity.current) { CardAnchorGap.roundToPx() }

    DisposableEffect(aMap, anchor.position) {
        val position = anchor.position
        val project = { screenPoint.value = aMap.projection?.toScreenLocation(position) }
        val listener = object : AMap.OnCameraChangeListener {
            override fun onCameraChange(cameraPosition: CameraPosition?) = project()
            override fun onCameraChangeFinish(cameraPosition: CameraPosition?) = project()
        }
        aMap.addOnCameraChangeListener(listener)
        // 先算一次，否则得等下一次相机事件才出现，看着像卡了一下
        project()
        onDispose { aMap.removeOnCameraChangeListener(listener) }
    }

    Layout(content = anchor.content, modifier = Modifier.fillMaxSize()) { measurables, constraints ->
        val placeable = measurables.first().measure(constraints.copy(minWidth = 0, minHeight = 0))
        val point = screenPoint.value
        if (point == null) {
            // 还没投影出屏幕坐标，这一帧先不画
            layout(0, 0) {}
        } else {
            layout(constraints.maxWidth, constraints.maxHeight) {
                // 卡片下沿和标记尖底之间留出标记图形的高度，别压住图标
                val maxX = (constraints.maxWidth - placeable.width).coerceAtLeast(0)
                val maxY = (constraints.maxHeight - placeable.height).coerceAtLeast(0)
                placeable.place(
                    x = (point.x - placeable.width / 2).coerceIn(0, maxX),
                    y = (point.y - placeable.height - gapPx).coerceIn(0, maxY)
                )
            }
        }
    }
}

/**
 * 高德隐私合规声明：声明「隐私政策已包含高德条款且已弹窗告知用户」并置为已同意。
 *
 * 必须在创建任何地图对象之前调用，否则地图一片空白——表现和 Key 失效完全一样，
 * 只能从日志里的 `INVALID_USER_SCODE` 区分。重复调用无副作用。
 *
 * 同意与否是应用层的决策（见 `MainActivity` 的隐私弹框），所以不放在本文件的地图创建流程里。
 */
fun agreeAmapPrivacy(context: Context) {
    MapsInitializer.updatePrivacyShow(context, true, true)
    MapsInitializer.updatePrivacyAgree(context, true)
    // 定位 SDK 与地图 SDK 各记一份隐私状态，只声明地图那份的话定位会直接失败
    AMapLocationClient.updatePrivacyShow(context, true, true)
    AMapLocationClient.updatePrivacyAgree(context, true)
}

/**
 * 卡片下沿与标记尖底之间留的距离。
 *
 * 取自 Web `MapAnchoredCard.jsx` 的 `offsetY = 53`：36×43 的图形 + 3px 间隙 + 22px 的名称胶囊
 * 一共 68，Web 那边按卡片自身内边距又收了一点，取 53 两边观感一致。
 */
private val CardAnchorGap = 53.dp

/** 已经挂在地图上的一个标记，用来做增量更新。 */
private class RenderedMarker(
    val marker: MapMarker,
    val selected: Boolean,
    val overlay: Marker,
)

/**
 * 「我的位置」蓝点的持有者。
 *
 * 它和 POI 标记走的是两条路（不可点、不聚合、不参与增量 diff），所以单独存一个引用。
 */
private class UserLocationHolder {
    var marker: Marker? = null
}

/** 蓝点的层级。比选中态的 POI 标记（1f）还高，免得被附近的名称胶囊压住。 */
private const val UserLocationZIndex = 2f

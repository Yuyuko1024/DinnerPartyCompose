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
 * 锚定在地图坐标上的浮层，位置跟着 [position] 走。
 *
 * @param markerId 对应的标记 id。
 * @param position 锚点经纬度。
 */
data class MapAnchor(
    val markerId: Long,
    val position: LatLng,
    val content: @Composable () -> Unit,
)

/**
 * 高德地图（3D 版 + TextureMapView），内部完成隐私合规声明。
 *
 * @param logoBottomMargin 「高德地图」Logo 需抬升的高度。
 * @param markers 地图上的地点标记，列表变化时全量重绘。
 * @param anchor 锚定在地图坐标正上方的浮层。
 * @param userLocation 用户当前位置，非空时画「我的位置」蓝点。
 * @param onMarkerClick 标记点击回调。
 * @param onMapClick 地图空白处点击回调。
 * @param onMapReady 地图就绪回调。
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

    // Logo 抬升高度 = 调用方给的高度 + 导航栏高度。
    val logoMarginPx = with(density) { logoBottomMargin.roundToPx() } +
        WindowInsets.navigationBars.getBottom(density)

    // 存一份 factory 里拿到的 AMap
    var aMap by remember { mutableStateOf<AMap?>(null) }

    // 回调经 rememberUpdatedState 转发
    val currentOnMarkerClick by rememberUpdatedState(onMarkerClick)
    val currentOnMapClick by rememberUpdatedState(onMapClick)

    val mapView = remember(context) {
        TextureMapView(context).apply { onCreate(Bundle()) }
    }

    // 深色模式用高德内置夜视图
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
    // 蓝点图预先算好，只有一张 44dp 位图
    val userLocationGraphic = remember(context, density) {
        userLocationIcon(context, density.density)
    }

    // 已经挂在地图上的标记，用来做增量更新；地图换了要丢掉重来
    val rendered = remember(aMap) { HashMap<Long, RenderedMarker>() }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                mapView.apply {
                    getMap().apply {
                        uiSettings.run {
                            // 关掉高德自带的缩放按钮、指南针与定位按钮
                            isZoomControlsEnabled = false
                            isCompassEnabled = false
                            isMyLocationButtonEnabled = false
                            setLogoBottomMargin(logoMarginPx)
                        }
                        // 标记点击交给回调，并吃掉高德默认气泡
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
            // 地图对象常驻，重绘时不做事
            update = {}
        )

        // 标记增量更新
        LaunchedEffect(aMap, markers, anchor?.markerId, renderer) {
            val map = aMap ?: return@LaunchedEffect
            val selectedId = anchor?.markerId
            val alive = HashSet<Long>(markers.size)

            for (marker in markers) {
                alive += marker.id
                val selected = marker.id == selectedId
                val shown = rendered[marker.id]
                // 外观没变则跳过
                if (shown != null && shown.marker == marker && shown.selected == selected) continue
                shown?.overlay?.remove()

                val graphic = renderer.graphic(marker, selected)
                val overlay = map.addMarker(
                    MarkerOptions()
                        .position(marker.position)
                        .icon(graphic.descriptor)
                        // 锚点落在图形底边
                        .anchor(0.5f, graphic.anchorV)
                        // 选中的压在最上层
                        .zIndex(if (selected) 1f else 0f)
                )
                if (overlay == null) {
                    rendered.remove(marker.id)
                    continue
                }
                // 关掉高德信息气泡
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

        // 「我的位置」蓝点，与 POI 标记分开管理
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
                // 蓝点关闭信息气泡
                overlay.setInfoWindowEnable(false)
                userLocationHolder.marker = overlay
            } else {
                // 再次定位时移动蓝点
                shown.position = userLocation
            }
        }

        val map = aMap
        if (map != null) {
            anchor?.let { AnchoredOverlay(aMap = map, anchor = it) }
        }
    }
}

/** 把 [MapAnchor] 的内容摆到它坐标的正上方，相机一变就重新投影。 */
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

/** 高德隐私合规声明。 */
fun agreeAmapPrivacy(context: Context) {
    MapsInitializer.updatePrivacyShow(context, true, true)
    MapsInitializer.updatePrivacyAgree(context, true)
    // 定位 SDK 与地图 SDK 各声明一次
    AMapLocationClient.updatePrivacyShow(context, true, true)
    AMapLocationClient.updatePrivacyAgree(context, true)
}

/** 卡片下沿与标记尖底之间的距离。 */
private val CardAnchorGap = 53.dp

/** 已经挂在地图上的一个标记。 */
private class RenderedMarker(
    val marker: MapMarker,
    val selected: Boolean,
    val overlay: Marker,
)

/** 「我的位置」蓝点的持有者。 */
private class UserLocationHolder {
    var marker: Marker? = null
}

/** 蓝点的层级，压在所有 POI 标记之上。 */
private const val UserLocationZIndex = 2f

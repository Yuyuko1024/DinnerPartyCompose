package cc.dinnerparty.compose.ui.view

import android.Manifest
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cc.dinnerparty.compose.R
import cc.dinnerparty.compose.data.model.MapBounds
import cc.dinnerparty.compose.ui.view.component.AmapView
import cc.dinnerparty.compose.ui.view.component.MapBottomBar
import cc.dinnerparty.compose.ui.view.component.MapFloatingActions
import cc.dinnerparty.compose.ui.view.component.MapSearchBar
import cc.dinnerparty.compose.ui.view.component.PlaceCardDialog
import cc.dinnerparty.compose.ui.view.component.toMapMarker
import cc.dinnerparty.compose.ui.viewmodel.MapViewModel
import cc.dinnerparty.compose.utils.LocationClient
import cc.dinnerparty.compose.utils.LocationFailure
import cc.dinnerparty.compose.utils.LocationResult
import cc.dinnerparty.compose.utils.PermissionResult
import cc.dinnerparty.compose.utils.isGranted
import cc.dinnerparty.compose.utils.openAppSettings
import cc.dinnerparty.compose.utils.rememberPermissionLauncher
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.model.CameraPosition
import com.amap.api.maps.model.LatLng
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.UnstableSaltUiApi
import com.moriafly.salt.ui.dialog.YesNoDialog
import kotlin.math.max

/**
 * 地图主页。
 */
@UnstableSaltUiApi
@Composable
fun MainView(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val viewModel: MapViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showLocationPermissionGuide by remember { mutableStateOf(false) }

    // 地图对象
    var amap by remember { mutableStateOf<AMap?>(null) }

    // 当前设备定位到的位置。
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var locating by remember { mutableStateOf(false) }

    val locationClient = remember(context) { LocationClient(context) }
    DisposableEffect(locationClient) {
        onDispose { locationClient.destroy() }
    }

    // 定位成功之后自动移动视角
    val showUserLocation: (LatLng) -> Unit = { position ->
        userLocation = position
        amap?.let { map ->
            val zoom = max(map.cameraPosition?.zoom ?: LocateZoom, LocateZoom)
            map.animateCamera(
                CameraUpdateFactory.newLatLngZoom(position, zoom),
                LocateDurationMs,
                null,
            )
        }
    }

    val locateUser: (Boolean) -> Unit = { notifyFailure ->
        locating = true
        locationClient.requestOnce { result ->
            locating = false
            when (result) {
                is LocationResult.Success -> showUserLocation(result.position)

                is LocationResult.Failure -> if (notifyFailure) {
                    Toast.makeText(
                        context,
                        when (result.reason) {
                            LocationFailure.PERMISSION -> R.string.location_error_permission
                            LocationFailure.SERVICES_UNAVAILABLE -> R.string.location_error_services
                            LocationFailure.UNKNOWN -> R.string.location_error_failed
                        },
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
        }
    }

    // 冷启动自动定一次位
    LaunchedEffect(amap) {
        if (amap == null) return@LaunchedEffect
        if (LocationPermissions.none { it.isGranted(context) }) return@LaunchedEffect
        locateUser(false)
    }

    val requestLocationPermission = rememberPermissionLauncher(
        *LocationPermissions,
    ) { result ->
        when (result) {
            // 已授权
            PermissionResult.GRANTED -> locateUser(true)
            // 权限被拒
            PermissionResult.DENIED -> Unit
            // 引导去设置页
            PermissionResult.DENIED_PERMANENTLY -> showLocationPermissionGuide = true
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SaltTheme.colors.background)
    ) {
        val nodes = uiState.visibleNodes
        val markers = remember(nodes) { nodes.mapNotNull { it.toMapMarker() } }
        val selectedPlace = uiState.selectedPlace

        val density = LocalDensity.current.density
        val publishViewport: (AMap) -> Unit = { map ->
            val bounds = map.visibleBounds()
            val zoom = map.cameraPosition?.zoom
            if (bounds != null && zoom != null) {
                viewModel.updateViewport(
                    bounds = bounds,
                    zoom = zoom,
                    mapMaxZoom = map.maxZoomLevel.toInt(),
                    clusterRadiusPx = ClusterRadius * density,
                )
            }
        }

        AmapView(
            modifier = Modifier.fillMaxSize(),
            logoBottomMargin = BottomOverlayHeight,
            markers = markers,
            userLocation = userLocation,
            selectedMarkerId = selectedPlace?.id,
            onMarkerClick = { marker ->
                if (marker.clusterCount == null) {
                    viewModel.selectPlace(marker.id)
                    // 把点中的地点移到地图中心
                    amap?.animateCamera(
                        CameraUpdateFactory.newLatLng(marker.position),
                        MarkerCenterDurationMs,
                        null,
                    )
                } else {
                    // 放大到聚合散开的级别
                    amap?.let { map ->
                        viewModel.expansionZoomFor(marker.id)?.let { expansion ->
                            map.animateCamera(
                                CameraUpdateFactory.newLatLngZoom(marker.position, expansion.toFloat()),
                                ClusterExpandDurationMs,
                                null,
                            )
                            // 聚合成气泡后清掉卡片
                            viewModel.clearSelection()
                        }
                    }
                }
            },
            onMapClick = viewModel::clearSelection,
            onMapReady = { map ->
                amap = map
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(DefaultCenter, DefaultZoom))

                // 地图加载完再取一次视野
                map.setOnMapLoadedListener { publishViewport(map) }

                // 相机停稳后重算视野
                map.setOnCameraChangeListener(object : AMap.OnCameraChangeListener {
                    override fun onCameraChange(position: CameraPosition?) = Unit

                    override fun onCameraChangeFinish(position: CameraPosition?) {
                        publishViewport(map)
                    }
                })
            }
        )

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
                onLocateClick = { if (!locating) requestLocationPermission() }
            )
            MapSearchBar(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
            MapBottomBar(
                modifier = Modifier.fillMaxWidth()
            )
        }

        PlaceCardDialog(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            place = uiState.selectedPlace,
            onDismissRequest = viewModel::clearSelection,
        )
    }

    if (showLocationPermissionGuide) {
        YesNoDialog(
            onDismissRequest = { showLocationPermissionGuide = false },
            onConfirm = {
                showLocationPermissionGuide = false
                context.openAppSettings()
            },
            title = stringResource(R.string.location_permission_title),
            content = stringResource(R.string.location_permission_guide),
            cancelText = stringResource(R.string.action_cancel),
            confirmText = stringResource(R.string.action_go_to_settings)
        )
    }
}

/** 底部常驻浮层总高。 */
private val BottomOverlayHeight = 136.dp

/** 定位权限。 */
private val LocationPermissions = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)

/** 默认中心点：广州 */
private val DefaultCenter = LatLng(23.016485, 113.394405)

/** 聚合半径，单位 dp。 */
private const val ClusterRadius = 70.0

/** 点开聚合气泡时的相机动画时长。 */
private const val ClusterExpandDurationMs = 300L

/** 初始缩放级别。 */
private const val DefaultZoom = 15f

/** 定位成功后至少放到这一级。 */
private const val LocateZoom = 18f

/** 相机飞到定位点的时长。 */
private const val LocateDurationMs = 400L

/** 点开地点时相机移到地图中心的时长。 */
private const val MarkerCenterDurationMs = 300L

/** 地图当前可见的经纬度范围。 */
private fun AMap.visibleBounds(): MapBounds? {
    val bounds = projection?.visibleRegion?.latLngBounds ?: return null
    return MapBounds(
        minLng = bounds.southwest.longitude,
        minLat = bounds.southwest.latitude,
        maxLng = bounds.northeast.longitude,
        maxLat = bounds.northeast.latitude,
    )
}
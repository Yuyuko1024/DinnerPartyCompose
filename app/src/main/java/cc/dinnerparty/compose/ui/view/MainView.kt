package cc.dinnerparty.compose.ui.view

import android.Manifest
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cc.dinnerparty.compose.R
import cc.dinnerparty.compose.api.model.Place
import cc.dinnerparty.compose.data.model.MapBounds
import cc.dinnerparty.compose.ui.adaptive.LocalIsTablet
import cc.dinnerparty.compose.ui.adaptive.WindowSizeClass
import cc.dinnerparty.compose.ui.adaptive.currentWindowSizeClass
import cc.dinnerparty.compose.ui.view.component.AmapView
import cc.dinnerparty.compose.ui.view.component.AvatarUpdateFlow
import cc.dinnerparty.compose.ui.view.component.EditPasswordDialog
import cc.dinnerparty.compose.ui.view.component.EditUsernameDialog
import cc.dinnerparty.compose.ui.view.component.SearchPoi
import cc.dinnerparty.compose.ui.view.component.rememberDismiss
import cc.dinnerparty.compose.ui.view.component.rememberSearch
import cc.dinnerparty.compose.ui.view.component.toMapMarker
import cc.dinnerparty.compose.ui.viewmodel.AuthViewModel
import cc.dinnerparty.compose.ui.viewmodel.MapViewModel
import cc.dinnerparty.compose.ui.viewmodel.SearchViewModel
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
import kotlin.coroutines.resume
import kotlin.math.max
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * 地图主页。
 */
@UnstableSaltUiApi
@Composable
fun MainView(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val viewModel: MapViewModel = viewModel()
    val authViewModel: AuthViewModel = viewModel()
    val searchViewModel: SearchViewModel = viewModel()
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchBinding = rememberSearch(searchViewModel)
    val dismiss = rememberDismiss()
    var showLocationPermissionGuide by remember { mutableStateOf(false) }

    // 账号编辑弹框开关
    var editUsername by remember { mutableStateOf(false) }
    var editPassword by remember { mutableStateOf(false) }

    // 待二次确认的账号操作
    var pendingAccountAction by remember { mutableStateOf<AccountAction?>(null) }

    // 头像选择裁剪上传流程
    var avatarUpdating by remember { mutableStateOf(false) }

    // 地图对象
    var amap by remember { mutableStateOf<AMap?>(null) }

    // 高德检索命中点 动画结束后单独标注
    var searchPoi by remember { mutableStateOf<SearchPoi?>(null) }

    // 高德检索命中点对应的卡片数据 未收录于站内
    var amapPlace by remember { mutableStateOf<Place?>(null) }

    // 当前设备定位到的位置。
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var locating by remember { mutableStateOf(false) }

    // 当前显示的浮层
    val overlay = rememberMapOverlayManager()

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

    val windowSizeClass = currentWindowSizeClass()

    // 选中搜索结果后先放大到最大级别再显示地点卡片
    LaunchedEffect(searchBinding.state.focusedItem, amap) {
        val item = searchBinding.state.focusedItem ?: return@LaunchedEffect
        val map = amap ?: return@LaunchedEffect
        val target = LatLng(item.latitude, item.longitude)
        val placeId = item.placeId
        // 详情在相机动画期间并行加载
        placeId?.let(viewModel::openPlace)
        map.flyTo(target, map.maxZoomLevel, SearchDurationMs)
        if (placeId == null) {
            viewModel.clearSelection()
            // 高德结果等相机停稳后再标注并弹卡片
            searchPoi = SearchPoi(target, item.name)
            amapPlace = Place(
                name = item.name,
                address = item.address,
                latitude = item.latitude,
                longitude = item.longitude
            )
            overlay.show(MapOverlay.Place)
        } else {
            searchPoi = null
            amapPlace = null
            overlay.show(MapOverlay.Place)
        }
        searchViewModel.consumeFocus()
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
            map.cameraPosition?.target?.let(searchViewModel::updateCenter)
        }

        AmapView(
            modifier = Modifier.fillMaxSize(),
            // 手机端底栏常驻，地图 Logo 需要抬升
            logoBottomMargin = if (windowSizeClass == WindowSizeClass.Compact) PhoneOverlayHeight else 0.dp,
            markers = markers,
            userLocation = userLocation,
            searchPoi = searchPoi,
            selectedMarkerId = selectedPlace?.id,
            onMarkerClick = { marker ->
                searchPoi = null
                amapPlace = null
                if (marker.clusterCount == null) {
                    overlay.show(MapOverlay.Place)
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
                            overlay.hide()
                        }
                    }
                }
            },
            onMapClick = {
                dismiss()
                searchPoi = null
                amapPlace = null
                overlay.hide()
                viewModel.clearSelection()
            },
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

        CompositionLocalProvider(
            LocalIsTablet provides (windowSizeClass != WindowSizeClass.Compact)
        ) {
            when (windowSizeClass) {
                WindowSizeClass.Compact -> MainPhoneUI(
                    overlay = overlay,
                    authViewModel = authViewModel,
                    search = searchBinding,
                    locating = locating,
                    onLocateClick = { if (!locating) requestLocationPermission() },
                    place = amapPlace ?: selectedPlace,
                    external = amapPlace != null,
                    onDismissPlaceCard = {
                        overlay.hide()
                        amapPlace = null
                        searchPoi = null
                        viewModel.clearSelection()
                    },
                    onUpdateAvatar = { avatarUpdating = true },
                    onUpdateUsername = { editUsername = true },
                    onUpdatePassword = { editPassword = true },
                    onLogoutAll = { pendingAccountAction = AccountAction.LOGOUT_ALL },
                    onDeleteAccount = { pendingAccountAction = AccountAction.DELETE_ACCOUNT }
                )

                WindowSizeClass.Medium, WindowSizeClass.Expanded -> MainPadUI(
                    overlay = overlay,
                    authViewModel = authViewModel,
                    search = searchBinding,
                    locating = locating,
                    onLocateClick = { if (!locating) requestLocationPermission() },
                    place = amapPlace ?: selectedPlace,
                    external = amapPlace != null,
                    onDismissPlaceCard = {
                        overlay.hide()
                        amapPlace = null
                        searchPoi = null
                        viewModel.clearSelection()
                    },
                    onUpdateAvatar = { avatarUpdating = true },
                    onUpdateUsername = { editUsername = true },
                    onUpdatePassword = { editPassword = true },
                    onLogoutAll = { pendingAccountAction = AccountAction.LOGOUT_ALL },
                    onDeleteAccount = { pendingAccountAction = AccountAction.DELETE_ACCOUNT }
                )
            }
        }
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

    if (editUsername) {
        EditUsernameDialog(
            currentUsername = authState.session?.user?.username.orEmpty(),
            onDismiss = { editUsername = false },
            authViewModel = authViewModel,
            onMessage = { message ->
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            },
        )
    }

    if (editPassword) {
        EditPasswordDialog(
            onDismiss = { editPassword = false },
            authViewModel = authViewModel,
            onMessage = { message ->
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            },
        )
    }

    pendingAccountAction?.let { action ->
        val isLogout = action == AccountAction.LOGOUT_ALL
        YesNoDialog(
            onDismissRequest = { pendingAccountAction = null },
            onConfirm = {
                pendingAccountAction = null
                when (action) {
                    AccountAction.LOGOUT_ALL -> authViewModel.logoutAll()
                    AccountAction.DELETE_ACCOUNT -> authViewModel.deleteAccount()
                }
            },
            title = stringResource(
                if (isLogout) R.string.account_logout_title else R.string.account_delete_title
            ),
            content = stringResource(
                if (isLogout) R.string.account_logout_content else R.string.account_delete_content
            ),
            cancelText = stringResource(R.string.action_cancel),
            confirmText = stringResource(
                if (isLogout) R.string.account_logout_confirm else R.string.account_delete_confirm
            )
        )
    }

    AvatarUpdateFlow(
        visible = avatarUpdating,
        onDismiss = { avatarUpdating = false },
        authViewModel = authViewModel,
        onMessage = { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        },
    )
}

/** 需要二次确认的账号操作。 */
internal enum class AccountAction {
    LOGOUT_ALL,
    DELETE_ACCOUNT
}

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

/** 搜索结果定位的相机动画时长。 */
private const val SearchDurationMs = 300L

/** 相机飞到目标点 动画结束后返回 */
private suspend fun AMap.flyTo(target: LatLng, zoom: Float, duration: Long) {
    suspendCancellableCoroutine { cont ->
        animateCamera(
            CameraUpdateFactory.newLatLngZoom(target, zoom),
            duration,
            object : AMap.CancelableCallback {
                override fun onFinish() {
                    if (cont.isActive) cont.resume(Unit)
                }

                override fun onCancel() {
                    if (cont.isActive) cont.resume(Unit)
                }
            }
        )
        cont.invokeOnCancellation { stopAnimation() }
    }
}

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
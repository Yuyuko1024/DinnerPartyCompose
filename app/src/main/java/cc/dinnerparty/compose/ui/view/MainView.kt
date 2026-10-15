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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cc.dinnerparty.compose.R
import cc.dinnerparty.compose.data.model.MapBounds
import cc.dinnerparty.compose.ui.view.component.AmapView
import cc.dinnerparty.compose.ui.view.component.MapAnchor
import cc.dinnerparty.compose.ui.view.component.MapBottomBar
import cc.dinnerparty.compose.ui.view.component.MapFloatingActions
import cc.dinnerparty.compose.ui.view.component.MapSearchBar
import cc.dinnerparty.compose.ui.view.component.PlaceCard
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
 *
 * 按架构约定，地图是唯一常驻的根页面，其余路由都以透明浮层叠在其上。
 * 本文件负责地图之上的常驻界面外壳，以及把 [MapViewModel] 的状态搬运到地图上——
 * 数据怎么来由 ViewModel 决定，这里只做「状态 → 标记」的转换。
 *
 * 权限申请是「用得到才问」：系统权限弹窗只会在用户点定位按钮时出现。
 * 冷启动时如果权限**本来就有**，会安静地自动定一次位，不弹窗也不报错。
 */
@UnstableSaltUiApi
@Composable
fun MainView(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val viewModel: MapViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showLocationPermissionGuide by remember { mutableStateOf(false) }

    // 点聚合气泡、点定位都要动相机，而相机只有地图自己给得出来，所以这里留一份引用
    var amap by remember { mutableStateOf<AMap?>(null) }

    // 定位到的位置。Web 那边定一次就把蓝点留在图上，再点只是把它挪过去
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var locating by remember { mutableStateOf(false) }

    val locationClient = remember(context) { LocationClient(context) }
    DisposableEffect(locationClient) {
        onDispose { locationClient.destroy() }
    }

    // 定位成功之后：蓝点留在图上，相机挪过去。
    // 已经比「看得清街道」更近时不再拉远，只换中心——Web 取的也是 max(当前级别, 18)，
    // 不然每次点定位都会把用户刚放大的视角拉回去
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

    // `notifyFailure` 分两种来路：用户点按钮的失败了要说一声；
    // 冷启动自动跑的用户并没要求过什么，弹 Toast 只会让他莫名其妙（Web 那边也只打日志）
    val locateUser: (Boolean) -> Unit = { notifyFailure ->
        locating = true
        locationClient.requestOnce { result ->
            locating = false
            when (result) {
                is LocationResult.Success -> showUserLocation(result.position)

                // 失败原因分得粗是故意的：告诉用户该去开什么就够，具体错误码看日志
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

    // 冷启动自动定一次位。`amap` 只在就绪时被赋值一次，所以这个 effect 每个进程只跑一次。
    // 只在**已经**有权限时才动：用户刚进 App 正要看地图，不该先被系统权限弹窗拦一道，
    // 那次申请留给他自己点定位按钮时再问。
    LaunchedEffect(amap) {
        if (amap == null) return@LaunchedEffect
        if (LocationPermissions.none { it.isGranted(context) }) return@LaunchedEffect
        locateUser(false)
    }

    val requestLocationPermission = rememberPermissionLauncher(
        *LocationPermissions,
    ) { result ->
        when (result) {
            // 拿到权限就直接定，别让用户再点一下
            PermissionResult.GRANTED -> locateUser(true)
            // 还能再问，这次先不打扰，用户下次点定位再走一遍
            PermissionResult.DENIED -> Unit
            // 系统不会再弹窗了，只能引导去设置页
            PermissionResult.DENIED_PERMANENTLY -> showLocationPermissionGuide = true
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SaltTheme.colors.background)
    ) {
        // 该画哪些节点由 ViewModel 按缩放级别算好，这里只做「节点 → 标记」的转换
        val nodes = uiState.visibleNodes
        val markers = remember(nodes) { nodes.mapNotNull { it.toMapMarker() } }
        // 选中态只记 id：标记可能被聚合吞掉（缩小时），那时它的坐标就不存在了
        val selectedPlace = uiState.selectedPlace
        val selectedMarker = remember(markers, selectedPlace) {
            markers.firstOrNull { it.id == selectedPlace?.id }
        }

        // 相机每停一次就重算视野：既决定去拉哪一块数据，也决定该显示哪些节点。
        // 高德没有单独的 getZoomLevel，缩放级别在相机位置上。
        val publishViewport: (AMap) -> Unit = { map ->
            val bounds = map.visibleBounds()
            val zoom = map.cameraPosition?.zoom
            if (bounds != null && zoom != null) viewModel.updateViewport(bounds, zoom)
        }

        AmapView(
            modifier = Modifier.fillMaxSize(),
            logoBottomMargin = BottomOverlayHeight,
            markers = markers,
            userLocation = userLocation,
            anchor = if (selectedPlace != null && selectedMarker != null) {
                MapAnchor(
                    markerId = selectedMarker.id,
                    position = selectedMarker.position,
                    content = {
                        PlaceCard(
                            place = selectedPlace,
                            onClose = viewModel::clearSelection
                        )
                    }
                )
            } else {
                null
            },
            onMarkerClick = { marker ->
                if (marker.clusterCount == null) {
                    viewModel.selectPlace(marker.id)
                } else {
                    // 展开到「下一层」就够：再细一级的网格里这个气泡会被它的下一层取代。
                    // 不是一路拆到单点——那样几十个地点会一次点击直接冲到米级
                    amap?.let { map ->
                        viewModel.expansionZoomFor(marker.id)?.let { expansion ->
                            map.animateCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                    marker.position,
                                    // 放到最大也散不开时（坐标重合或贴得太近）就放到地图允许的最大级别，
                                    // 至少让用户看到底下到底是什么情况，而不是点了没反应
                                    expansion.coerceAtMost(map.maxZoomLevel.toInt()).toFloat(),
                                ),
                                ClusterExpandDurationMs,
                                null,
                            )
                            // 卡片锚在标记上，标记被聚合吞了它就没地方待了
                            viewModel.clearSelection()
                        }
                    }
                }
            },
            onMapClick = viewModel::clearSelection,
            onMapReady = { map ->
                amap = map
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(DefaultCenter, DefaultZoom))

                // 首屏必须等地图真正加载完再取视野，否则 visibleRegion 还是空的
                map.setOnMapLoadedListener { publishViewport(map) }

                // 相机停稳后重算视野。onCameraChangeFinish 要等惯性彻底停下才触发，
                // 所以聚合不会在拖动过程中反复重排；范围没变时 ViewModel 会自己吞掉重拉
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
                // 定位途中再点一次没有意义，还会把上一次的监听器顶掉
                onLocateClick = { if (!locating) requestLocationPermission() }
            )
            MapSearchBar(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
            MapBottomBar(
                modifier = Modifier.fillMaxWidth()
            )
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
}

/**
 * 底部常驻浮层总高：底部操作栏（1dp 描边 + 上下各 4dp 留白 + 58dp 条目）+ 搜索条（上下各 8dp + 44dp）。
 * 高德要求「高德地图」Logo 始终可见，所以地图里的 Logo 要抬到这个高度之上。
 */
private val BottomOverlayHeight = 136.dp

/**
 * 定位要用的两个权限。
 *
 * 精确和粗略一起申请：Android 12 起用户可以只给「大致位置」，
 * 那时粗略定位同样能定出位置，不能因为精确那个没给就整条路走不通。
 */
private val LocationPermissions = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)

/** 默认中心点：广州，与 Web 端 `MapUtils.DEFAULT_CENTER` 一致。 */
private val DefaultCenter = LatLng(23.016485, 113.394405)

/**
 * 点开聚合气泡时的相机动画时长。
 *
 * 太快像跳帧，太慢让人干等，300ms 差不多就是「看得出它在飞过去，但不觉得慢」。
 */
private const val ClusterExpandDurationMs = 300L

/**
 * 初始缩放级别。
 *
 * Web 端 `DEFAULT_ZOOM = 24` 是它自己地图库的刻度，高德的缩放上限只有 20 左右，
 * 直接取上限的话一屏只有一百多米、附近基本不会有地点，所以这里取一个看得见周边饭馆的级别。
 */
private const val DefaultZoom = 15f

/**
 * 定位成功后至少放到这一级：街道级别，能看清自己站在哪条路上。
 * 与 Web `MapUtils.LOCATE_ME_MIN_ZOOM` 一致。
 */
private const val LocateZoom = 18f

/**
 * 相机飞到定位点的时长。
 *
 * 比展开聚合（300ms）略长：这一下是「从这里挪到那里」，距离可能很远，太快会让人觉得地图闪了一下。
 */
private const val LocateDurationMs = 400L

/**
 * 地图当前可见的经纬度范围。
 *
 * 地图还没加载完时 `getProjection()` 拿不到可见区域，返回 null 让调用方跳过这次请求。
 */
private fun AMap.visibleBounds(): MapBounds? {
    val bounds = projection?.visibleRegion?.latLngBounds ?: return null
    return MapBounds(
        minLng = bounds.southwest.longitude,
        minLat = bounds.southwest.latitude,
        maxLng = bounds.northeast.longitude,
        maxLat = bounds.northeast.latitude,
    )
}
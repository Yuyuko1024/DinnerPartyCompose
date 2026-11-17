package cc.dinnerparty.compose.ui.view.screen

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cc.dinnerparty.compose.R
import cc.dinnerparty.compose.data.model.MapCamera
import cc.dinnerparty.compose.data.model.MapDefaults
import cc.dinnerparty.compose.ui.view.component.AmapView
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.model.LatLng
import com.moriafly.salt.ui.Button
import com.moriafly.salt.ui.ButtonAppearance
import com.moriafly.salt.ui.ItemDivider
import com.moriafly.salt.ui.ItemEdit
import com.moriafly.salt.ui.ItemPopup
import com.moriafly.salt.ui.ItemSlider
import com.moriafly.salt.ui.ItemSwitcher
import com.moriafly.salt.ui.RoundedColumn
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.Text
import com.moriafly.salt.ui.UnstableSaltUiApi
import com.moriafly.salt.ui.enabledAlpha
import com.moriafly.salt.ui.innerPadding
import com.moriafly.salt.ui.popup.PopupMenuItem
import com.moriafly.salt.ui.popup.PopupState
import com.moriafly.salt.ui.popup.rememberPopupState
import kotlin.math.roundToInt

/** 滑块的可选档位数 */
private const val ZoomSteps = 16

/** 选点准星尺寸 */
private val PinSize = 34.dp

/** 准星的描边宽度 */
private const val PinStroke = 3f

/** 准星外圈黑边的宽度 */
private const val PinShadow = 6f

/** 准星中心点半径 */
private const val PinDot = 2.5f

/** 相邻控件的间距 */
private val FieldGap = 12.dp

/** 输入框距上方标签的间距 */
private val LabelGap = 2.dp

/** 分组之间的间距 */
private val SectionGap = 20.dp

/** 纬度合法范围 */
private val LatRange = -90.0..90.0

/** 经度合法范围 */
private val LngRange = -180.0..180.0

/** 地图样式选项 目前都不允许选择 */
private enum class MapStyleOption(val label: Int) {
    Standard(R.string.map_style_standard),
    Satellite(R.string.map_style_satellite),
    Street(R.string.map_style_street),
}

/** 动效开关的种类 */
enum class MapMotion { Cluster, Overlay }

/** 个性化地图页的数据 */
class MapSettingsPageUi(
    val centerLat: Double = MapDefaults.Latitude,
    val centerLng: Double = MapDefaults.Longitude,
    val zoom: Float = MapDefaults.Zoom,
    val clusterMotion: Boolean = true,
    val overlayMotion: Boolean = true,
)

/** 个性化地图页的操作 */
class MapSettingsPageOps(
    val onBack: () -> Unit = {},
    val onSave: (MapCamera) -> Unit = {},
    val onSwitch: (MapMotion, Boolean) -> Unit = { _, _ -> },
)

/** 个性化地图全屏页 */
@Suppress("DEPRECATION")
@UnstableSaltUiApi
@Composable
fun MapSettingsPage(
    ui: MapSettingsPageUi,
    ops: MapSettingsPageOps,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val invalidText = stringResource(R.string.map_invalid)
    var latText by remember { mutableStateOf(ui.centerLat.toString()) }
    var lngText by remember { mutableStateOf(ui.centerLng.toString()) }
    var zoom by remember { mutableFloatStateOf(ui.zoom) }
    var picking by remember { mutableStateOf(false) }
    val styleState = rememberPopupState()

    Box(modifier = modifier.fillMaxSize()) {
        PageScaffold(
            info = PageInfo(
                path = stringResource(R.string.map_page_path),
                backText = stringResource(R.string.back_to_settings),
            ),
            onBack = ops.onBack,
        ) {
            RoundedColumn(
                modifier = Modifier.padding(bottom = PagePadding),
                paddingValues = PaddingValues(vertical = CardPadding),
            ) {
                MapStyleRow(state = styleState)
                ItemDivider()
                CoordField(
                    label = R.string.map_center_lat,
                    text = latText,
                    onChange = { latText = it },
                )
                CoordField(
                    label = R.string.map_center_lng,
                    text = lngText,
                    onChange = { lngText = it },
                )
                ItemDivider()
                ItemSlider(
                    value = zoom,
                    onValueChange = { zoom = it },
                    text = stringResource(R.string.map_zoom),
                    sub = zoom.roundToInt().toString(),
                    valueRange = MapDefaults.MinZoom..MapDefaults.MaxZoom,
                    steps = ZoomSteps,
                )
            }
            Spacer(modifier = Modifier.height(FieldGap))
            Row(Modifier.fillMaxWidth()) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = { picking = true },
                    text = stringResource(R.string.map_pick),
                    appearance = ButtonAppearance.Subtle,
                )
                Spacer(modifier = Modifier.width(FieldGap))
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val latitude = latText.trim().toDoubleOrNull()
                        val longitude = lngText.trim().toDoubleOrNull()
                        if (latitude == null || longitude == null ||
                            latitude !in LatRange || longitude !in LngRange
                        ) {
                            Toast.makeText(context, invalidText, Toast.LENGTH_SHORT).show()
                        } else {
                            ops.onSave(MapCamera(latitude, longitude, zoom))
                        }
                    },
                    text = stringResource(R.string.map_save),
                )
            }
            Spacer(modifier = Modifier.height(SectionGap))
            Text(
                text = stringResource(R.string.map_motion),
                color = SaltTheme.colors.text,
                style = SaltTheme.textStyles.main,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(FieldGap))
            RoundedColumn(
                modifier = Modifier.padding(bottom = PagePadding),
                paddingValues = PaddingValues(vertical = CardPadding),
            ) {
                ItemSwitcher(
                    state = ui.clusterMotion,
                    onChange = { enabled -> ops.onSwitch(MapMotion.Cluster, enabled) },
                    text = stringResource(R.string.map_motion_cluster),
                    sub = stringResource(R.string.map_motion_cluster_hint),
                )
                ItemDivider()
                ItemSwitcher(
                    state = ui.overlayMotion,
                    onChange = { enabled -> ops.onSwitch(MapMotion.Overlay, enabled) },
                    text = stringResource(R.string.map_motion_overlay),
                    sub = stringResource(R.string.map_motion_overlay_hint),
                )
            }
        }

        if (picking) {
            MapPicker(
                center = LatLng(
                    latText.trim().toDoubleOrNull() ?: ui.centerLat,
                    lngText.trim().toDoubleOrNull() ?: ui.centerLng,
                ),
                zoom = zoom,
                onDismiss = { picking = false },
                onPick = { position ->
                    latText = position.latitude.toString()
                    lngText = position.longitude.toString()
                    picking = false
                },
            )
        }
    }
}

/** 地图样式一行 三种样式全部置灰不可选 */
@Suppress("DEPRECATION")
@UnstableSaltUiApi
@Composable
private fun MapStyleRow(state: PopupState) {
    ItemPopup(
        state = state,
        text = stringResource(R.string.map_style),
        sub = stringResource(R.string.map_style_standard),
    ) {
        MapStyleOption.entries.forEach { option ->
            Box(modifier = Modifier.enabledAlpha(false)) {
                PopupMenuItem(
                    onClick = {},
                    selected = option == MapStyleOption.Standard,
                    text = stringResource(option.label),
                    iconPainter = rememberVectorPainter(
                        if (option == MapStyleOption.Standard) {
                            Icons.Outlined.RadioButtonChecked
                        } else {
                            Icons.Outlined.RadioButtonUnchecked
                        }
                    ),
                )
            }
        }
    }
}

/** 经纬度输入一行 */
@UnstableSaltUiApi
@Composable
private fun CoordField(label: Int, text: String, onChange: (String) -> Unit) {
    Column(modifier = Modifier.innerPadding(vertical = false)) {
        Text(
            text = stringResource(label),
            modifier = Modifier.padding(top = FieldGap, bottom = LabelGap),
            color = SaltTheme.colors.subText,
            style = SaltTheme.textStyles.sub,
        )
        ItemEdit(
            text = text,
            onChange = onChange,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
    }
}

/** 地图选点浮层 取地图中心的坐标 */
@UnstableSaltUiApi
@Composable
private fun MapPicker(
    center: LatLng,
    zoom: Float,
    onDismiss: () -> Unit,
    onPick: (LatLng) -> Unit,
) {
    var map by remember { mutableStateOf<AMap?>(null) }
    BackHandler(onBack = onDismiss)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SaltTheme.colors.background)
    ) {
        AmapView(
            modifier = Modifier.fillMaxSize(),
            onMapReady = { amap ->
                map = amap
                amap.moveCamera(CameraUpdateFactory.newLatLngZoom(center, zoom))
            },
        )
        Pin(
            modifier = Modifier
                .align(Alignment.Center)
                .size(PinSize)
        )
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(PagePadding),
            horizontalArrangement = Arrangement.spacedBy(FieldGap),
        ) {
            Button(
                onClick = onDismiss,
                text = stringResource(R.string.action_cancel),
                appearance = ButtonAppearance.Subtle,
            )
            Button(
                onClick = { map?.cameraPosition?.target?.let(onPick) },
                text = stringResource(R.string.map_pick_confirm),
            )
        }
    }
}

/** 屏幕中心的十字准星 白色描边压深色黑边 */
@Composable
private fun Pin(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val midX = size.width / 2
        val midY = size.height / 2
        val arm = size.width / 2
        val horizontal = Pair(Offset(midX - arm, midY), Offset(midX + arm, midY))
        val vertical = Pair(Offset(midX, midY - arm), Offset(midX, midY + arm))
        for (pair in listOf(horizontal, vertical)) {
            drawLine(Color.Black, pair.first, pair.second, strokeWidth = PinShadow)
            drawLine(Color.White, pair.first, pair.second, strokeWidth = PinStroke)
        }
        drawCircle(Color.White, radius = PinDot, center = Offset(midX, midY))
    }
}

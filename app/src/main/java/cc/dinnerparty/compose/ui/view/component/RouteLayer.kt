package cc.dinnerparty.compose.ui.view.component

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.amap.api.maps.AMap
import com.amap.api.maps.model.BitmapDescriptor
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.Marker
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.maps.model.Polyline
import com.amap.api.maps.model.PolylineOptions
import kotlin.math.roundToInt

/** 地图上的一条路线方案 */
data class MapRoute(val points: List<LatLng>, val selected: Boolean)

/** 路线起点或终点的标记 */
data class MapEndpoint(val position: LatLng, val start: Boolean)

/** 路线折线的配色与线宽 */
data class RouteStyle(
    val color: Int,
    val selectedWidth: Float,
    val otherWidth: Float,
    val selectedAlpha: Float,
    val otherAlpha: Float,
)

/** 路线折线层 每次重绘整体替换 */
internal class RouteHolder {

    private val lines = mutableListOf<Polyline>()

    fun render(map: AMap, routes: List<MapRoute>, style: RouteStyle) {
        clear()
        routes.forEach { route ->
            if (route.points.size < MinRoutePoints) return@forEach
            val options = PolylineOptions()
                .addAll(route.points)
                .width(if (route.selected) style.selectedWidth else style.otherWidth)
                .color(style.color)
                .transparency(if (route.selected) style.selectedAlpha else style.otherAlpha)
                .zIndex(RouteZIndex)
            if (!route.selected) options.setDottedLine(true)
            lines += map.addPolyline(options)
        }
    }

    fun clear() {
        lines.forEach { it.remove() }
        lines.clear()
    }
}

/** 起终点标记层 每次重绘整体替换 */
internal class EndpointHolder {

    private val markers = mutableListOf<Marker>()

    fun render(map: AMap, endpoints: List<MapEndpoint>, icons: EndpointIcons) {
        clear()
        endpoints.forEach { endpoint ->
            val icon = if (endpoint.start) icons.start else icons.end
            val marker = map.addMarker(
                MarkerOptions()
                    .position(endpoint.position)
                    .icon(icon)
                    .anchor(0.5f, 0.5f)
                    .zIndex(EndpointZIndex)
            ) ?: return@forEach
            marker.setInfoWindowEnable(false)
            markers += marker
        }
    }

    fun clear() {
        markers.forEach { it.remove() }
        markers.clear()
    }
}

/** 起终点标记的两张图标 */
internal class EndpointIcons(val start: BitmapDescriptor, val end: BitmapDescriptor)

/** 标记图标的文案与配色 */
internal class EndpointIconSpec(val label: String, val fill: Int, val text: Int)

/** 画一张圆形带文字的标记图 */
internal fun endpointIcon(density: Float, spec: EndpointIconSpec): BitmapDescriptor {
    val size = (EndpointIconSize * density).roundToInt()
    val center = size / 2f
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = Color.WHITE
    canvas.drawCircle(center, center, center, paint)
    paint.color = spec.fill
    canvas.drawCircle(center, center, center - EndpointRingWidth * density, paint)
    paint.color = spec.text
    paint.textAlign = Paint.Align.CENTER
    paint.typeface = Typeface.DEFAULT_BOLD
    paint.textSize = size * EndpointTextRatio
    val baseline = center - (paint.descent() + paint.ascent()) / 2f
    canvas.drawText(spec.label, center, baseline, paint)
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

/** 单条路线至少两个点 */
private const val MinRoutePoints = 2

/** 折线层级 压在站内标记之下 */
private const val RouteZIndex = 0.5f

/** 起终点标记层级 与站内标记同层 */
private const val EndpointZIndex = 1f

/** 标记图的直径 单位 dp */
private const val EndpointIconSize = 26f

/** 标记图白边宽度 单位 dp */
private const val EndpointRingWidth = 1.5f

/** 标记文字占图高的比例 */
private const val EndpointTextRatio = 0.5f

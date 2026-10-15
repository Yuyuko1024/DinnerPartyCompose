package cc.dinnerparty.compose.ui.view.component

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.amap.api.maps.model.BitmapDescriptorFactory

/**
 * 「我的位置」的小蓝点。
 *
 * 照抄 Web `map/marker-motion.css` 的 `.fm-user-location`：外面一圈半透明光晕，里面一个白描边
 * 的实心蓝点。Web 那边的光晕是 2 秒一次的呼吸动画，这里画的是它静止时的样子——高德的标记换一次
 * 位图就要重建整张图，为一个呼吸效果付这个代价不值当。
 */
internal fun userLocationIcon(context: Context, density: Float): MarkerGraphic {
    val size = (UserLocationSizeDp * density).toInt().coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
        this.density = context.resources.displayMetrics.densityDpi
    }
    val canvas = Canvas(bitmap)
    val center = size / 2f

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // 光晕。Web 是 0.35→1 的呼吸动画，取静止帧的 0.7 倍，观感最接近
    paint.color = HaloColor
    canvas.drawCircle(center, center, center * HaloScale, paint)

    // 实心蓝点：一点投影，压在浅色底图上也能看清边界
    paint.color = DotColor
    paint.setShadowLayer(ShadowRadiusDp * density, 0f, ShadowDyDp * density, ShadowColor)
    canvas.drawCircle(center, center, DotRadiusDp * density, paint)
    paint.clearShadowLayer()

    // 白描边画在蓝点半径的内侧半格上，这样描边外沿正好落在蓝点半径上，
    // 与 Web 那边 `box-sizing: border-box` 的 18px 含 3px 描边一致
    val border = DotBorderDp * density
    paint.color = Color.WHITE
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = border
    canvas.drawCircle(center, center, DotRadiusDp * density - border / 2f, paint)

    return MarkerGraphic(
        descriptor = BitmapDescriptorFactory.fromBitmap(bitmap),
        // 蓝点以自身中心对准坐标，和 Web 的 `anchor: "center"` 一致
        anchorV = 0.5f,
    )
}

// 以下尺寸与 Web `map/marker-motion.css` 的 `.fm-user-location` 一一对应，单位是 dp（等价于 CSS px）

/** 光晕占满整个 44×44 的框。 */
private const val UserLocationSizeDp = 44f

/** 静止帧的光晕缩放，对应 Web `prefers-reduced-motion` 下的 `scale(0.7)`。 */
private const val HaloScale = 0.7f

/** 蓝点外径 18px（含 3px 描边）。 */
private const val DotRadiusDp = 9f
private const val DotBorderDp = 3f

/** Web 的 `rgba(26, 115, 232, 0.28)`。 */
private const val HaloColor = 0x471A73E8

/** Web 的 `#1a73e8`。 */
private const val DotColor = 0xFF1A73E8.toInt()

/** Web 的 `box-shadow: 0 1px 4px rgba(0, 0, 0, 0.35)`。 */
private const val ShadowRadiusDp = 2f
private const val ShadowDyDp = 1f
private const val ShadowColor = 0x59000000

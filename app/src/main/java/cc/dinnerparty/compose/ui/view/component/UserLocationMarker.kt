package cc.dinnerparty.compose.ui.view.component

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.amap.api.maps.model.BitmapDescriptorFactory

/**
 * 画「我的位置」蓝点：外圈半透明光晕，内圈白描边的实心蓝点。
 */
internal fun userLocationIcon(context: Context, density: Float): MarkerGraphic {
    val size = (UserLocationSizeDp * density).toInt().coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
        this.density = context.resources.displayMetrics.densityDpi
    }
    val canvas = Canvas(bitmap)
    val center = size / 2f

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // 光晕
    paint.color = HaloColor
    canvas.drawCircle(center, center, center * HaloScale, paint)

    // 实心蓝点，带投影
    paint.color = DotColor
    paint.setShadowLayer(ShadowRadiusDp * density, 0f, ShadowDyDp * density, ShadowColor)
    canvas.drawCircle(center, center, DotRadiusDp * density, paint)
    paint.clearShadowLayer()

    // 白描边
    val border = DotBorderDp * density
    paint.color = Color.WHITE
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = border
    canvas.drawCircle(center, center, DotRadiusDp * density - border / 2f, paint)

    return MarkerGraphic(
        descriptor = BitmapDescriptorFactory.fromBitmap(bitmap),
        // 蓝点以自身中心对准坐标
        anchorV = 0.5f,
    )
}

/** 蓝点整张位图的边长。 */
private const val UserLocationSizeDp = 44f

/** 光晕的缩放。 */
private const val HaloScale = 0.7f

/** 蓝点半径，含描边。 */
private const val DotRadiusDp = 9f
private const val DotBorderDp = 3f

private const val HaloColor = 0x471A73E8
private const val DotColor = 0xFF1A73E8.toInt()

/** 蓝点的投影。 */
private const val ShadowRadiusDp = 2f
private const val ShadowDyDp = 1f
private const val ShadowColor = 0x59000000

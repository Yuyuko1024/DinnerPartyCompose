package cc.dinnerparty.compose.ui.view.component

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.ContextCompat
import cc.dinnerparty.compose.R
import com.amap.api.maps.model.BitmapDescriptor
import com.amap.api.maps.model.BitmapDescriptorFactory
import kotlin.math.ceil
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min

/** 名称胶囊的配色，随主题变化。 */
internal data class MarkerLabelColors(
    val border: Int,
    val text: Int,
    val background: Int,
    val avoidedBorder: Int,
    val avoidedText: Int,
    val avoidedBackground: Int,
)

/** 一张标记位图，以及它上面「地理坐标」所在的位置。 */
internal data class MarkerGraphic(
    val descriptor: BitmapDescriptor,
    /** 交给 `MarkerOptions.anchor(0.5f, anchorV)` 的纵向锚点。 */
    val anchorV: Float,
)

/**
 * 把「图形 + 名称胶囊」画成一张位图，位图按「地点 + 是否选中」缓存。
 * 图形的底边中点压在经纬度上，名称胶囊在经纬度再往下 3px。
 */
internal class PlaceMarkerRenderer(
    private val context: Context,
    private val density: Float,
    private val colors: MarkerLabelColors,
) {

    private val cache = HashMap<Pair<MapMarker, Boolean>, MarkerGraphic>()
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }

    fun graphic(marker: MapMarker, selected: Boolean): MarkerGraphic =
        cache.getOrPut(marker to selected) { build(marker, selected) }

    private fun build(marker: MapMarker, selected: Boolean): MarkerGraphic {
        // textPaint 共用，重画前先重置
        textPaint.reset()
        textPaint.isAntiAlias = true
        textPaint.textAlign = Paint.Align.LEFT
        val count = marker.clusterCount
        return if (count == null) buildPlace(marker, selected) else buildCluster(count)
    }

    /** 单个地点的位图：冰淇淋图形 + 名称胶囊。 */
    private fun buildPlace(marker: MapMarker, selected: Boolean): MarkerGraphic {
        val glyphWidth = PlaceGlyphWidthDp * density
        val glyphHeight = PlaceGlyphHeightDp * density
        val labelGap = LabelGapDp * density
        val labelHeight = LabelHeightDp * density
        val labelPaddingStart = LabelPaddingStartDp * density
        val labelPaddingEnd = LabelPaddingEndDp * density
        val labelStrokeWidth = LabelStrokeWidthDp * density

        textPaint.textSize = LabelTextSizeDp * density
        textPaint.typeface = Typeface.create(
            Typeface.DEFAULT,
            if (selected) Typeface.BOLD else Typeface.NORMAL
        )

        // 名称单行截断，超长用省略号
        val label = if (marker.name.isBlank()) {
            ""
        } else {
            val room = LabelMaxWidthDp * density - labelPaddingStart - labelPaddingEnd -
                labelStrokeWidth * 2
            TextUtils.ellipsize(marker.name, textPaint, room, TextUtils.TruncateAt.END).toString()
        }
        val hasLabel = label.isNotEmpty()
        val labelWidth = if (hasLabel) {
            (textPaint.measureText(label) + labelPaddingStart + labelPaddingEnd +
                labelStrokeWidth * 2).coerceAtMost(LabelMaxWidthDp * density)
        } else {
            0f
        }

        val width = ceil(max(glyphWidth, labelWidth)).toInt().coerceAtLeast(1)
        val height = ceil(glyphHeight + if (hasLabel) labelGap + labelHeight else 0f)
            .toInt().coerceAtLeast(1)

        val bitmap = newBitmap(width, height)
        val canvas = Canvas(bitmap)
        val centerX = width / 2f

        // 图形：底边中点压在锚点上
        drawGlyph(
            canvas = canvas,
            resourceId = if (marker.isAvoided) R.drawable.ic_place_marker_no
            else R.drawable.ic_place_marker_yes,
            left = centerX - glyphWidth / 2f,
            top = 0f,
            width = glyphWidth,
            height = glyphHeight,
        )

        if (hasLabel) {
            val top = glyphHeight + labelGap
            val pill = RectF(
                centerX - labelWidth / 2f,
                top,
                centerX + labelWidth / 2f,
                top + labelHeight
            )
            val radius = LabelRadiusDp * density

            fillPaint.color = if (marker.isAvoided) colors.avoidedBackground else colors.background
            canvas.drawRoundRect(pill, radius, radius, fillPaint)

            strokePaint.color = if (marker.isAvoided) colors.avoidedBorder else colors.border
            strokePaint.strokeWidth = labelStrokeWidth
            canvas.drawRoundRect(pill, radius, radius, strokePaint)

            textPaint.color = if (marker.isAvoided) colors.avoidedText else colors.text
            canvas.drawText(
                label,
                pill.left + labelPaddingStart + labelStrokeWidth,
                pill.centerY() - (textPaint.descent() + textPaint.ascent()) / 2f,
                textPaint
            )
        }

        return MarkerGraphic(
            descriptor = BitmapDescriptorFactory.fromBitmap(bitmap),
            // 位图顶部到锚点的距离占整张位图的比例
            anchorV = glyphHeight / height,
        )
    }

    /** 聚合气泡的位图：水滴图形 + 中间的数量。 */
    private fun buildCluster(count: Int): MarkerGraphic {
        // 整体放大，图形尺寸与字号同比例
        val scale = clusterScale(count)
        val glyphWidth = ClusterGlyphWidthDp * density * scale
        val glyphHeight = ClusterGlyphHeightDp * density * scale

        val width = ceil(glyphWidth).toInt().coerceAtLeast(1)
        val height = ceil(glyphHeight).toInt().coerceAtLeast(1)

        val bitmap = newBitmap(width, height)
        val canvas = Canvas(bitmap)
        drawGlyph(
            canvas = canvas,
            resourceId = R.drawable.ic_place_marker_union,
            left = 0f,
            top = 0f,
            width = width.toFloat(),
            height = height.toFloat(),
        )

        val text = formatCount(count)
        textPaint.color = ClusterCountColor
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = clusterCountFontSize(text) * density * scale
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(
            text,
            width * ClusterCountCenterXRatio,
            height * ClusterCountCenterYRatio - (textPaint.descent() + textPaint.ascent()) / 2f,
            textPaint
        )

        return MarkerGraphic(
            descriptor = BitmapDescriptorFactory.fromBitmap(bitmap),
            // 锚点在图片底边
            anchorV = 1f,
        )
    }

    /** 按屏幕密度创建位图并标记密度。 */
    private fun newBitmap(width: Int, height: Int): Bitmap =
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            density = context.resources.displayMetrics.densityDpi
        }

    /** 把 drawable 拉满给定矩形画上去。 */
    private fun drawGlyph(
        canvas: Canvas,
        resourceId: Int,
        left: Float,
        top: Float,
        width: Float,
        height: Float,
    ) {
        val drawable = ContextCompat.getDrawable(context, resourceId) ?: return
        drawable.setBounds(left.toInt(), top.toInt(), (left + width).toInt(), (top + height).toInt())
        drawable.draw(canvas)
    }
}

/** 单个地点的冰淇淋图形尺寸。 */
private const val PlaceGlyphWidthDp = 36f
private const val PlaceGlyphHeightDp = 43f

/** 聚合气泡的水滴图形尺寸，再按 [clusterScale] 整体放大。 */
private const val ClusterGlyphWidthDp = 36f
private const val ClusterGlyphHeightDp = 52f

/** 数量在气泡框里的相对位置。 */
private const val ClusterCountCenterXRatio = 0.54f
private const val ClusterCountCenterYRatio = 0.30f

/** 数量固定为黑色。 */
private val ClusterCountColor = Color.BLACK

private const val LabelGapDp = 3f
private const val LabelHeightDp = 22f
private const val LabelMaxWidthDp = 140f
private const val LabelTextSizeDp = 12f
private const val LabelPaddingStartDp = 6f
private const val LabelPaddingEndDp = 8f
private const val LabelRadiusDp = 8f
private const val LabelStrokeWidthDp = 1f

/**
 * 气泡随数量放大并封顶，数量按对数缓慢增长。
 */
private fun clusterScale(count: Int): Float {
    if (count < 2) return 1f
    return min(MaxClusterScale, 1.0 + ClusterScaleFactor * log2(max(1.0, count / 2.0))).toFloat()
}

/** 数字位数越多字号越小。 */
private fun clusterCountFontSize(text: String): Float = when {
    text.length <= 2 -> 20f
    text.length == 3 -> 16f
    else -> 13f
}

/** 上千压成 `1.2k`。 */
private fun formatCount(count: Int): String = when {
    count < 1000 -> count.toString()
    count < 10000 -> {
        val tenths = count / 100
        if (tenths % 10 == 0) "${tenths / 10}k" else "${tenths / 10}.${tenths % 10}k"
    }

    else -> "${count / 1000}k"
}

private const val MaxClusterScale = 1.55
private const val ClusterScaleFactor = 0.09

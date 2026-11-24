package cc.dinnerparty.compose.data.tiles

import cc.dinnerparty.compose.data.model.MapBounds
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sinh

internal fun tileColumn(longitude: Double, zoom: Int): Int =
    floor((longitude + 180.0) / 360.0 * 2.0.pow(zoom)).toInt()
        .coerceIn(0, (1 shl zoom) - 1)

internal fun tileRow(latitude: Double, zoom: Int): Int {
    val sine = sin(latitude.coerceIn(-85.0511, 85.0511) * PI / 180.0)
    return floor((0.5 - ln((1 + sine) / (1 - sine)) / (4 * PI)) * 2.0.pow(zoom))
        .toInt().coerceIn(0, (1 shl zoom) - 1)
}

internal fun tileLongitude(column: Double, zoom: Int): Double =
    column / 2.0.pow(zoom) * 360.0 - 180.0

internal fun tileLatitude(row: Double, zoom: Int): Double =
    atan(sinh(PI * (1 - 2 * row / 2.0.pow(zoom)))) * 180.0 / PI

internal fun tileKeys(bounds: MapBounds, zoom: Int): List<String> {
    val firstCol = tileColumn(bounds.minLng, zoom)
    val lastCol = tileColumn(bounds.maxLng, zoom)
    val firstRow = tileRow(bounds.maxLat, zoom)
    val lastRow = tileRow(bounds.minLat, zoom)
    val columns = if (bounds.minLng <= bounds.maxLng) (firstCol..lastCol).toList()
        else (firstCol until (1 shl zoom)).toList() + (0..lastCol).toList()
    val centerCol = tileColumn(if (bounds.minLng <= bounds.maxLng) {
        (bounds.minLng + bounds.maxLng) / 2
    } else (bounds.minLng + bounds.maxLng) / 2 + 180, zoom)
    val centerRow = (firstRow + lastRow) / 2.0
    val width = (1 shl zoom).toDouble()
    val nearColumns = columns.sortedBy { column ->
        val distance = kotlin.math.abs(column - centerCol).toDouble()
        minOf(distance, width - distance)
    }.take(48)
    val nearRows = (firstRow..lastRow).sortedBy { kotlin.math.abs(it - centerRow) }.take(48)
    return nearColumns.asSequence().flatMap { column ->
        nearRows.asSequence().map { row -> column to row }
    }.sortedBy { (column, row) ->
        val distance = kotlin.math.abs(column - centerCol).toDouble()
        minOf(distance, width - distance).pow(2) + (row - centerRow).pow(2)
    }.take(48).map { (column, row) -> "$column/$row" }.toList()
}

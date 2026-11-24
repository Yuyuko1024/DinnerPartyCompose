package cc.dinnerparty.compose.data.tiles

import cc.dinnerparty.compose.api.model.Place
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlin.math.pow

internal class TileDecoder {
    private val gson = Gson()

    fun leaf(body: JsonObject): List<Place> {
        check(body.get("v").asInt == 1)
        val fields = body.getAsJsonArray("fields").map { it.asString }
        return body.getAsJsonArray("rows").map { entry ->
            val row = entry.asJsonArray
            check(row.size() == fields.size)
            val record = JsonObject()
            fields.forEachIndexed { index, field -> record.add(field, row[index]) }
            gson.fromJson(record, Place::class.java).copy(markerOnly = true)
        }
    }

    fun aggregate(
        body: JsonObject,
        dict: JsonObject,
        key: String,
        nextZoom: Int,
    ): List<Place> {
        check(body.get("v").asInt == 1)
        val zoom = body.get("z").asInt
        val cellZoom = body.get("cell").asInt
        val coordinates = key.split('/').map(String::toInt)
        val shift = 2.0.pow(cellZoom - zoom).toInt()
        val tags = dict.getAsJsonArray("tags")
        return body.getAsJsonArray("cells").flatMap { entry ->
            val cell = entry.asJsonArray
            val column = coordinates[0] * shift + cell[0].asInt
            val row = coordinates[1] * shift + cell[1].asInt
            val longitude = tileLongitude(column + (cell[2].asInt + 0.5) / 8, cellZoom)
            val latitude = tileLatitude(row + (cell[3].asInt + 0.5) / 8, cellZoom)
            cell[4].asJsonArray.mapIndexed { index, member ->
                val attributes = member.asJsonArray
                val names = attributes[0].asJsonArray.map { tags[it.asInt].asString }
                val identity = ((cellZoom.toLong() shl 28) or
                    (column.toLong() shl 14) or row.toLong()) shl 20
                check(index < (1 shl 20))
                Place(
                    id = -(identity + index + 1), name = "",
                    longitude = longitude, latitude = latitude,
                    category = names.joinToString(","),
                    perPersonCost = attributes[1].asInt.takeIf { it > 0 },
                    glyph = attributes[3].asInt,
                    aggregateZoom = nextZoom,
                    aggregateCell = listOf(cellZoom, column, row),
                )
            }
        }
    }
}

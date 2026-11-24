package cc.dinnerparty.compose.data.tiles

import cc.dinnerparty.compose.api.model.Place
import com.google.gson.annotations.SerializedName

data class TileLevel(
    val tile: Int = 0,
    val cell: Int = 0,
    val minZoom: Int = 0,
    val maxZoom: Int = 30,
)

data class TileManifest(
    val levels: List<TileLevel>,
    val leaf: TileLevel,
    val dict: String,
    val tiles: Map<String, Map<String, String>>,
) {
    val v: Int = 1

    fun band(zoom: Float): TileLevel = if (zoom >= leaf.minZoom) leaf
        else levels.firstOrNull { zoom >= it.minZoom && zoom <= it.maxZoom } ?: levels.last()

    fun nextZoom(level: TileLevel): Int =
        levels.firstOrNull { it.minZoom > level.minZoom }?.minZoom ?: leaf.minZoom
}

data class TileTicket(
    val ticket: String,
    @SerializedName("expires_at") val expiresAt: Long,
)

data class TileBatch(
    val places: List<Place>,
    val warning: String? = null,
    val level: TileLevel? = null,
)

@Target(AnnotationTarget.FUNCTION)
annotation class allowSilent(val reason: String)

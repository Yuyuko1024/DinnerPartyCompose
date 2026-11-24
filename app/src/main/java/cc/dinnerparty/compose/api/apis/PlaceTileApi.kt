package cc.dinnerparty.compose.api.apis

import cc.dinnerparty.compose.data.tiles.TileManifest
import cc.dinnerparty.compose.data.tiles.TileTicket
import com.google.gson.JsonObject
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface PlaceTileApi {
    @GET("places/tiles/manifest")
    suspend fun manifest(): TileManifest

    @GET("places/tiles/{hash}/dict")
    suspend fun dictionary(@Path("hash") hash: String): JsonObject

    @GET("places/tiles/ticket")
    suspend fun ticket(@Header("Authorization") authorization: String): TileTicket

    @GET("places/tiles/{hash}/{path}")
    suspend fun tile(
        @Path("hash") hash: String,
        @Path(value = "path", encoded = true) path: String,
        @Query("t") ticket: String?,
    ): JsonObject
}

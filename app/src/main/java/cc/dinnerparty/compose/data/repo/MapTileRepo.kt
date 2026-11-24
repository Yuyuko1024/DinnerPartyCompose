package cc.dinnerparty.compose.data.repo

import cc.dinnerparty.compose.api.ApiClient
import cc.dinnerparty.compose.api.AuthTokenHolder
import cc.dinnerparty.compose.api.apis.PlaceTileApi
import cc.dinnerparty.compose.api.model.Place
import cc.dinnerparty.compose.data.cluster.ClusterViewport
import cc.dinnerparty.compose.data.cluster.expanded
import cc.dinnerparty.compose.data.tiles.TileBatch
import cc.dinnerparty.compose.data.tiles.TileDecoder
import cc.dinnerparty.compose.data.tiles.TileDiskCache
import cc.dinnerparty.compose.data.tiles.TileLevel
import cc.dinnerparty.compose.data.tiles.TileManifest
import cc.dinnerparty.compose.data.tiles.TileTicket
import cc.dinnerparty.compose.data.tiles.allowSilent
import cc.dinnerparty.compose.data.tiles.tileColumn
import cc.dinnerparty.compose.data.tiles.tileKeys
import cc.dinnerparty.compose.data.tiles.tileRow
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class MapTileRepo(
    cacheDir: File,
    private val api: PlaceTileApi = ApiClient.createService(PlaceTileApi::class.java),
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val disk = TileDiskCache(File(cacheDir, "map-tiles"))
    private val decoder = TileDecoder()
    private val memory = LinkedHashMap<String, List<Place>>(64, 0.75f, true)
    private var manifest: TileManifest? = null
    private var dictionary: JsonObject? = null
    private var manifestAt = 0L
    private var token: String? = null
    private var ticket: TileTicket? = null
    private var limitedUntil = 0L
    private var warning: String? = null
    private val patches = LinkedHashMap<Long, Pair<Place, Long>>()

    fun patch(place: Place) {
        patches[checkNotNull(place.id)] = place to now()
    }

    suspend fun update(view: ClusterViewport, force: Boolean): TileBatch {
        warning = null
        val currentToken = AuthTokenHolder.token
        if (currentToken != token) {
            token = currentToken
            ticket = null
            limitedUntil = 0
        }
        if (force || manifest == null || now() - manifestAt >= 300_000) refresh()
        val current = checkNotNull(manifest)
        val band = current.band(view.zoom)
        val leaf = band.tile == current.leaf.tile
        val bounds = if (leaf) view.bounds else view.bounds.expanded(0.35)
        val places = ArrayList<Place>()
        for (key in tileKeys(bounds, band.tile)) {
            val loaded = load(band, key)
            if (loaded != null) places += loaded
            else if (leaf) places += fallback(key)
        }
        patches.entries.removeAll { now() - it.value.second >= 600_000 }
        if (leaf) {
            val merged = places.associateByTo(LinkedHashMap()) { it.id }
            patches.entries.removeAll { (id, patch) ->
                val cached = merged[id]
                val saved = patch.first
                cached != null && cached.name == saved.name && cached.category == saved.category &&
                    cached.longitude == saved.longitude && cached.latitude == saved.latitude &&
                    cached.perPersonCost == saved.perPersonCost &&
                    cached.ratingCount == saved.ratingCount && cached.ratingAverage == saved.ratingAverage
            }
            patches.forEach { (id, patch) -> merged[id] = patch.first }
            return TileBatch(merged.values.toList(), warning, band)
        }
        return TileBatch(places, warning, band)
    }

    private suspend fun refresh() {
        val fresh = api.manifest()
        check(fresh.v == 1 && fresh.levels.isNotEmpty())
        check(fresh.dict.matches(Regex("[0-9a-f]{12}")))
        check(fresh.leaf.tile in 0..14)
        fresh.levels.forEach {
            check(it.tile in 0..fresh.leaf.tile && it.cell in it.tile..14)
        }
        val dict = if (manifest?.dict == fresh.dict) checkNotNull(dictionary)
            else disk.read("dict_${fresh.dict}") ?: api.dictionary(fresh.dict).also {
                disk.write("dict_${fresh.dict}", it)
            }
        check(dict.getAsJsonArray("tags") != null && dict.getAsJsonArray("regions") != null)
        manifest = fresh
        dictionary = dict
        manifestAt = now()
    }

    private suspend fun load(level: TileLevel, key: String): List<Place>? {
        val current = checkNotNull(manifest)
        val hash = current.tiles[level.tile.toString()]?.get(key) ?: return emptyList()
        check(hash.matches(Regex("[0-9a-f]{12}")))
        val leaf = level.tile == current.leaf.tile
        val cacheKey = "${hash}_${level.tile}_${key.replace('/', '_')}"
        val memoryKey = "$cacheKey@${current.dict}"
        memory[memoryKey]?.let { return it }
        val stored = disk.read(cacheKey)
        val body = stored ?: request(level, key, hash)?.also { disk.write(cacheKey, it) }
            ?: return null
        val places = withContext(Dispatchers.Default) {
            if (leaf) decoder.leaf(body)
            else decoder.aggregate(body, checkNotNull(dictionary), key, current.nextZoom(level))
        }
        memory[memoryKey] = places
        while (memory.size > 96) memory.remove(memory.keys.first())
        return places
    }

    @allowSilent("瓦片请求失败时显示原因并使用匿名聚合补位")
    private suspend fun request(level: TileLevel, key: String, hash: String): JsonObject? {
        val leaf = level.tile == checkNotNull(manifest).leaf.tile
        if (leaf && now() < limitedUntil) {
            warning = "明细瓦片加载额度已用完，暂时显示聚合点"
            return null
        }
        val credential = if (leaf) ticketQuery() else null
        return try {
            api.tile(hash, "${level.tile}/$key", credential)
        } catch (error: CancellationException) {
            throw error
        } catch (error: HttpException) {
            val raw = error.response()?.errorBody()?.string()
            val failure = runCatching { JsonParser.parseString(raw).asJsonObject }.getOrNull()
            val message = failure?.get("error")?.asString ?: error.message()
            if (leaf && error.code() == 429) {
                val retry = failure?.get("retry_after")?.asLong
                    ?: error.response()?.headers()?.get("Retry-After")?.toLongOrNull() ?: 3600
                limitedUntil = now() + retry * 1000
            }
            if (error.code() == 404) manifestAt = 0
            warning = message
            null
        } catch (error: IOException) {
            warning = error.message ?: "地图瓦片加载失败"
            null
        }
    }

    @allowSilent("票据请求失败时提示原因并按游客加载瓦片")
    private suspend fun ticketQuery(): String? {
        val currentToken = token ?: return null
        ticket?.takeIf { it.expiresAt - now() > 3_600_000 }?.let { return it.ticket }
        return try {
            api.ticket("Bearer $currentToken").also { ticket = it }.ticket
        } catch (error: CancellationException) {
            throw error
        } catch (error: HttpException) {
            warning = "地图票据获取失败，按游客加载，${error.code()}"
            null
        } catch (error: IOException) {
            warning = "地图票据获取失败，按游客加载，${error.message}"
            null
        }
    }

    private suspend fun fallback(key: String): List<Place> {
        val current = checkNotNull(manifest)
        val level = current.levels.last()
        val coordinates = key.split('/').map(String::toInt)
        val shift = 1 shl (current.leaf.tile - level.tile)
        val parent = "${coordinates[0] / shift}/${coordinates[1] / shift}"
        return load(level, parent).orEmpty().filter { place ->
            tileColumn(place.longitude!!, current.leaf.tile) == coordinates[0] &&
                tileRow(place.latitude!!, current.leaf.tile) == coordinates[1]
        }
    }
}

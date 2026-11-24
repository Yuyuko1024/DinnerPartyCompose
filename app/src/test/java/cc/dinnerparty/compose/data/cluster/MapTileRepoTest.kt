package cc.dinnerparty.compose.data.cluster

import cc.dinnerparty.compose.api.AuthTokenHolder
import cc.dinnerparty.compose.api.apis.PlaceTileApi
import cc.dinnerparty.compose.data.model.MapBounds
import cc.dinnerparty.compose.data.repo.MapClusterRepo
import cc.dinnerparty.compose.data.repo.MapTileRepo
import cc.dinnerparty.compose.data.tiles.TileDecoder
import cc.dinnerparty.compose.data.tiles.TileManifest
import cc.dinnerparty.compose.data.tiles.TileTicket
import cc.dinnerparty.compose.data.tiles.tileKeys
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.IOException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import retrofit2.HttpException
import retrofit2.Response

class MapTileRepoTest {
    @get:Rule val folder = TemporaryFolder()
    private val view = ClusterViewport(MapBounds(0.001, 0.001, 0.002, 0.002), 13f, 20, 68.0)

    @After fun clearToken() {
        AuthTokenHolder.token = null
    }

    @Test fun manifest_uses_web_bands_and_initializes_leaf_defaults() {
        val manifest = FakeApi().manifestBody
        assertEquals(listOf(2, 2, 5, 5, 8, 8, 12),
            listOf(0f, 7f, 8f, 10f, 11f, 12f, 13f).map { manifest.band(it).tile })
        assertEquals(30, manifest.leaf.maxZoom)
        assertEquals(0, manifest.leaf.cell)
    }

    @Test fun anonymous_members_have_stable_keys_and_no_server_ids_or_names() = runBlocking {
        val api = FakeApi()
        val repo = MapTileRepo(folder.root, api)
        val aggregateView = view.copy(zoom = 12f)
        val first = repo.update(aggregateView, false).places.single()
        assertTrue(first.id!! < 0)
        assertEquals("", first.name)
        assertEquals("咖啡", first.category)
        assertEquals(1, first.glyph)
        assertEquals(13, first.aggregateZoom)
        assertEquals(first, repo.update(aggregateView, false).places.single())
        assertEquals(listOf("8/128/127"), api.paths)
        assertEquals(0, api.ticketCalls)
    }

    @Test fun detail_reads_field_order_and_fetches_only_visible_tiles() = runBlocking {
        val api = FakeApi()
        val places = MapTileRepo(folder.root, api).update(view, false).places
        assertEquals(42L, places.single().id)
        assertEquals("咖啡店", places.single().name)
        assertTrue(places.single().markerOnly)
        assertNull(places.single().aggregateZoom)
        assertEquals(listOf("12/2048/2047"), api.paths)
    }

    @Test fun disk_cache_survives_repository_recreation_without_spending_quota() = runBlocking {
        val api = FakeApi()
        val first = MapTileRepo(folder.root, api).update(view, false)
        val restored = MapTileRepo(folder.root, api).update(view, false)
        assertEquals(first.places, restored.places)
        assertEquals(1, api.paths.size)
        assertEquals(1, api.dictCalls)
    }

    @Test fun manifest_hash_change_replaces_cached_detail() = runBlocking {
        val api = FakeApi()
        val repo = MapTileRepo(folder.root, api)
        repo.update(view, false)
        api.manifestBody = api.manifestBody.copy(tiles = mapOf("12" to mapOf("2048/2047" to "333333333333")))
        api.leafBody = json("""{"v":1,"fields":["id","name","longitude","latitude"],"rows":[[43,"新店",0.001,0.001]]}""")
        assertEquals(43L, repo.update(view, true).places.single().id)
        assertEquals(2, api.paths.size)
    }

    @Test fun quota_failure_uses_city_aggregate_and_stops_further_leaf_requests() = runBlocking {
        val api = FakeApi()
        api.leafError = HttpException(Response.error<JsonObject>(429,
            """{"error":"明细额度用完"}""".toResponseBody()))
        val repo = MapTileRepo(folder.root, api)
        val result = repo.update(view, false)
        assertEquals(13, result.places.single().aggregateZoom)
        assertEquals("明细额度用完", result.warning)
        assertNotNull(repo.update(view, false).warning)
        assertEquals(1, api.paths.count { it.startsWith("12/") })
    }

    @Test fun network_failure_falls_back_and_remains_retryable() = runBlocking {
        val api = FakeApi()
        api.leafError = IOException("offline")
        val repo = MapTileRepo(folder.root, api)
        assertEquals("offline", repo.update(view, false).warning)
        api.leafError = null
        assertEquals(42L, repo.update(view, false).places.single().id)
    }

    @Test fun login_uses_ticket_and_logout_never_reuses_it() = runBlocking {
        val api = FakeApi()
        AuthTokenHolder.token = "resident"
        val repo = MapTileRepo(folder.root, api)
        repo.update(view, false)
        assertEquals(listOf("ticket-resident"), api.credentials)
        AuthTokenHolder.token = null
        api.manifestBody = api.manifestBody.copy(tiles = mapOf("12" to mapOf("2048/2047" to "444444444444")))
        repo.update(view, true)
        assertEquals(listOf("ticket-resident", null), api.credentials)
    }

    @Test fun failed_ticket_warns_and_loads_as_guest() = runBlocking {
        val api = FakeApi()
        AuthTokenHolder.token = "resident"
        api.ticketError = IOException("ticket offline")
        val result = MapTileRepo(folder.root, api).update(view, false)
        assertTrue(result.warning!!.contains("票据获取失败"))
        assertEquals(42L, result.places.single().id)
        assertNull(api.credentials.single())
    }

    @Test fun anonymous_singles_and_groups_zoom_instead_of_opening_members() = runBlocking {
        val tileRepo = MapTileRepo(folder.root, FakeApi())
        val repo = MapClusterRepo(tileRepo::update)
        val aggregateView = view.copy(zoom = 12f)
        val single = repo.update(aggregateView).single()
        assertEquals(ClusterClick.Zoom(13f), repo.clusterClick(single.id!!, 12f, 20f))
        val double = single.copy(id = single.id - 1)
        val groups = MapClusterRepo({ _, _ -> cc.dinnerparty.compose.data.tiles.TileBatch(listOf(single, double)) })
        groups.update(aggregateView)
        val group = groups.query(aggregateView).single() as MapClusterNode.Group
        assertEquals(ClusterClick.Zoom(13f), groups.clusterClick(group.id, 12f, 20f))
    }

    @Test fun saved_edits_override_stale_tiles_only_in_detail_band() = runBlocking {
        val repo = MapTileRepo(folder.root, FakeApi())
        val place = repo.update(view, false).places.single().copy(name = "已修改")
        repo.patch(place)
        assertEquals("已修改", repo.update(view, true).places.single().name)
        assertEquals("", repo.update(view.copy(zoom = 12f), false).places.single().name)
    }

    @Test fun coverage_handles_dateline_and_caps_world_queries() {
        val crossing = tileKeys(MapBounds(179.9, -0.1, -179.9, 0.1), 2)
        assertTrue(crossing.any { it.startsWith("3/") })
        assertTrue(crossing.any { it.startsWith("0/") })
        assertEquals(48, tileKeys(MapBounds(-180.0, -85.0, 180.0, 85.0), 12).size)
    }

    @Test fun unsupported_tile_format_is_not_silently_dropped() {
        val failed = runCatching { TileDecoder().leaf(json("""{"v":2,"fields":[],"rows":[]}""")) }
        assertTrue(failed.exceptionOrNull() is IllegalStateException)
    }

    @Test fun band_switch_keeps_previous_snapshot_until_new_band_is_ready() = runBlocking {
        val source = MapTileRepo(folder.root, FakeApi())
        val oldView = view.copy(zoom = 12f)
        val oldBatch = source.update(oldView, false)
        val newBatch = source.update(view, false)
        val pending = CompletableDeferred<cc.dinnerparty.compose.data.tiles.TileBatch>()
        var first = true
        val repo = MapClusterRepo({ _, _ ->
            if (first) { first = false; oldBatch } else pending.await()
        })
        repo.update(oldView)
        val oldNodes = repo.snapshot(oldView).nodes
        val loading = async(start = CoroutineStart.UNDISPATCHED) { repo.update(view) }
        assertEquals(12, repo.snapshot(view).zoom)
        assertEquals(oldNodes, repo.snapshot(view).nodes)
        pending.complete(newBatch)
        loading.await()
        assertEquals(13, repo.snapshot(view).zoom)
        assertEquals(42L, (repo.snapshot(view).nodes.single() as MapClusterNode.Single).place.id)
    }

    private class FakeApi : PlaceTileApi {
        var manifestBody = Gson().fromJson("""{
          "v":1,"dict":"dddddddddddd",
          "levels":[{"tile":2,"cell":9,"minZoom":0,"maxZoom":7},
            {"tile":5,"cell":12,"minZoom":8,"maxZoom":10},
            {"tile":8,"cell":14,"minZoom":11,"maxZoom":12}],
          "leaf":{"tile":12,"minZoom":13},
          "tiles":{"8":{"128/127":"888888888888"},"12":{"2048/2047":"121212121212"}}
        }""", TileManifest::class.java)
        var leafBody = json("""{"v":1,"fields":["name","id","latitude","longitude"],"rows":[["咖啡店",42,0.001,0.001]]}""")
        var leafError: Exception? = null
        var ticketError: Exception? = null
        var ticketCalls = 0
        var dictCalls = 0
        val paths = ArrayList<String>()
        val credentials = ArrayList<String?>()

        override suspend fun manifest() = manifestBody

        override suspend fun dictionary(hash: String): JsonObject {
            dictCalls++
            return json("""{"tags":["咖啡"],"regions":[["省","市","区"]]}""")
        }

        override suspend fun ticket(authorization: String): TileTicket {
            ticketCalls++
            ticketError?.let { throw it }
            return TileTicket("ticket-${authorization.removePrefix("Bearer ")}", Long.MAX_VALUE)
        }

        override suspend fun tile(hash: String, path: String, ticket: String?): JsonObject {
            paths += path
            if (path.startsWith("12/")) {
                credentials += ticket
                leafError?.let { throw it }
                return leafBody
            }
            return json("""{"v":1,"z":8,"cell":14,"cells":[[0,63,0,7,[[[0],12,0,1]]]]}""")
        }
    }

    companion object {
        private fun json(content: String): JsonObject = JsonParser.parseString(content).asJsonObject
    }
}

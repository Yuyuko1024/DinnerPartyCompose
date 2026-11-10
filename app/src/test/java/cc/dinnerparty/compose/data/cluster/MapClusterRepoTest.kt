package cc.dinnerparty.compose.data.cluster

import cc.dinnerparty.compose.api.model.Place
import cc.dinnerparty.compose.data.model.MapBounds
import cc.dinnerparty.compose.data.repo.MapClusterRepo
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class MapClusterRepoTest {
    private val bounds = MapBounds(112.0, 22.0, 114.0, 24.0)
    private val view = ClusterViewport(bounds, 10f, 20, 68.0)
    private val places = listOf(
        Place(id = 1, longitude = 113.0, latitude = 23.0, name = "first"),
        Place(id = 2, longitude = 113.0001, latitude = 23.0),
    )

    @Test
    fun covered_camera_updates_reuse_index_and_skip_network() = runBlocking {
        var calls = 0
        val repo = MapClusterRepo({ calls++; places })
        repo.update(view)
        val group = repo.query(view).single() as MapClusterNode.Group
        repo.update(view.copy(zoom = 11f))
        assertEquals(1, calls)
        assertEquals(group.id, (repo.query(view).single() as MapClusterNode.Group).id)
    }

    @Test
    fun changed_radius_or_map_limit_rebuilds_even_with_cached_data() = runBlocking {
        var calls = 0
        val repo = MapClusterRepo({ calls++; places })
        repo.update(view)
        val old = repo.query(view).single() as MapClusterNode.Group
        repo.update(view.copy(radiusPx = 150.0))
        val fresh = repo.query(view).single() as MapClusterNode.Group
        assertNotEquals(old.id, fresh.id)
        val limited = view.copy(mapMaxZoom = 10)
        repo.update(limited)
        val terminal = repo.query(limited).single() as MapClusterNode.Group
        assertEquals(11, repo.expansionZoom(terminal.id))
        assertEquals(ClusterClick.Members(places), repo.clusterClick(terminal.id, 10f, 10f))
        assertEquals(1, calls)
    }

    @Test
    fun forced_reload_updates_appearance_without_changing_topology() = runBlocking {
        var response = places
        val repo = MapClusterRepo({ response })
        repo.update(view)
        val group = repo.query(view).single() as MapClusterNode.Group
        response = places.map { it.copy(name = "updated") }
        repo.update(view, force = true)
        assertEquals(group.id, (repo.query(view).single() as MapClusterNode.Group).id)
        repo.update(view.copy(radiusPx = 1.0))
        val singles = repo.query(view.copy(zoom = 20f, radiusPx = 1.0)).filterIsInstance<MapClusterNode.Single>()
        assertTrue(singles.all { it.place.name == "updated" })
    }

    @Test
    fun terminal_cluster_opens_all_members_without_a_zoom_jump() = runBlocking {
        val overlap = places.map { it.copy(longitude = 113.0) }
        val repo = MapClusterRepo({ overlap })
        repo.update(view)
        val group = repo.query(view).single() as MapClusterNode.Group
        assertEquals(21, repo.expansionZoom(group.id))
        assertEquals(ClusterClick.Members(overlap), repo.clusterClick(group.id, 10f, 20f))
        assertEquals(ClusterClick.Members(overlap), repo.clusterClick(group.id, 20f, 20f))
    }

    @Test
    fun non_terminal_cluster_zooms_to_its_next_real_split() = runBlocking {
        val spread = listOf(places[0], places[1].copy(longitude = 113.01))
        val repo = MapClusterRepo({ spread })
        repo.update(view)
        val group = repo.query(view).single() as MapClusterNode.Group
        val expansion = repo.expansionZoom(group.id)!!
        assertTrue(expansion in 11..20)
        assertEquals(ClusterClick.Zoom(expansion.toFloat()), repo.clusterClick(group.id, 10f, 20f))
        assertEquals(ClusterClick.Members(spread), repo.clusterClick(group.id, 19.96f, 20f))
    }

    @Test
    fun fractional_map_maximum_is_used_when_deciding_whether_to_show_members() = runBlocking {
        val overlap = places.map { it.copy(longitude = 113.0) }
        val repo = MapClusterRepo({ overlap })
        repo.update(view.copy(mapMaxZoom = 21))
        val group = repo.query(view).single() as MapClusterNode.Group
        assertEquals(22, repo.expansionZoom(group.id))
        assertEquals(ClusterClick.Members(overlap), repo.clusterClick(group.id, 20.5f, 20.5f))
        assertNull(repo.clusterClick(-1, 20.5f, 20.5f))
    }

    @Test
    fun terminal_members_are_deduplicated_and_sorted_by_web_place_keys() = runBlocking {
        val overlap = listOf(places[0].copy(id = 2), places[0].copy(id = 10),
            places[0], places[0].copy(id = 2, name = "duplicate"))
        val repo = MapClusterRepo({ overlap })
        repo.update(view)
        val group = repo.query(view).single() as MapClusterNode.Group
        val action = repo.clusterClick(group.id, 20f, 20f) as ClusterClick.Members
        assertEquals(listOf(1L, 10L, 2L), action.places.map { it.id })
        assertEquals(3, group.count)
    }

    @Test
    fun late_response_cannot_replace_newer_viewport() = runBlocking {
        val delayed = CompletableDeferred<List<Place>>()
        var calls = 0
        val repo = MapClusterRepo({ calls++; if (calls == 1) delayed.await() else places })
        val old = async(start = CoroutineStart.UNDISPATCHED) { repo.update(view) }
        repo.update(view)
        val group = repo.query(view).single() as MapClusterNode.Group
        delayed.complete(listOf(Place(id = 99, longitude = 113.0, latitude = 23.0)))
        old.await()
        assertEquals(group.id, (repo.query(view).single() as MapClusterNode.Group).id)
        assertEquals(listOf(1L, 2L), repo.update(view).map { it.id })
    }

    @Test
    fun failed_request_keeps_last_successful_markers_and_can_retry() = runBlocking {
        var fail = false
        val repo = MapClusterRepo({ if (fail) error("network failure") else places })
        repo.update(view)
        val previous = repo.query(view)
        fail = true
        val failure = runCatching { repo.update(view, force = true) }
        assertTrue(failure.isFailure)
        assertEquals(previous, repo.query(view))
        fail = false
        assertEquals(places, repo.update(view))
    }
}

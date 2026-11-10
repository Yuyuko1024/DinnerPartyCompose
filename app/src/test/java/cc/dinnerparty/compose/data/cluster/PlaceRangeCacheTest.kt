package cc.dinnerparty.compose.data.cluster

import cc.dinnerparty.compose.api.model.Place
import cc.dinnerparty.compose.data.model.MapBounds
import org.junit.Assert.*
import org.junit.Test

class PlaceRangeCacheTest {
    private val view = MapBounds(112.0, 22.0, 114.0, 24.0)

    @Test
    fun prefetch_covers_small_pans_and_expires_after_two_minutes() {
        var now = 1000L
        val cache = PlaceRangeCache { now }
        assertTrue(cache.needsFetch(view))
        cache.apply(view.expanded(1.0), emptyList())
        assertFalse(cache.needsFetch(view))
        assertFalse(cache.needsFetch(MapBounds(112.1, 22.1, 114.1, 24.1)))
        assertTrue(cache.needsFetch(MapBounds(114.0, 22.0, 116.0, 24.0)))
        now += 120_001
        assertTrue(cache.needsFetch(view))
    }

    @Test
    fun response_removes_deleted_places_only_inside_requested_region() {
        val cache = PlaceRangeCache()
        cache.apply(view, listOf(place(1, 113.0), place(2, 113.1)))
        val other = MapBounds(115.0, 22.0, 117.0, 24.0)
        cache.apply(other, listOf(place(3, 116.0)))
        cache.apply(view, listOf(place(2, 113.2)))
        assertEquals(listOf(2L, 3L), cache.all().map { it.id })
        assertEquals(113.2, cache.all().first().longitude!!, 0.0)
    }

    @Test
    fun invalidation_keeps_markers_but_requires_a_new_request() {
        val cache = PlaceRangeCache()
        cache.apply(view.expanded(1.0), listOf(place(1, 113.0)))
        cache.invalidate()
        assertEquals(1, cache.all().size)
        assertTrue(cache.needsFetch(view))
    }

    @Test
    fun oldest_regions_are_evicted_and_latest_dense_region_is_retained() {
        val cache = PlaceRangeCache { 1000L }
        for (index in 0..12) {
            val longitude = index * 10.0 - 100
            cache.apply(MapBounds(longitude, 22.0, longitude + 1, 24.0),
                listOf(place(index.toLong(), longitude + 0.5)))
        }
        assertEquals(12, cache.all().size)
        assertFalse(cache.all().any { it.id == 0L })
        assertTrue(cache.all().any { it.id == 12L })
    }

    private fun place(id: Long, longitude: Double) =
        Place(id = id, longitude = longitude, latitude = 23.0)
}

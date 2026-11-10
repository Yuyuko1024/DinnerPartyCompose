package cc.dinnerparty.compose.data.cluster

import cc.dinnerparty.compose.api.model.Place
import cc.dinnerparty.compose.data.model.MapBounds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 聚合算法的回归测试。 */
class PlaceClusterIndexTest {

    @Test
    fun nearby_places_merge_when_zoomed_out() {
        val index = indexOf(place(1, 113.0000, 23.0000), place(2, 113.0004, 23.0004))

        val nodes = index.query(EVERYTHING, zoom = 6f)

        assertEquals(listOf(2), nodes.filterIsInstance<MapClusterNode.Group>().map { it.count })
        assertTrue(nodes.filterIsInstance<MapClusterNode.Single>().isEmpty())
    }

    @Test
    fun expansion_zoom_actually_splits_the_cluster() {
        val index = indexOf(place(1, 113.0000, 23.0000), place(2, 113.0010, 23.0010))
        val group = index.query(EVERYTHING, zoom = 6f)
            .filterIsInstance<MapClusterNode.Group>()
            .single()

        val expansion = index.expansionZoom(group.id)!!
        assertTrue("只能往里放大", expansion > 6)

        // 放到这一级，气泡应该换成下一层
        val atExpansion = index.query(EVERYTHING, expansion.toFloat())
        assertEquals(2, atExpansion.count { it is MapClusterNode.Single })
    }

    /** 一小撮坐标几乎重合的点，不该把整个聚合的展开级别拖到最大级别。 */
    @Test
    fun near_duplicate_points_do_not_drag_expansion_to_max_zoom() {
        val index = PlaceClusterIndex(provincePlaces(), mapMaxZoom = MaxZoom, radiusPx = RadiusPx)
        val group = index.query(EVERYTHING, zoom = 5f)
            .filterIsInstance<MapClusterNode.Group>()
            .maxByOrNull { it.count }!!

        assertEquals(17, group.count)

        // 这一片铺开约 5km，展开级别应该远小于上限
        val expansion = index.expansionZoom(group.id)!!
        assertTrue("展开级别被拖到了退出聚合那一级：$expansion", expansion < MaxZoom - 1)
    }


    @Test
    fun clicking_a_cluster_shows_a_finer_level_each_time() {
        val index = PlaceClusterIndex(provincePlaces(), mapMaxZoom = MaxZoom, radiusPx = RadiusPx)
        var level = 5
        var shown = index.query(EVERYTHING, level.toFloat()).size
        var clicks = 0

        while (clicks < 10) {
            val group = index.query(EVERYTHING, level.toFloat())
                .filterIsInstance<MapClusterNode.Group>()
                .maxByOrNull { it.count } ?: break

            val next = index.expansionZoom(group.id)!!
            assertTrue("点击必须往里放大：$level → $next", next > level)

            if (next > MaxZoom) break
            level = next
            val after = index.query(EVERYTHING, level.toFloat()).size
            assertTrue("展开之后应该看到更细的一层：$shown → $after", after > shown)
            shown = after
            clicks++
        }

        assertTrue("常规点先逐级展开，剩余过近地点交给成员选择", clicks > 0)
    }

    @Test
    fun distant_places_stay_separate_outside_radius() {
        val index = indexOf(place(1, 113.0, 23.0), place(2, 116.4, 39.9))

        val nodes = index.query(EVERYTHING, zoom = 8f)

        assertEquals(2, nodes.count { it is MapClusterNode.Single })
    }

    @Test
    fun neighbouring_places_remain_clustered_when_still_inside_radius_at_max_zoom() {
        val index = PlaceClusterIndex(
            listOf(place(1, 119.9750, 31.8110), place(2, 119.9751, 31.8110)),
            mapMaxZoom = MaxZoom,
            radiusPx = RadiusPx,
        )
        val group = index.query(EVERYTHING, MaxZoom.toFloat()).single() as MapClusterNode.Group
        assertEquals(2, group.count)
        assertEquals(MaxZoom + 1, index.expansionZoom(group.id))
        assertEquals(listOf(1L, 2L), index.members(group.id).map { it.id })
    }

    /** 半径调大之后，同一个缩放级别下的气泡应该更少。 */
    @Test
    fun larger_radius_produces_fewer_bubbles() {
        val places = provincePlaces()
        val small = PlaceClusterIndex(places, mapMaxZoom = MaxZoom, radiusPx = 68.0)
        val large = PlaceClusterIndex(places, mapMaxZoom = MaxZoom, radiusPx = RadiusPx)

        val smallCount = small.query(EVERYTHING, zoom = 12f).size
        val largeCount = large.query(EVERYTHING, zoom = 12f).size

        assertTrue("半径变大后气泡应该更少：$smallCount → $largeCount", largeCount < smallCount)
    }

    @Test
    fun identical_coordinates_stay_in_the_same_group_even_beyond_map_max_zoom() {
        val index = indexOf(place(1, 113.0, 23.0), place(2, 113.0, 23.0))
        val group = index.query(EVERYTHING, 6f).single() as MapClusterNode.Group
        assertEquals(MaxZoom + 1, index.expansionZoom(group.id))
        assertEquals(listOf(group), index.query(EVERYTHING, MaxZoom.toFloat()))
        assertEquals(listOf(group), index.query(EVERYTHING, (MaxZoom + 1).toFloat()))
    }

    @Test
    fun invalid_places_are_skipped() {
        val index = indexOf(
            place(1, 113.0, 23.0),
            Place(id = 2, name = "没有坐标"),
            Place(name = "没有 id", longitude = 113.0, latitude = 23.0),
        )

        // 无效地点被跳过
        assertEquals(1, index.query(EVERYTHING, zoom = 6f).size)
    }

    @Test
    fun places_outside_viewport_are_excluded() {
        val index = indexOf(place(1, 113.0, 23.0), place(2, 116.4, 39.9))

        val nodes = index.query(
            MapBounds(minLng = 112.0, minLat = 22.0, maxLng = 114.0, maxLat = 24.0),
            zoom = 10f,
        )

        assertEquals(
            listOf(1L),
            nodes.filterIsInstance<MapClusterNode.Single>().map { it.place.id },
        )
    }
    @Test
    fun nearby_points_across_a_grid_boundary_merge() {
        val radius = 68.0
        val cell = radius / 256.0 / 1024.0
        val boundary = cell * 2000
        val index = PlaceClusterIndex(listOf(
            place(1, (boundary - cell * 0.01) * 360 - 180, 0.0),
            place(2, (boundary + cell * 0.01) * 360 - 180, 0.0),
        ), MaxZoom, radius)
        assertEquals(2, (index.query(EVERYTHING, 10f).single() as MapClusterNode.Group).count)
    }

    @Test
    fun diagonal_points_in_one_cell_outside_radius_do_not_merge() {
        val radius = 68.0
        val cell = radius / 256.0 / 1024.0
        val corner = cell * 2000
        fun at(id: Long, fraction: Double): Place {
            val coord = corner + cell * fraction
            val latitude = kotlin.math.atan(kotlin.math.sinh(Math.PI * (1 - 2 * coord))) * 180 / Math.PI
            return place(id, coord * 360 - 180, latitude)
        }
        val index = PlaceClusterIndex(listOf(at(1, 0.02), at(2, 0.98)), MaxZoom, radius)
        assertEquals(2, index.query(EVERYTHING, 10f).size)
    }

    @Test
    fun duplicate_ids_and_non_finite_coordinates_are_excluded() {
        val index = indexOf(place(1, 113.0, 23.0), place(1, 114.0, 24.0),
            place(2, Double.NaN, 23.0), place(3, 113.0, Double.POSITIVE_INFINITY),
            place(4, 181.0, 23.0), place(5, 113.0, 86.0))
        val single = index.query(EVERYTHING, 20f).single() as MapClusterNode.Single
        assertEquals(1L, single.place.id)
        assertEquals(113.0, single.longitude, 0.0)
    }

    @Test
    fun input_order_does_not_change_cluster_membership() {
        val places = provincePlaces()
        val forward = PlaceClusterIndex(places, MaxZoom, RadiusPx)
        val reverse = PlaceClusterIndex(places.reversed(), MaxZoom, RadiusPx)
        for (zoom in 0..MaxZoom) {
            fun signature(index: PlaceClusterIndex) = index.query(EVERYTHING, zoom.toFloat()).map {
                when (it) {
                    is MapClusterNode.Single -> listOf(it.place.id!!)
                    is MapClusterNode.Group -> index.members(it.id).map { place -> place.id!! }
                }
            }.sortedBy { it.joinToString() }
            assertEquals(signature(forward), signature(reverse))
        }
    }

    @Test
    fun each_level_preserves_every_member_once() {
        val places = provincePlaces()
        val index = PlaceClusterIndex(places, MaxZoom, RadiusPx)
        for (zoom in 0..MaxZoom) {
            val ids = index.query(EVERYTHING, zoom.toFloat()).flatMap {
                when (it) {
                    is MapClusterNode.Single -> listOf(it.place.id)
                    is MapClusterNode.Group -> index.members(it.id).map { place -> place.id }
                }
            }
            assertEquals(places.map { it.id }.sortedBy { it }, ids.sortedBy { it })
        }
    }

    @Test
    fun expansion_returns_direct_children_with_weighted_centers() {
        val index = PlaceClusterIndex(provincePlaces(), MaxZoom, RadiusPx)
        val group = index.query(EVERYTHING, 5f).single() as MapClusterNode.Group
        val children = index.children(group.id)
        assertTrue(children.size >= 2)
        assertEquals(children.toSet(), index.query(EVERYTHING, index.expansionZoom(group.id)!!.toFloat()).toSet())
        val members = index.members(group.id)
        assertEquals(members.map { it.longitude!! }.average(), group.longitude, 1e-7)
    }

    @Test
    fun stale_cluster_id_does_not_resolve_in_a_new_index() {
        val places = provincePlaces()
        val old = PlaceClusterIndex(places, MaxZoom, RadiusPx)
        val fresh = PlaceClusterIndex(places, MaxZoom, RadiusPx)
        val group = old.query(EVERYTHING, 5f).single() as MapClusterNode.Group
        org.junit.Assert.assertNull(fresh.expansionZoom(group.id))
        assertTrue(fresh.members(group.id).isEmpty())
    }

    @Test
    fun antimeridian_viewport_includes_both_sides() {
        val index = indexOf(place(1, 179.0, 0.0), place(2, -179.0, 0.0), place(3, 0.0, 0.0))
        val nodes = index.query(MapBounds(170.0, -10.0, -170.0, 10.0), 20f)
        assertEquals(setOf(1L, 2L), nodes.map { (it as MapClusterNode.Single).place.id }.toSet())
    }

    @Test
    fun web_radius_of_68_logical_pixels_splits_at_the_expected_level() {
        val longitudeGap = 67.0 * 360.0 / (256.0 * (1 shl 12))
        val index = PlaceClusterIndex(listOf(place(1, 0.0, 0.0), place(2, longitudeGap, 0.0)),
            MaxZoom, 68.0)
        val group = index.query(EVERYTHING, 12f).single() as MapClusterNode.Group
        assertEquals(13, index.expansionZoom(group.id))
        assertEquals(2, index.query(EVERYTHING, 13f).filterIsInstance<MapClusterNode.Single>().size)
    }

    @Test
    fun points_just_outside_web_radius_do_not_collide_at_the_same_level() {
        val longitudeGap = 69.0 * 360.0 / (256.0 * (1 shl 12))
        val index = PlaceClusterIndex(listOf(place(1, 0.0, 0.0), place(2, longitudeGap, 0.0)),
            MaxZoom, 68.0)
        assertEquals(2, index.query(EVERYTHING, 12f).filterIsInstance<MapClusterNode.Single>().size)
        val group = index.query(EVERYTHING, 11f).single() as MapClusterNode.Group
        assertEquals(12, index.expansionZoom(group.id))
    }

    @Test
    fun fractional_zoom_has_the_same_hysteresis_at_the_map_maximum() {
        assertEquals(10, logicalZoom(10.51f, 10, 19))
        assertEquals(11, logicalZoom(10.66f, 10, 19))
        assertEquals(11, logicalZoom(10.49f, 11, 19))
        assertEquals(10, logicalZoom(10.34f, 11, 19))
        assertEquals(18, logicalZoom(18.6f, 18, 19))
        assertEquals(19, logicalZoom(18.66f, 18, 19))
        assertEquals(19, logicalZoom(20f, 18, 19))
        assertEquals(18, logicalZoom(Float.NaN, 18, 19))
    }

}

private fun place(id: Long, longitude: Double, latitude: Double) =
    Place(id = id, name = "p$id", longitude = longitude, latitude = latitude)

private fun indexOf(vararg places: Place) =
    PlaceClusterIndex(places.toList(), mapMaxZoom = MaxZoom, radiusPx = RadiusPx)

/**
 * 一片铺开约 5km 的地点，外加一对坐标几乎重合的点。
 */
private fun provincePlaces(): List<Place> {
    val places = ArrayList<Place>()
    var id = 1L
    for (i in 0..4) {
        for (j in 0..2) {
            places += place(id++, 113.0 + i * 0.01, 23.0 + j * 0.01)
        }
    }
    places += place(id++, 113.021, 23.011)
    places += place(id, 113.021001, 23.011)
    return places
}

/** 高德地图的缩放级别上限。 */
private const val MaxZoom = 20

/** 测试用的聚合半径（屏幕像素）。 */
private const val RadiusPx = 412.0

/** 包住整个地球的视野。 */
private val EVERYTHING = MapBounds(minLng = -180.0, minLat = -85.0, maxLng = 180.0, maxLat = 85.0)

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
        val index = indexOf(place(1, 113.0000, 23.0000), place(2, 113.0004, 23.0004))
        val group = index.query(EVERYTHING, zoom = 6f)
            .filterIsInstance<MapClusterNode.Group>()
            .single()

        val expansion = index.expansionZoom(group.id)
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
        val expansion = index.expansionZoom(group.id)
        assertTrue("展开级别被拖到了退出聚合那一级：$expansion", expansion < MaxZoom - 1)
    }

    /** 一次点击看到更细的一层，反复点最终全部散成单点。 */
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

            val next = index.expansionZoom(group.id)
            assertTrue("点击必须往里放大：$level → $next", next > level)

            level = next
            val after = index.query(EVERYTHING, level.toFloat()).size
            assertTrue("展开之后应该看到更细的一层：$shown → $after", after > shown)
            shown = after
            clicks++
        }

        assertTrue("反复点击最终应该把所有地点都散开", clicks > 0)
    }

    @Test
    fun distant_places_never_merge() {
        val index = indexOf(place(1, 113.0, 23.0), place(2, 116.4, 39.9))

        val nodes = index.query(EVERYTHING, zoom = 3f)

        assertEquals(2, nodes.count { it is MapClusterNode.Single })
    }

    /** 隔几米的两家店，到退出聚合的级别必须分开。 */
    @Test
    fun neighbouring_places_split_at_the_exit_level() {
        // 常州，东西相差 0.0001°，约 9.5m
        val index = PlaceClusterIndex(
            listOf(place(1, 119.9750, 31.8110), place(2, 119.9751, 31.8110)),
            mapMaxZoom = MaxZoom,
            radiusPx = RadiusPx,
        )

        // 退出聚合的那一级和地图极限都给单点
        listOf(MaxZoom - 1, MaxZoom).forEach { zoom ->
            val nodes = index.query(EVERYTHING, zoom = zoom.toFloat())
            assertEquals(2, nodes.size)
            assertTrue(
                "第 $zoom 级上不应该再有聚合",
                nodes.all { it is MapClusterNode.Single },
            )
        }
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

    /** 坐标完全相同的两个地点，到退出聚合那一级会变成两个重叠在一起的单点。 */
    @Test
    fun identical_coordinates_become_overlapping_singles_at_exit_level() {
        val index = indexOf(place(1, 113.0, 23.0), place(2, 113.0, 23.0))
        val group = index.query(EVERYTHING, zoom = 6f)
            .filterIsInstance<MapClusterNode.Group>()
            .single()

        // 展开级别最多就到退出聚合那一级
        assertEquals(MaxZoom - 1, index.expansionZoom(group.id))
        assertEquals(2, index.query(EVERYTHING, zoom = (MaxZoom - 1).toFloat()).size)
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

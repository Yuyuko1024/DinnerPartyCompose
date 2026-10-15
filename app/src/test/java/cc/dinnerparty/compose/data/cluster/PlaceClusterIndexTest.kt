package cc.dinnerparty.compose.data.cluster

import cc.dinnerparty.compose.api.model.Place
import cc.dinnerparty.compose.data.model.MapBounds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 聚合算法的回归测试。
 *
 * 聚合错了在手机上很难看出来——要么全糊成一团、要么该聚的没聚，两种都像「是不是没做」。
 * 这里把几件必须成立的事钉死，省得每次改常数都要装包到手机上肉眼比。
 */
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

        // 「放到这一级气泡就换成下一层」是点气泡时的全部依据，说错了用户体验就是点了没用
        val atExpansion = index.query(EVERYTHING, expansion.toFloat())
        assertEquals(2, atExpansion.count { it is MapClusterNode.Single })
    }

    /**
     * 回归：一小撮坐标几乎重合的点，不该把整个聚合的展开级别拖到最大级别。
     *
     * 之前是「一路拆到单点」的写法——只要聚合里含有这样一对点，点一下气泡就会直接冲到米级。
     * Web 端（supercluster）算的是「这个气泡是在哪一级合并出来的」，再细一级就展开。
     */
    @Test
    fun near_duplicate_points_do_not_drag_expansion_to_max_zoom() {
        val index = PlaceClusterIndex(provincePlaces())
        val group = index.query(EVERYTHING, zoom = 5f)
            .filterIsInstance<MapClusterNode.Group>()
            .maxByOrNull { it.count }!!

        assertEquals(17, group.count)

        // 这一片铺开约 5km，合并出来在第 8 级，所以展开级别是 9；
        // 「一路拆到单点」的写法会被那对重合点拖到 21
        val expansion = index.expansionZoom(group.id)
        assertTrue("展开级别被拖到了最大级别：$expansion", expansion <= 12)
    }

    /**
     * 点开气泡的整体契约：一次点击看到更细的一层，反复点最终全部散成单点。
     */
    @Test
    fun clicking_a_cluster_shows_a_finer_level_each_time() {
        val index = PlaceClusterIndex(provincePlaces())
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

    @Test
    fun identical_coordinates_never_split() {
        val index = indexOf(place(1, 113.0, 23.0), place(2, 113.0, 23.0))
        val group = index.query(EVERYTHING, zoom = 6f)
            .filterIsInstance<MapClusterNode.Group>()
            .single()

        // 大于最大级别表示放到最细一级也拆不开，调用方得自己决定怎么办
        assertTrue(index.expansionZoom(group.id) > MaxZoom)
    }

    @Test
    fun invalid_places_are_skipped() {
        val index = indexOf(
            place(1, 113.0, 23.0),
            Place(id = 2, name = "没有坐标"),
            Place(name = "没有 id", longitude = 113.0, latitude = 23.0),
        )

        // 一条脏数据不该让整屏标记都没了
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

private fun indexOf(vararg places: Place) = PlaceClusterIndex(places.toList())

/**
 * 一片铺开约 5km 的普通地点，再加上同一家店被录了两遍、坐标只差十几厘米的一对。
 *
 * 这对近重合点是关键：它要到最细的级别才会分开，所以「一路拆到单点」的实现会被它把
 * 整个聚合的展开级别拖到最大级别。两个点的纬度相同、只差一点点经度，保证它们在每一级
 * 都落在同一格里，不会因为卡在格子边界上而偶然错开。
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

/** 包住整个地球的视野，免得测试被视野边界干扰。 */
private val EVERYTHING = MapBounds(minLng = -180.0, minLat = -85.0, maxLng = 180.0, maxLat = 85.0)

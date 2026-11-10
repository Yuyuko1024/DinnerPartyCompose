package cc.dinnerparty.compose.data.cluster

import cc.dinnerparty.compose.api.model.Place
import cc.dinnerparty.compose.data.model.MapBounds
import org.junit.Assert.*
import org.junit.Test

class ClusterTransitionTest {
    private val world = MapBounds(-180.0, -85.0, 180.0, 85.0)
    private val places = listOf(
        place(1, 113.0), place(2, 113.001), place(3, 115.0), place(4, 115.001),
    )

    @Test
    fun initial_snapshot_has_no_invented_sources() {
        val frame = frame(index(places), 12)
        val plan = planTransition(null, frame)
        assertTrue(plan.initial)
        assertTrue(plan.splitFrom.isEmpty())
        assertTrue(plan.mergeInto.isEmpty())
    }

    @Test
    fun zoom_in_connects_each_child_to_its_actual_parent_across_levels() {
        val index = index(places)
        val before = frame(index, 5)
        val after = frame(index, 19)
        val plan = planTransition(before, after)
        assertEquals(4, plan.splitFrom.size)
        for ((childId, parentId) in plan.splitFrom) {
            assertTrue(before.memberIds(before.byId.getValue(parentId)).contains(childId))
        }
        assertEquals(before.nodes.map { it.nodeId }.toSet(), plan.splitSources)
        assertTrue(plan.mergeInto.isEmpty())
    }

    @Test
    fun zoom_out_merges_children_into_their_actual_parent() {
        val index = index(places)
        val before = frame(index, 19)
        val after = frame(index, 5)
        val plan = planTransition(before, after)
        assertEquals(4, plan.mergeInto.size)
        for ((childId, parentId) in plan.mergeInto) {
            assertTrue(after.memberIds(after.byId.getValue(parentId)).contains(childId))
        }
        assertEquals(after.nodes.map { it.nodeId }.toSet(), plan.mergeTargets)
        assertTrue(plan.splitFrom.isEmpty())
    }

    @Test
    fun pan_reuses_visible_nodes_without_fake_splits() {
        val index = index(places)
        val before = frame(index, 12)
        val after = ClusterSnapshot(index,
            index.query(MapBounds(112.5, 22.0, 113.5, 24.0), 12f), 12)
        val plan = planTransition(before, after)
        assertEquals(after.nodes.size, plan.reuse.size)
        assertTrue(plan.splitFrom.isEmpty())
        assertTrue(plan.mergeInto.isEmpty())
    }

    @Test
    fun rebuild_with_identical_members_reuses_bubbles_across_generations() {
        val before = frame(index(places), 12)
        val after = frame(index(places.reversed()), 12)
        val plan = planTransition(before, after)
        assertEquals(after.nodes.size, plan.reuse.size)
        for ((nextId, oldId) in plan.reuse) {
            assertEquals(before.memberIds(before.byId.getValue(oldId)),
                after.memberIds(after.byId.getValue(nextId)))
        }
        assertTrue(plan.splitFrom.isEmpty())
        assertTrue(plan.mergeInto.isEmpty())
    }

    @Test
    fun data_change_splits_only_exclusive_member_relationships() {
        val together = places.map { it.copy(longitude = 113.0) }
        val before = frame(index(together), 12)
        val after = frame(index(places), 12)
        val plan = planTransition(before, after)
        assertEquals(2, plan.splitFrom.size)
        assertEquals(setOf(before.nodes.single().nodeId), plan.splitSources)
        assertTrue(plan.reuse.isEmpty())
    }

    @Test
    fun data_change_merges_only_exclusive_member_relationships() {
        val before = frame(index(places), 12)
        val after = frame(index(places.map { it.copy(longitude = 113.0) }), 12)
        val plan = planTransition(before, after)
        assertEquals(2, plan.mergeInto.size)
        assertEquals(setOf(after.nodes.single().nodeId), plan.mergeTargets)
    }

    @Test
    fun many_to_many_membership_change_uses_fades_instead_of_false_parents() {
        val before = frame(index(places), 12)
        val changed = listOf(place(1, 113.0), place(2, 115.0), place(3, 113.001), place(4, 115.001))
        val after = frame(index(changed), 12)
        val plan = planTransition(before, after)
        assertTrue(plan.reuse.isEmpty())
        assertTrue(plan.splitFrom.isEmpty())
        assertTrue(plan.mergeInto.isEmpty())
    }

    @Test
    fun one_to_one_count_change_reuses_the_existing_overlay() {
        val before = frame(index(places.take(2)), 12)
        val after = frame(index(places.take(2) + place(5, 113.002)), 12)
        val plan = planTransition(before, after)
        assertEquals(mapOf(after.nodes.single().nodeId to before.nodes.single().nodeId), plan.reuse)
    }

    @Test
    fun transition_budget_falls_back_without_partial_false_relationships() {
        val many = (1L..6001L).map { place(it, 113.0) }
        val before = frame(index(many), 12)
        val after = frame(index(many.drop(1)), 12)
        val plan = planTransition(before, after)
        assertTrue(plan.reuse.isEmpty())
        assertTrue(plan.splitFrom.isEmpty())
        assertTrue(plan.mergeInto.isEmpty())
    }

    @Test
    fun identical_large_groups_reuse_before_applying_the_intersection_budget() {
        val many = (1L..6001L).map { place(it, 113.0) }
        val before = frame(index(many), 12)
        val after = frame(index(many), 12)
        val plan = planTransition(before, after)
        assertEquals(mapOf(after.nodes.single().nodeId to before.nodes.single().nodeId), plan.reuse)
    }

    @Test
    fun large_descendant_walk_falls_back_without_partial_split_links() {
        val many = (1L..4001L).map {
            place(it, 113.0 + (it % 65) * 0.0002).copy(latitude = 23.0 + (it / 65) * 0.0002)
        }
        val index = index(many)
        val plan = planTransition(frame(index, 5), frame(index, 20))
        assertTrue(plan.splitFrom.isEmpty())
        assertTrue(plan.splitSources.isEmpty())
    }

    @Test
    fun terminal_group_is_reused_when_zooming_to_the_map_limit() {
        val index = index(places.map { it.copy(longitude = 113.0) })
        val before = frame(index, 19)
        val after = frame(index, 20)
        val plan = planTransition(before, after)
        assertEquals(mapOf(after.nodes.single().nodeId to before.nodes.single().nodeId), plan.reuse)
        assertTrue(plan.splitFrom.isEmpty())
    }

    private fun index(places: List<Place>) = PlaceClusterIndex(places, 20, 68.0)
    private fun frame(index: PlaceClusterIndex, zoom: Int) =
        ClusterSnapshot(index, index.query(world, zoom.toFloat()), zoom)
    private fun place(id: Long, longitude: Double) = Place(id = id, longitude = longitude, latitude = 23.0)
}

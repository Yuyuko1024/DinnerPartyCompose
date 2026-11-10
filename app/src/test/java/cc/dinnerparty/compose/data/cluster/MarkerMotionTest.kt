package cc.dinnerparty.compose.data.cluster

import org.junit.Assert.*
import org.junit.Test

class MarkerMotionTest {
    @Test
    fun split_moves_from_parent_and_reveals_children_with_web_timing() {
        val motion = MarkerMotion(MarkerPhase.Split, 0.7f, 0.35f)
        assertEquals(320L, motion.duration)
        assertEquals(MotionVisual(0f, 0.7f, 0.35f), motion.sample(0f))
        assertEquals(MotionVisual(1f, 1f, 1f), motion.sample(320f))
        assertTrue(motion.sample(80f).progress > 0.25f)
        assertEquals(1f, motion.sample(200f).opacity, 0.001f)
    }

    @Test
    fun merge_converges_then_shrinks_and_fades() {
        val motion = MarkerMotion(MarkerPhase.Merge)
        assertEquals(260L, motion.duration)
        assertEquals(MotionVisual(0f, 1f, 1f), motion.sample(0f))
        assertEquals(1f, motion.sample(50f).opacity, 0f)
        val final = motion.sample(260f)
        assertEquals(1f, final.progress, 0f)
        assertEquals(0.72f, final.scale, 0.0001f)
        assertEquals(0f, final.opacity, 0.0001f)
    }

    @Test
    fun merge_target_waits_110ms_then_overshoots_and_settles_at_320ms() {
        val motion = MarkerMotion(MarkerPhase.MergeTarget)
        assertEquals(0f, motion.sample(109f).opacity, 0f)
        assertEquals(0.82f, motion.sample(110f).scale, 0f)
        assertTrue((111..319).maxOf { motion.sample(it.toFloat()).scale } > 1.05f)
        assertEquals(MotionVisual(1f, 1f, 1f), motion.sample(320f))
    }

    @Test
    fun split_parent_fades_at_170ms_and_unrelated_nodes_use_short_fades() {
        val source = MarkerMotion(MarkerPhase.SplitSource)
        assertEquals(170L, source.duration)
        assertEquals(0f, source.sample(170f).opacity, 0f)
        assertEquals(0.86f, source.sample(170f).scale, 0f)
        assertEquals(160L, MarkerMotion(MarkerPhase.Enter).duration)
        assertEquals(140L, MarkerMotion(MarkerPhase.Exit).duration)
    }

    @Test
    fun retargeted_motion_starts_from_current_scale_and_opacity() {
        val previous = MarkerMotion(MarkerPhase.Split, 0.7f, 0.35f).sample(30f)
        val next = MarkerMotion(MarkerPhase.Reposition, previous.scale, previous.opacity)
        assertEquals(previous.scale, next.sample(0f).scale, 0f)
        assertEquals(previous.opacity, next.sample(0f).opacity, 0f)
        assertEquals(MotionVisual(1f, 1f, 1f), next.sample(next.duration.toFloat()))
    }

    @Test
    fun count_change_can_bounce_while_the_same_overlay_repositions() {
        val motion = MarkerMotion(MarkerPhase.Reposition, recount = true)
        assertEquals(220L, motion.duration)
        assertEquals(0.94f, motion.sample(0f).scale, 0f)
        assertTrue((1..210).maxOf { motion.sample(it.toFloat()).scale } > 1.03f)
        assertEquals(MotionVisual(1f, 1f, 1f), motion.sample(220f))
    }

    @Test
    fun all_phases_keep_finite_values_for_entire_timeline() {
        for (phase in MarkerPhase.entries) {
            val motion = MarkerMotion(phase)
            for (time in -10..400) {
                val visual = motion.sample(time.toFloat())
                assertTrue(visual.progress in 0f..1f)
                assertTrue(visual.opacity in 0f..1f)
                assertTrue(visual.scale.isFinite() && visual.scale > 0f)
            }
        }
    }
}

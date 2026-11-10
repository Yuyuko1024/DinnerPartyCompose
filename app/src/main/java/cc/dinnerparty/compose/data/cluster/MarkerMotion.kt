package cc.dinnerparty.compose.data.cluster

enum class MarkerPhase {
    Split, Merge, SplitSource, MergeTarget, Enter, Exit, Reposition, Recount,
}

data class MotionVisual(val progress: Float, val scale: Float, val opacity: Float)

class MarkerMotion(
    val phase: MarkerPhase,
    private val fromScale: Float = 1f,
    private val fromAlpha: Float = 1f,
    private val recount: Boolean = false,
) {
    val duration: Long = when (phase) {
        MarkerPhase.Split, MarkerPhase.MergeTarget -> 320
        MarkerPhase.Merge -> 260
        MarkerPhase.SplitSource -> 170
        MarkerPhase.Enter -> 160
        MarkerPhase.Exit -> 140
        MarkerPhase.Reposition -> 220
        MarkerPhase.Recount -> 210
    }

    fun sample(elapsed: Float): MotionVisual {
        val fraction = (elapsed / duration).coerceIn(0f, 1f)
        return when (phase) {
            MarkerPhase.Split -> {
                val progress = easeOut(fraction)
                val reveal = (progress / 0.55f).coerceAtMost(1f)
                MotionVisual(progress, lerp(fromScale, 1f, reveal), lerp(fromAlpha, 1f, reveal))
            }
            MarkerPhase.Merge -> {
                val progress = bezier(fraction, 0.4f, 0f, 0.2f, 1f)
                val scale = if (progress <= 0.7f) lerp(fromScale, 0.82f, progress / 0.7f)
                    else lerp(0.82f, 0.72f, (progress - 0.7f) / 0.3f)
                MotionVisual(progress, scale, fromAlpha * (1 - ((progress - 0.7f) / 0.3f).coerceIn(0f, 1f)))
            }
            MarkerPhase.MergeTarget -> {
                val progress = cssOut(((elapsed - 110f) / 210f).coerceIn(0f, 1f))
                val scale = when {
                    progress <= 0.55f -> lerp(0.82f, 1.06f, progress / 0.55f)
                    progress <= 0.8f -> lerp(1.06f, 0.985f, (progress - 0.55f) / 0.25f)
                    else -> lerp(0.985f, 1f, (progress - 0.8f) / 0.2f)
                }
                MotionVisual(1f, scale, (progress / 0.55f).coerceAtMost(1f))
            }
            MarkerPhase.SplitSource -> {
                val progress = cssIn(fraction)
                MotionVisual(0f, lerp(fromScale, 0.86f, progress), fromAlpha * (1 - progress))
            }
            MarkerPhase.Enter -> MotionVisual(1f, 1f, cssOut(fraction))
            MarkerPhase.Exit -> MotionVisual(0f, fromScale, fromAlpha * (1 - cssIn(fraction)))
            MarkerPhase.Reposition -> {
                val reveal = easeOut((elapsed / 160f).coerceIn(0f, 1f))
                val scale = if (recount) recountScale((elapsed / 210f).coerceIn(0f, 1f))
                    else lerp(fromScale, 1f, reveal)
                MotionVisual(easeOut(fraction), scale, lerp(fromAlpha, 1f, reveal))
            }
            MarkerPhase.Recount -> {
                MotionVisual(1f, recountScale(fraction), 1f)
            }
        }
    }
}

private fun recountScale(fraction: Float): Float {
    val progress = easeOut(fraction)
    return if (progress <= 0.6f) lerp(0.94f, 1.04f, progress / 0.6f)
        else lerp(1.04f, 1f, (progress - 0.6f) / 0.4f)
}

private fun easeOut(fraction: Float): Float = bezier(fraction, 0.22f, 0.8f, 0.25f, 1f)
private fun cssOut(fraction: Float): Float = bezier(fraction, 0f, 0f, 0.58f, 1f)
private fun cssIn(fraction: Float): Float = bezier(fraction, 0.42f, 0f, 1f, 1f)
private fun lerp(start: Float, end: Float, fraction: Float): Float = start + (end - start) * fraction

private fun bezier(fraction: Float, timeCtrl1: Float, valueCtrl1: Float,
    timeCtrl2: Float, valueCtrl2: Float): Float {
    if (fraction <= 0f || fraction >= 1f) return fraction
    var lower = 0f
    var upper = 1f
    repeat(14) {
        val middle = (lower + upper) / 2
        if (cubic(middle, timeCtrl1, timeCtrl2) < fraction) lower = middle else upper = middle
    }
    return cubic((lower + upper) / 2, valueCtrl1, valueCtrl2)
}

private fun cubic(fraction: Float, first: Float, second: Float): Float {
    val inverse = 1 - fraction
    return 3 * inverse * inverse * fraction * first +
        3 * inverse * fraction * fraction * second + fraction * fraction * fraction
}

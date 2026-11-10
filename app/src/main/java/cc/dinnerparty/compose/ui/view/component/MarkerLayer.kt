package cc.dinnerparty.compose.ui.view.component

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.os.SystemClock
import android.provider.Settings
import android.view.View
import android.widget.FrameLayout
import cc.dinnerparty.compose.data.cluster.ClusterSnapshot
import cc.dinnerparty.compose.data.cluster.MarkerMotion
import cc.dinnerparty.compose.data.cluster.MarkerPhase
import cc.dinnerparty.compose.data.cluster.planTransition
import com.amap.api.maps.AMap
import com.amap.api.maps.TextureMapView
import com.amap.api.maps.model.CameraPosition
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.Marker
import com.amap.api.maps.model.MarkerOptions
import kotlin.math.min
import kotlin.math.roundToInt

internal data class MarkerLayerUpdate(
    val markers: List<MapMarker>,
    val snapshot: ClusterSnapshot?,
    val selectedId: Long?,
    val renderer: PlaceMarkerRenderer,
)

internal class MarkerLayer(private val map: AMap, private val host: TextureMapView) {
    private var entries = LinkedHashMap<Long, MarkerEntry>()
    private val flights = LinkedHashMap<MarkerEntry, MarkerFlight>()
    private var snapshot: ClusterSnapshot? = null
    private var pending: MarkerLayerUpdate? = null
    private var moving = false
    private var active = true
    private var closed = false
    private var canvasOwns = false
    private var skipAnimation = false
    private var durationScale = 1f
    private val margin = 80f * host.resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val sourceRect = Rect()
    private val targetRect = RectF()
    private val canvasLayer = object : View(host.context) {
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            drawFrame(canvas)
        }
    }
    private val cameraListener = object : AMap.OnCameraChangeListener {
        override fun onCameraChange(position: CameraPosition?) {
            if (!moving) finish()
            moving = true
        }

        override fun onCameraChangeFinish(position: CameraPosition?) {
            moving = false
            val request = pending ?: return
            pending = null
            update(request)
        }
    }

    init {
        canvasLayer.isClickable = false
        canvasLayer.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        host.addView(canvasLayer, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        map.addOnCameraChangeListener(cameraListener)
    }

    fun update(request: MarkerLayerUpdate) {
        if (closed) return
        if (moving || !active) {
            pending = request
            return
        }
        val next = request.snapshot
        val plan = next?.let { planTransition(snapshot, it) }
        val animate = snapshot != null && plan?.initial != true && !skipAnimation &&
            ValueAnimator.areAnimatorsEnabled()
        skipAnimation = false
        durationScale = Settings.Global.getFloat(host.context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE, 1f).coerceAtLeast(0.01f)
        if (!animate) finish()
        val old = LinkedHashMap(entries)
        val oldById = entries.toMap()
        val targets = request.markers.associateBy { it.id }
        val updated = LinkedHashMap<Long, MarkerEntry>()
        var travelers = 0
        for (marker in request.markers) {
            val fromId = plan?.reuse?.get(marker.id) ?: marker.id
            val entry = old.remove(fromId)
            val selected = marker.id == request.selectedId
            if (entry != null) {
                refresh(entry, marker, selected, request.renderer, animate)
                updated[marker.id] = entry
                continue
            }
            val created = create(marker, selected, request.renderer) ?: continue
            updated[marker.id] = created
            val target = project(marker.position)
            if (!animate || !onscreen(target)) {
                reveal(created)
                continue
            }
            val parent = plan?.splitFrom?.get(marker.id)?.let { oldById[it] }
            val origin = parent?.let { visualPoint(it) }
            if (parent != null && origin != null && onscreen(origin) && travelers < MAX_TRAVELERS) {
                travelers++
                val startScale = min(1f, glyphHeight(parent.graphic) / glyphHeight(created.graphic) * 0.7f)
                start(created, MarkerMotion(MarkerPhase.Split, startScale, 0.35f), origin, target)
            } else if (plan?.mergeTargets?.contains(marker.id) == true) {
                start(created, MarkerMotion(MarkerPhase.MergeTarget), target, target)
            } else {
                start(created, MarkerMotion(MarkerPhase.Enter), target, target)
            }
        }
        for ((id, entry) in old) {
            val origin = visualPoint(entry)
            val canGhost = animate && onscreen(origin) && flights.values.count { it.ghost } < MAX_GHOSTS
            entry.overlay.setObject(null)
            entry.overlay.isClickable = false
            entry.overlay.remove()
            if (!canGhost) {
                flights.remove(entry)
                continue
            }
            val target = plan?.mergeInto?.get(id)?.let { targets[it] }?.let { project(it.position) }
            val phase = when {
                target != null && travelers < MAX_TRAVELERS -> MarkerPhase.Merge
                plan?.splitSources?.contains(id) == true -> MarkerPhase.SplitSource
                else -> MarkerPhase.Exit
            }
            if (phase == MarkerPhase.Merge) travelers++
            start(entry, MarkerMotion(phase, entry.scale, entry.opacity), origin,
                if (phase == MarkerPhase.Merge) target!! else origin, ghost = true)
        }
        entries = updated
        if (flights.isNotEmpty()) {
            canvasOwns = true
            entries.values.forEach { it.overlay.isVisible = false }
        } else if (canvasOwns) finish()
        snapshot = next
        canvasLayer.invalidate()
    }

    fun setActive(enabled: Boolean) {
        active = enabled
        if (!enabled) {
            finish()
            skipAnimation = true
        } else {
            val request = pending ?: return
            pending = null
            snapshot = null
            update(request)
        }
    }

    fun finish() {
        flights.clear()
        if (canvasOwns) {
            canvasOwns = false
            entries.values.forEach { it.hideLabel = false; reveal(it) }
        }
        canvasLayer.invalidate()
    }

    fun close() {
        if (closed) return
        closed = true
        flights.clear()
        pending = null
        entries.values.forEach { it.overlay.remove() }
        entries.clear()
        map.removeOnCameraChangeListener(cameraListener)
        host.removeView(canvasLayer)
    }

    private fun create(marker: MapMarker, selected: Boolean,
        renderer: PlaceMarkerRenderer): MarkerEntry? {
        val graphic = renderer.graphic(marker, selected)
        val overlay = map.addMarker(MarkerOptions().position(marker.position)
            .icon(graphic.descriptor).anchor(0.5f, graphic.anchorV)
            .zIndex(markerZ(marker, selected)).isOpenCollisionDetection(false).visible(false)) ?: return null
        overlay.setInfoWindowEnable(false)
        overlay.setObject(marker)
        return MarkerEntry(marker, graphic, overlay).apply {
            screen = project(marker.position)
            order = markerZ(marker, selected)
            this.selected = selected
            this.renderer = renderer
        }
    }

    private fun refresh(entry: MarkerEntry, marker: MapMarker, selected: Boolean,
        renderer: PlaceMarkerRenderer, animate: Boolean) {
        val previous = entry.marker
        val origin = visualPoint(entry)
        val changed = entry.renderer !== renderer || entry.selected != selected ||
            previous.name != marker.name || previous.isAvoided != marker.isAvoided ||
            previous.clusterCount != marker.clusterCount
        if (changed) {
            val graphic = renderer.graphic(marker, selected)
            entry.overlay.setIcon(graphic.descriptor)
            entry.overlay.setAnchor(0.5f, graphic.anchorV)
            entry.graphic = graphic
        }
        entry.selected = selected
        entry.renderer = renderer
        entry.marker = marker
        entry.overlay.setObject(marker)
        entry.order = markerZ(marker, selected)
        entry.overlay.zIndex = entry.order
        val moved = previous.position != marker.position
        val recounted = previous.clusterCount != marker.clusterCount
        val target = project(marker.position)
        if (!animate) {
            flights.remove(entry)
            reveal(entry)
        } else if (moved && (onscreen(origin) || onscreen(target))) {
            start(entry, MarkerMotion(MarkerPhase.Reposition, entry.scale, entry.opacity,
                recount = recounted && entry.opacity >= 0.99f), origin, target)
        } else if (recounted && entry.opacity >= 0.99f && onscreen(target)) {
            start(entry, MarkerMotion(MarkerPhase.Recount), target, target)
        } else if (entry !in flights) {
            reveal(entry)
        }
    }

    private fun start(entry: MarkerEntry, motion: MarkerMotion,
        origin: PointF, target: PointF, ghost: Boolean = false) {
        if (!ghost) entry.overlay.isVisible = false
        entry.hideLabel = true
        val flight = MarkerFlight(motion, origin, target, SystemClock.uptimeMillis(), durationScale, ghost)
        flights[entry] = flight
        sample(entry, flight, flight.startedAt)
    }

    private fun drawFrame(canvas: Canvas) {
        if (closed || !canvasOwns) return
        if (!ValueAnimator.areAnimatorsEnabled()) {
            finish()
            return
        }
        val now = SystemClock.uptimeMillis()
        val running = flights.entries.toList()
        for ((entry, flight) in running) {
            val elapsed = (now - flight.startedAt) / flight.timeScale
            if (elapsed >= flight.motion.duration) {
                flights.remove(entry)
                if (!flight.ghost) reveal(entry)
            } else {
                sample(entry, flight, now)
            }
        }
        if (flights.isEmpty()) {
            finish()
            entries.values.sortedBy { it.order }.forEach { drawEntry(canvas, it, false) }
            canvasLayer.postInvalidateOnAnimation()
            return
        }
        flights.filterValues { it.ghost }.keys.forEach { drawEntry(canvas, it, true) }
        entries.values.sortedBy { it.order }.forEach { entry ->
            if (onscreen(entry.screen)) drawEntry(canvas, entry, entry.hideLabel)
        }
        canvasLayer.postInvalidateOnAnimation()
    }

    private fun drawEntry(canvas: Canvas, entry: MarkerEntry, hideLabel: Boolean) {
        val bitmap = entry.graphic.bitmap
        val glyphHeight = glyphHeight(entry.graphic)
        val height = if (hideLabel) glyphHeight else bitmap.height.toFloat()
        sourceRect.set(0, 0, bitmap.width, height.roundToInt())
        targetRect.set(-bitmap.width / 2f, -glyphHeight, bitmap.width / 2f, height - glyphHeight)
        paint.alpha = (entry.opacity * 255).roundToInt().coerceIn(0, 255)
        canvas.save()
        canvas.translate(entry.screen.x, entry.screen.y)
        canvas.scale(entry.scale, entry.scale)
        canvas.drawBitmap(bitmap, sourceRect, targetRect, paint)
        canvas.restore()
    }

    private fun sample(entry: MarkerEntry, flight: MarkerFlight, now: Long) {
        val visual = flight.motion.sample((now - flight.startedAt) / flight.timeScale)
        entry.screen = PointF(
            flight.origin.x + (flight.target.x - flight.origin.x) * visual.progress,
            flight.origin.y + (flight.target.y - flight.origin.y) * visual.progress)
        entry.scale = visual.scale
        entry.opacity = visual.opacity
    }

    private fun reveal(entry: MarkerEntry) {
        entry.overlay.position = entry.marker.position
        entry.overlay.isVisible = !canvasOwns
        entry.screen = project(entry.marker.position)
        entry.scale = 1f
        entry.opacity = 1f
    }

    private fun project(position: LatLng): PointF {
        val point = map.projection.toScreenLocation(position)
        return PointF(point.x.toFloat(), point.y.toFloat())
    }

    private fun visualPoint(entry: MarkerEntry): PointF {
        val flight = flights[entry] ?: return project(entry.marker.position)
        sample(entry, flight, SystemClock.uptimeMillis())
        return entry.screen
    }

    private fun onscreen(point: PointF): Boolean = point.x > -margin && point.y > -margin &&
        point.x < host.width + margin && point.y < host.height + margin
}

private class MarkerEntry(var marker: MapMarker, var graphic: MarkerGraphic, val overlay: Marker) {
    var screen = PointF()
    var scale = 1f
    var opacity = 1f
    var order = 110f
    var hideLabel = false
    var selected = false
    var renderer: PlaceMarkerRenderer? = null
}

private class MarkerFlight(val motion: MarkerMotion, val origin: PointF, val target: PointF,
    val startedAt: Long, val timeScale: Float, val ghost: Boolean)

private fun glyphHeight(graphic: MarkerGraphic): Float = graphic.bitmap.height * graphic.anchorV
private fun markerZ(marker: MapMarker, selected: Boolean): Float =
    if (selected) 400f else if (marker.clusterCount != null) 120f else 110f

private const val MAX_TRAVELERS = 160
private const val MAX_GHOSTS = 120

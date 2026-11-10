package cc.dinnerparty.compose.data.cluster

import cc.dinnerparty.compose.api.model.Place
import cc.dinnerparty.compose.data.model.MapBounds

class PlaceRangeCache(private val now: () -> Long = System::currentTimeMillis) {
    private val places = LinkedHashMap<Long, Place>()
    private val regions = ArrayList<Region>()

    fun all(): List<Place> = places.values.toList()

    fun needsFetch(bounds: MapBounds): Boolean {
        val timestamp = now()
        val outer = bounds.expanded(0.5)
        val covered = regions.filter { it.bounds.contains(bounds) }
        covered.forEach { it.lastUsed = timestamp }
        return covered.none { timestamp - it.fetchedAt <= CACHE_TTL } ||
            covered.none { it.bounds.contains(outer) }
    }

    fun apply(bounds: MapBounds, incoming: List<Place>) {
        val normalized = normalize(incoming).associateBy { it.id!! }
        places.entries.removeAll { (id, place) ->
            id !in normalized && bounds.contains(place)
        }
        places.putAll(normalized)
        regions.removeAll { bounds.contains(it.bounds) }
        regions += Region(bounds, now(), now())
        while (regions.size > 1 && (regions.size > MAX_REGIONS || places.size > MAX_PLACES)) {
            val oldest = regions.dropLast(1).minBy { it.lastUsed }
            regions.remove(oldest)
            places.entries.removeAll { (_, place) -> regions.none { it.bounds.contains(place) } }
        }
    }

    fun invalidate() {
        regions.forEach { it.fetchedAt = Long.MIN_VALUE / 2 }
    }

    private class Region(val bounds: MapBounds, var fetchedAt: Long, var lastUsed: Long)
}

internal fun MapBounds.expanded(ratio: Double): MapBounds {
    val lngSpan = maxLng - minLng
    val latSpan = maxLat - minLat
    return MapBounds(
        if (minLng > maxLng) -180.0 else (minLng - lngSpan * ratio).coerceAtLeast(-180.0),
        (minLat - latSpan * ratio).coerceAtLeast(-MAX_MAP_LAT),
        if (minLng > maxLng) 180.0 else (maxLng + lngSpan * ratio).coerceAtMost(180.0),
        (maxLat + latSpan * ratio).coerceAtMost(MAX_MAP_LAT),
    )
}

private fun MapBounds.contains(inner: MapBounds): Boolean =
    inner.minLng >= minLng && inner.maxLng <= maxLng &&
        inner.minLat >= minLat && inner.maxLat <= maxLat

private fun MapBounds.contains(place: Place): Boolean {
    val longitude = place.longitude ?: return false
    val latitude = place.latitude ?: return false
    val inLng = if (minLng > maxLng) longitude >= minLng || longitude <= maxLng
        else longitude in minLng..maxLng
    return inLng && latitude in minLat..maxLat
}

private const val CACHE_TTL = 120_000L
private const val MAX_REGIONS = 12
private const val MAX_PLACES = 8000

package cc.dinnerparty.compose.data.repo

import android.content.Context
import cc.dinnerparty.compose.api.ApiClient
import cc.dinnerparty.compose.api.apiCall
import cc.dinnerparty.compose.api.apis.PlaceApi
import cc.dinnerparty.compose.api.model.Place
import cc.dinnerparty.compose.data.model.SearchItem
import com.amap.api.maps.model.LatLng
import com.amap.api.services.core.AMapException
import com.amap.api.services.core.LatLonPoint
import com.amap.api.services.core.PoiItem
import com.amap.api.services.poisearch.PoiResult
import com.amap.api.services.poisearch.PoiSearch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** 搜索结果入口 站内关键词与高德未收录地点 */
class SearchRepo(context: Context) {

    private val appContext = context.applicationContext
    private val placeApi by lazy { ApiClient.createService(PlaceApi::class.java) }

    /** 站内地点搜索 */
    suspend fun searchSite(query: String, center: LatLng?, limit: Int): List<SearchItem> =
        apiCall {
            placeApi.searchPlaces(
                query = query,
                limit = limit,
                lat = center?.latitude,
                lng = center?.longitude,
            )
        }.mapNotNull { place -> place.toSearchItem(center) }

    /** 高德地点搜索 结果会排除站内已有的同名地点 */
    suspend fun searchAmap(
        query: String,
        center: LatLng?,
        known: List<SearchItem>,
    ): List<SearchItem> {
        val point = center ?: return emptyList()
        return searchPoi(query, point).filterNot { result ->
            known.any { it.samePlace(result) }
        }
    }

    private suspend fun searchPoi(query: String, center: LatLng): List<SearchItem> =
        suspendCancellableCoroutine { continuation ->
            val poiQuery = PoiSearch.Query(query, "", "")
            poiQuery.setPageSize(PoiPageSize)
            poiQuery.setPageNum(1)

            val search = try {
                PoiSearch(appContext, poiQuery)
            } catch (error: Exception) {
                continuation.resumeWithException(error)
                return@suspendCancellableCoroutine
            }

            search.setBound(
                PoiSearch.SearchBound(
                    LatLonPoint(center.latitude, center.longitude),
                    PoiRadius,
                )
            )
            search.setOnPoiSearchListener(object : PoiSearch.OnPoiSearchListener {
                override fun onPoiSearched(result: PoiResult?, code: Int) {
                    if (code != AMapException.CODE_AMAP_SUCCESS) {
                        continuation.resumeWithException(
                            IllegalStateException("AMap search failed with code $code")
                        )
                        return
                    }
                    continuation.resume(
                        result?.pois.orEmpty().mapNotNull { it.toSearchItem() }
                    )
                }

                override fun onPoiItemSearched(item: PoiItem?, code: Int) = Unit
            })
            search.searchPOIAsyn()
        }
}

/** 站内地点转搜索结果 */
private fun Place.toSearchItem(center: LatLng?): SearchItem? {
    val lat = latitude ?: return null
    val lng = longitude ?: return null
    val title = name?.takeIf { it.isNotBlank() } ?: return null
    return SearchItem(
        id = "site_${id ?: "$lat,$lng"}",
        placeId = id,
        name = title,
        address = address,
        latitude = lat,
        longitude = lng,
        distanceMeters = distanceKm?.takeIf { it.isFinite() }?.times(1000.0)
            ?: center?.let { metersBetween(it, lat, lng) },
        fromAmap = false,
    )
}

/** 高德地点转搜索结果 */
private fun PoiItem.toSearchItem(): SearchItem? {
    val point = latLonPoint ?: return null
    val title = title?.takeIf { it.isNotBlank() } ?: return null
    val street = snippet?.takeIf { it.isNotBlank() }
    val fallback = listOfNotNull(provinceName, cityName, adName)
        .joinToString("")
        .takeIf { it.isNotBlank() }
    return SearchItem(
        id = "amap_$poiId",
        placeId = null,
        name = title,
        address = street ?: fallback,
        latitude = point.latitude,
        longitude = point.longitude,
        distanceMeters = distance.takeIf { it > 0 }?.toDouble(),
        fromAmap = true,
    )
}

/** 同名且五十米内视为同一地点 */
private fun SearchItem.samePlace(other: SearchItem): Boolean {
    if (normalizeName(name) != normalizeName(other.name)) return false
    val meters = metersBetween(LatLng(latitude, longitude), other.latitude, other.longitude)
    return meters < SamePlaceMeters
}

private fun normalizeName(value: String): String = value.replace(NameNoise, "").lowercase()

private fun metersBetween(from: LatLng, lat: Double, lng: Double): Double {
    val latDelta = Math.toRadians(lat - from.latitude)
    val lngDelta = Math.toRadians(lng - from.longitude)
    val sinLat = sin(latDelta / 2)
    val sinLng = sin(lngDelta / 2)
    val h = sinLat * sinLat +
        cos(Math.toRadians(from.latitude)) * cos(Math.toRadians(lat)) * sinLng * sinLng
    return 2 * EarthRadiusM * asin(min(1.0, sqrt(h)))
}

private val NameNoise = Regex("""[\s（）()]""")

private const val PoiRadius = 20_000
private const val PoiPageSize = 20
private const val SamePlaceMeters = 50.0
private const val EarthRadiusM = 6371008.8

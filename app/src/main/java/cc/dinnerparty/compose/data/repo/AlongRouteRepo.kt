package cc.dinnerparty.compose.data.repo

import android.content.Context
import android.util.Log
import cc.dinnerparty.compose.api.ApiClient
import cc.dinnerparty.compose.api.apiCall
import cc.dinnerparty.compose.api.apis.AlongRouteApi
import cc.dinnerparty.compose.api.model.AlongRouteRequest
import cc.dinnerparty.compose.api.model.Place
import cc.dinnerparty.compose.data.model.AlongRouteMode
import cc.dinnerparty.compose.data.model.AlongRoutePlan
import cc.dinnerparty.compose.data.model.GeoPoint
import cc.dinnerparty.compose.data.model.RouteEndpoint
import com.amap.api.services.core.LatLonPoint
import com.amap.api.services.geocoder.GeocodeSearch
import com.amap.api.services.geocoder.RegeocodeQuery
import com.amap.api.services.help.Inputtips
import com.amap.api.services.help.InputtipsQuery
import com.amap.api.services.route.BusPathV2
import com.amap.api.services.route.DrivePathV2
import com.amap.api.services.route.Path
import com.amap.api.services.route.RidePath
import com.amap.api.services.route.RouteSearchV2
import com.amap.api.services.route.WalkPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 顺路吃的路线规划与沿线检索 */
class AlongRouteRepo(private val context: Context) {

    private val alongRouteApi: AlongRouteApi by lazy {
        ApiClient.createService(AlongRouteApi::class.java)
    }

    /** 按出行方式规划路线 返回高德给出的全部方案 */
    suspend fun plan(
        mode: AlongRouteMode,
        origin: GeoPoint,
        destination: GeoPoint,
    ): List<AlongRoutePlan> = withContext(Dispatchers.IO) {
        Log.d(AlongRouteLogTag, "plan $mode $origin -> $destination")
        val plans = when (mode) {
            AlongRouteMode.Driving -> drivePlans(origin, destination)
            AlongRouteMode.Transit -> transitPlans(origin, destination)
            AlongRouteMode.Walking -> walkPlans(origin, destination)
            AlongRouteMode.Riding -> ridePlans(origin, destination)
        }
        plans.forEach { plan ->
            val points = plan.points.size
            Log.d(AlongRouteLogTag, "plan $mode $plan.index points $points")
        }
        if (plans.isEmpty()) {
            Log.w(AlongRouteLogTag, "plan $mode 无可用路线")
        }
        plans
    }

    /** 沿路线走廊检索站内地点 */
    suspend fun search(
        paths: List<List<GeoPoint>>,
        query: String,
        categories: List<String>,
        corridorMeters: Int,
    ): List<Place> = apiCall {
        val sampled = paths.map { path -> path.sampled().map { listOf(it.longitude, it.latitude) } }
        Log.d(AlongRouteLogTag, "search paths ${sampled.size} points ${sampled.map { it.size }}")
        val places = alongRouteApi.search(
            AlongRouteRequest(
                paths = sampled,
                query = query,
                categories = categories,
                corridorMeters = corridorMeters,
                limit = SearchLimit,
            )
        ).places.orEmpty()
        Log.d(AlongRouteLogTag, "search places ${places.size}")
        places
    }

    /** 输入端点名称时的联想 只保留带坐标的结果 */
    suspend fun suggest(keyword: String): List<RouteEndpoint> = withContext(Dispatchers.IO) {
        val query = InputtipsQuery(keyword, "")
        Inputtips(context, query).requestInputtips().orEmpty().mapNotNull { tip ->
            val point = tip.point ?: return@mapNotNull null
            val name = tip.name?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            RouteEndpoint(GeoPoint(point.latitude, point.longitude), name)
        }.take(TipLimit)
    }

    private fun drivePlans(origin: GeoPoint, destination: GeoPoint): List<AlongRoutePlan> {
        val query = RouteSearchV2.DriveRouteQuery(
            fromAndTo(origin, destination),
            RouteSearchV2.DrivingStrategy.DEFAULT,
            emptyList(),
            emptyList(),
            null,
        )
        query.setUseFerry(true)
        query.setShowFields(RouteSearchV2.ShowFields.POLINE)
        val result = RouteSearchV2(context).calculateDriveRoute(query)
        val paths = result.paths.orEmpty()
        Log.d(AlongRouteLogTag, "drive paths ${paths.size} points ${paths.map { it.polyline?.size }}")
        return paths.mapPlans(::drivePoints)
    }

    private fun walkPlans(origin: GeoPoint, destination: GeoPoint): List<AlongRoutePlan> {
        val query = RouteSearchV2.WalkRouteQuery(fromAndTo(origin, destination))
        query.setShowFields(RouteSearchV2.ShowFields.POLINE)
        val result = RouteSearchV2(context).calculateWalkRoute(query)
        val paths = result.paths.orEmpty()
        Log.d(AlongRouteLogTag, "walk paths ${paths.size} points ${paths.map { it.polyline?.size }}")
        return paths.mapPlans(::walkPoints)
    }

    private fun ridePlans(origin: GeoPoint, destination: GeoPoint): List<AlongRoutePlan> {
        val query = RouteSearchV2.RideRouteQuery(fromAndTo(origin, destination))
        query.setShowFields(RouteSearchV2.ShowFields.POLINE)
        val result = RouteSearchV2(context).calculateRideRoute(query)
        val paths = result.paths.orEmpty()
        Log.d(AlongRouteLogTag, "ride paths ${paths.size} points ${paths.map { it.polyline?.size }}")
        return paths.mapPlans(::ridePoints)
    }

    private fun transitPlans(origin: GeoPoint, destination: GeoPoint): List<AlongRoutePlan> {
        val from = cityOf(origin)
        val to = cityOf(destination)
        Log.d(AlongRouteLogTag, "bus cities $from $to")
        val query = RouteSearchV2.BusRouteQuery(
            fromAndTo(origin, destination),
            RouteSearchV2.BusMode.BUS_DEFAULT,
            from,
            NightBus,
        )
        query.setCityd(to)
        query.setShowFields(RouteSearchV2.ShowFields.POLINE)
        val result = RouteSearchV2(context).calculateBusRoute(query)
        val paths = result.paths.orEmpty()
        Log.d(AlongRouteLogTag, "bus paths ${paths.size} points ${paths.map { it.polyline?.size }}")
        return paths.mapPlans(::busPoints)
    }

    /** 公交方案的城市名 逆地理编码失败直接抛给上层 */
    private fun cityOf(point: GeoPoint): String {
        val query = RegeocodeQuery(
            LatLonPoint(point.latitude, point.longitude),
            GeocodeRadius,
            GeocodeSearch.AMAP,
        )
        val address = GeocodeSearch(context).getFromLocation(query)
        return address.city ?: address.province
            ?: throw IllegalStateException("公交路线缺少城市信息")
    }

    /** 公交路径优先取整条折线 缺失时按步行与公交分段拼接 */
    private fun busPoints(path: BusPathV2): List<GeoPoint> = pointsFrom(path.polyline) {
        path.steps.orEmpty().flatMap { step ->
            val walk = step.walk?.polyline.orEmpty()
            val lines = step.busLines.orEmpty().flatMap { it.polyline.orEmpty() }
            walk + lines
        }
    }
}

private fun drivePoints(path: DrivePathV2): List<GeoPoint> =
    pointsFrom(path.polyline) { path.steps.orEmpty().flatMap { it.polyline.orEmpty() } }

private fun walkPoints(path: WalkPath): List<GeoPoint> =
    pointsFrom(path.polyline) { path.steps.orEmpty().flatMap { it.polyline.orEmpty() } }

private fun ridePoints(path: RidePath): List<GeoPoint> =
    pointsFrom(path.polyline) { path.steps.orEmpty().flatMap { it.polyline.orEmpty() } }

/** 优先取整条折线 为空时用分段拼接的结果 */
private inline fun pointsFrom(
    own: List<LatLonPoint>?,
    steps: () -> List<LatLonPoint>,
): List<GeoPoint> {
    val line = own.orEmpty()
    return (if (line.isEmpty()) steps() else line).toGeoPoints()
}

/** 按抽出方式把高德路径转成方案 索引按结果顺序重排 */
private fun <T : Path> List<T>.mapPlans(pointsOf: (T) -> List<GeoPoint>): List<AlongRoutePlan> {
    val plans = mapNotNull { path ->
        val points = pointsOf(path)
        if (points.size < MinPathPoints) {
            null
        } else {
            AlongRoutePlan(0, points, path.distance, path.duration)
        }
    }
    if (plans.size < size) {
        Log.d(AlongRouteLogTag, "mapPlans dropped ${size - plans.size}")
    }
    return plans.mapIndexed { index, plan -> plan.copy(index = index) }
}

private fun List<LatLonPoint>.toGeoPoints(): List<GeoPoint> =
    map { GeoPoint(it.latitude, it.longitude) }

/** 均匀抽稀到检索接口允许的点数 */
private fun List<GeoPoint>.sampled(): List<GeoPoint> {
    if (size <= MaxSearchPoints) return this
    val step = (size - 1).toDouble() / (MaxSearchPoints - 1)
    return List(MaxSearchPoints) { index ->
        this[Math.round(index * step).toInt().coerceAtMost(size - 1)]
    }
}

private fun fromAndTo(origin: GeoPoint, destination: GeoPoint): RouteSearchV2.FromAndTo =
    RouteSearchV2.FromAndTo(
        LatLonPoint(origin.latitude, origin.longitude),
        LatLonPoint(destination.latitude, destination.longitude),
    )

/** 单条路线至少两个点 */
private const val MinPathPoints = 2

/** 检索接口允许的坐标点数上限 */
private const val MaxSearchPoints = 380

/** 检索返回的地点数量上限 */
private const val SearchLimit = 60

/** 逆地理编码的搜索半径 单位米 */
private const val GeocodeRadius = 1000f

/** 公交查询的夜班标记 1 表示包含夜班车 */
private const val NightBus = 1

/** 顺路吃的日志标签 */
internal const val AlongRouteLogTag = "AlongRoute"

/** 输入提示返回的候选数量上限 */
private const val TipLimit = 8

package cc.dinnerparty.compose.utils

import android.content.Context
import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.location.AMapLocationClientOption.AMapLocationMode
import com.amap.api.maps.model.LatLng

/** 定位失败的原因。 */
enum class LocationFailure {
    /** 没有定位权限。 */
    PERMISSION,

    /** 设备定位服务不可用。 */
    SERVICES_UNAVAILABLE,

    /** 其它失败情况。 */
    UNKNOWN,
}

/** 一次定位请求的结果。 */
sealed interface LocationResult {
    /** 定位成功，坐标是 GCJ-02。 */
    data class Success(val position: LatLng) : LocationResult

    data class Failure(val reason: LocationFailure) : LocationResult
}

/** 一次性定位，拿到一个坐标就停，用完调用 [destroy]。 */
class LocationClient(context: Context) {

    // 只持有 applicationContext
    private val appContext = context.applicationContext

    private var client: AMapLocationClient? = null

    /** 请求一次定位。回调在主线程，且只会被调用一次。 */
    fun requestOnce(onResult: (LocationResult) -> Unit) {
        try {
            val locationClient = client ?: createClient().also { client = it }

            // 拿到结果后摘掉监听
            locationClient.setLocationListener { location ->
                locationClient.stopLocation()
                locationClient.setLocationListener(null)
                onResult(readResult(location))
            }
            locationClient.startLocation()
        } catch (error: Exception) {
            // 构造或启动抛异常时返回失败
            onResult(LocationResult.Failure(LocationFailure.UNKNOWN))
        }
    }

    /** 释放内部连接。 */
    fun destroy() {
        client?.onDestroy()
        client = null
    }

    private fun createClient(): AMapLocationClient {
        val locationClient = AMapLocationClient(appContext)
        locationClient.setLocationOption(
            AMapLocationClientOption().apply {
                // 高精度模式
                setLocationMode(AMapLocationMode.Hight_Accuracy)
                // 只定位一次
                setOnceLocation(true)
                // 取最新位置，忽略缓存
                setOnceLocationLatest(true)
                // 不做逆地理编码
                setNeedAddress(false)
                // 关闭传感器
                setSensorEnable(false)
                // 请求超时时间
                setHttpTimeOut(LOCATION_TIMEOUT_MS)
            }
        )
        return locationClient
    }

    private fun readResult(location: AMapLocation?): LocationResult {
        if (location == null) return LocationResult.Failure(LocationFailure.UNKNOWN)
        if (location.errorCode != 0) {
            return LocationResult.Failure(failureOf(location.errorCode))
        }
        return LocationResult.Success(LatLng(location.latitude, location.longitude))
    }
}

/** 等定位结果的上限。 */
private const val LOCATION_TIMEOUT_MS = 10_000L

/** 把高德的错误码归到界面要区分的那几种。 */
private fun failureOf(errorCode: Int): LocationFailure = when (errorCode) {
    AMapLocation.ERROR_CODE_FAILURE_LOCATION_PERMISSION -> LocationFailure.PERMISSION

    // 无网、飞行模式等均归为服务不可用
    AMapLocation.ERROR_CODE_FAILURE_NOWIFIANDAP,
    AMapLocation.ERROR_CODE_AIRPLANEMODE_WIFIOFF,
    AMapLocation.ERROR_CODE_NOCGI_WIFIOFF,
    AMapLocation.ERROR_CODE_SERVICE_FAIL -> LocationFailure.SERVICES_UNAVAILABLE

    else -> LocationFailure.UNKNOWN
}

package cc.dinnerparty.compose.utils

import android.content.Context
import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.location.AMapLocationClientOption.AMapLocationMode
import com.amap.api.maps.model.LatLng

/**
 * 定位失败的原因。
 *
 * 分得比较粗，因为用户能做的事只有三件：去设置开权限、去设置开定位服务、再点一次。
 * 更细的错误码（卫星数不够、解析失败之类）对用户没有意义，看日志就够了。
 */
enum class LocationFailure {
    /** 没有定位权限。点之前已经申请过，只有中途被收回才会走到这里。 */
    PERMISSION,

    /** 设备侧压根定不了位：定位服务关着、飞行模式、没网也没基站。 */
    SERVICES_UNAVAILABLE,

    /** 其它情况：超时、服务端返回的错等。 */
    UNKNOWN,
}

/** 一次定位请求的结果。 */
sealed interface LocationResult {
    /** 定位成功，坐标是 GCJ-02，和高德地图同一套，可以直接用。 */
    data class Success(val position: LatLng) : LocationResult

    data class Failure(val reason: LocationFailure) : LocationResult
}

/**
 * 一次性定位。
 *
 * 只要「用户现在在哪」这一个点，拿到就停：持续定位会一直唤着 GPS 和网络，而这个应用没有
 * 跟随移动的需求，想刷新位置再点一次定位按钮就是了。
 *
 * 用完必须 [destroy]，否则高德内部那条服务连接不会释放。
 */
class LocationClient(context: Context) {

    // 存 applicationContext：这个对象活得可能比 Activity 久，存 Activity 会漏
    private val appContext = context.applicationContext

    private var client: AMapLocationClient? = null

    /**
     * 请求一次定位。回调在主线程，且只会被调用一次。
     *
     * 高德要求 `startLocation` 在**主线程**调用（内部要用 Looper），所以别挪到 IO 线程去。
     */
    fun requestOnce(onResult: (LocationResult) -> Unit) {
        try {
            val locationClient = client ?: createClient().also { client = it }

            // 一次性定位，所以监听器只管这一次，回来之后就摘掉
            locationClient.setLocationListener { location ->
                locationClient.stopLocation()
                locationClient.setLocationListener(null)
                onResult(readResult(location))
            }
            locationClient.startLocation()
        } catch (error: Exception) {
            // 构造或启动都可能抛：多半是定位权限被撤了，或者隐私声明没做
            onResult(LocationResult.Failure(LocationFailure.UNKNOWN))
        }
    }

    /** 释放内部连接。Activity 销毁时调用，之后这个对象不能再用了。 */
    fun destroy() {
        client?.onDestroy()
        client = null
    }

    private fun createClient(): AMapLocationClient {
        val locationClient = AMapLocationClient(appContext)
        locationClient.setLocationOption(
            AMapLocationClientOption().apply {
                // 街道级精度就够回答「我在哪条街上」，再高只是白白拖长第一次定位的等待
                setLocationMode(AMapLocationMode.Hight_Accuracy)
                // 一次就够，别让它按 interval 反复回调
                setOnceLocation(true)
                // 上一次的缓存位置可能离得老远，宁可多等几秒也要当下这一次
                setOnceLocationLatest(true)
                // 逆地理编码出来的地址这里用不上，少发一次请求就少等一会儿
                setNeedAddress(false)
                // 关掉传感器：结果里那个方向角我们用不到
                setSensorEnable(false)
                // 网络差时高德默认只等 3 秒，城市里偶尔不够，放宽到 10 秒
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

/**
 * 等定位结果的上限。
 *
 * 高德自己的默认值是 3 秒，室内或信号差的地方经常不够，会让用户看到「定位失败」但其实
 * 再等两秒就成了。10 秒是「还愿意等」和「别让人干等」之间的折中。
 */
private const val LOCATION_TIMEOUT_MS = 10_000L

/** 把高德的错误码归到界面要区分的那几种。 */
private fun failureOf(errorCode: Int): LocationFailure = when (errorCode) {
    AMapLocation.ERROR_CODE_FAILURE_LOCATION_PERMISSION -> LocationFailure.PERMISSION

    // 无网且无基站、飞行模式且 Wifi 关闭、无 CGJ 且 Wifi 关闭、连服务都起不来：
    // 归到一类就够了，用户要做的事都是「去把定位服务打开」
    AMapLocation.ERROR_CODE_FAILURE_NOWIFIANDAP,
    AMapLocation.ERROR_CODE_AIRPLANEMODE_WIFIOFF,
    AMapLocation.ERROR_CODE_NOCGI_WIFIOFF,
    AMapLocation.ERROR_CODE_SERVICE_FAIL -> LocationFailure.SERVICES_UNAVAILABLE

    else -> LocationFailure.UNKNOWN
}

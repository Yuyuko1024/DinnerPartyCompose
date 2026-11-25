package cc.dinnerparty.compose.api.apis

import cc.dinnerparty.compose.api.model.AlongRouteRequest
import cc.dinnerparty.compose.api.model.AlongRouteResult
import retrofit2.http.Body
import retrofit2.http.POST

/** 顺路吃接口。 */
interface AlongRouteApi {

    /** 按路线走廊检索站内地点。 */
    @POST("api/along-route/search")
    suspend fun search(@Body body: AlongRouteRequest): AlongRouteResult
}

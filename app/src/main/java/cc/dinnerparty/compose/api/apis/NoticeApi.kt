package cc.dinnerparty.compose.api.apis

import cc.dinnerparty.compose.api.model.NoticeResponse
import retrofit2.http.GET

/** 站点公告。 */
interface NoticeApi {

    /** 当前生效的公告。没有生效公告时响应体是 `{"notice": null}`，不是 404。 */
    @GET("notices/current")
    suspend fun getCurrentNotice(): NoticeResponse
}

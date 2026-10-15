package cc.dinnerparty.compose.data.repo

import cc.dinnerparty.compose.api.ApiClient
import cc.dinnerparty.compose.api.apiCall
import cc.dinnerparty.compose.api.apis.NoticeApi
import cc.dinnerparty.compose.api.model.SiteNotice

/** 站点公告数据入口。 */
class NoticeRepo {

    private val noticeApi: NoticeApi by lazy { ApiClient.createService(NoticeApi::class.java) }

    /** 当前生效的公告，没有则返回 null。 */
    suspend fun getCurrentNotice(): SiteNotice? =
        apiCall { noticeApi.getCurrentNotice() }.notice
}

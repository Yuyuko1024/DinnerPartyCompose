package cc.dinnerparty.compose.api

import java.io.IOException

/**
 * 后端返回的业务错误。
 *
 * 后端所有非 2xx 响应体都是 `{"error": "中文消息"}`，这里把它抬成一个异常，
 * 界面层只读 [serverMessage] 就能直接展示，不用再去解析 HTTP 状态码。
 *
 * @param httpCode HTTP 状态码，401 / 404 / 500 这些在业务上要区别对待。
 * @param serverMessage 后端给的中文文案；响应体不是约定格式时为 null。
 */
class ApiException(
    val httpCode: Int,
    val serverMessage: String?,
) : IOException(serverMessage ?: "HTTP $httpCode")

package cc.dinnerparty.compose.api

import java.io.IOException

/**
 * 后端返回的业务错误，响应体形如 `{"error": "中文消息"}`。
 *
 * @param httpCode HTTP 状态码。
 * @param serverMessage 后端给的中文文案，响应体不是约定格式时为 null。
 */
class ApiException(
    val httpCode: Int,
    val serverMessage: String?,
) : IOException(serverMessage ?: "HTTP $httpCode")

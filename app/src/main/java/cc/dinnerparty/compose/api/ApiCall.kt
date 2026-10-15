package cc.dinnerparty.compose.api

import com.google.gson.JsonParser
import retrofit2.HttpException

/**
 * 把 Retrofit 的 [HttpException] 统一转成带后端文案的 [ApiException]。
 *
 * 只做一层转换、不吞异常：Repo / ViewModel 依旧按普通异常处理，
 * 所以每个接口调用都套一层 `apiCall { ... }` 就能拿到一致的错误模型。
 */
suspend fun <T> apiCall(block: suspend () -> T): T =
    try {
        block()
    } catch (error: HttpException) {
        throw error.toApiException()
    }

private fun HttpException.toApiException(): ApiException {
    // 出错路径拿不到响应体（连接被掐断等）时不该再抛一个新异常，所以整段 runCatching
    val rawBody = runCatching { response()?.errorBody()?.string() }.getOrNull()
    val serverMessage = rawBody
        ?.let { body ->
            runCatching { JsonParser.parseString(body).asJsonObject.get("error")?.asString }
                .getOrNull()
        }
        ?.takeIf { it.isNotBlank() }

    return ApiException(
        httpCode = code(),
        serverMessage = serverMessage ?: rawBody?.takeIf { it.isNotBlank() },
    )
}

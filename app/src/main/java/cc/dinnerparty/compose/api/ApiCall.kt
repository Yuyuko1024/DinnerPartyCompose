package cc.dinnerparty.compose.api

import com.google.gson.JsonParser
import retrofit2.HttpException

suspend fun <T> apiCall(block: suspend () -> T): T =
    try {
        block()
    } catch (error: HttpException) {
        throw error.toApiException()
    }

private fun HttpException.toApiException(): ApiException {
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

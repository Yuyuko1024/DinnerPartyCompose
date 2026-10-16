package cc.dinnerparty.compose.api

import java.util.UUID
import okhttp3.Interceptor
import okhttp3.Response

/** 为每个请求加上 `X-Request-ID` 头。 */
internal class RequestIdInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
            .newBuilder()
            .header("X-Request-ID", UUID.randomUUID().toString())
            .build()
        return chain.proceed(request)
    }
}

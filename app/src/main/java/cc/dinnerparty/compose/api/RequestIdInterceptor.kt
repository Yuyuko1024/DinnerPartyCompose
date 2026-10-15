package cc.dinnerparty.compose.api

import java.util.UUID
import okhttp3.Interceptor
import okhttp3.Response

/**
 * 每个请求带一个 `X-Request-ID`。
 *
 * 后端用它串联一次请求涉及的所有日志并原样回显，用户报问题时凭这个 id 就能直接定位。
 */
internal class RequestIdInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
            .newBuilder()
            .header("X-Request-ID", UUID.randomUUID().toString())
            .build()
        return chain.proceed(request)
    }
}

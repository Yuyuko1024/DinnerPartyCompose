package cc.dinnerparty.compose.api

import okhttp3.Interceptor
import okhttp3.Response

/** 给已登录的请求加上 `Authorization: Bearer <token>` 头。 */
internal class AuthInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = AuthTokenHolder.token ?: return chain.proceed(chain.request())
        val request = chain.request()
            .newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
        return chain.proceed(request)
    }
}

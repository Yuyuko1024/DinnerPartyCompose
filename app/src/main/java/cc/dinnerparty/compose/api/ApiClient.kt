package cc.dinnerparty.compose.api

import cc.dinnerparty.compose.BuildConfig
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.Strictness
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * 全局唯一的 Retrofit 客户端。不需要 Context，所以直接是 `object`。
 *
 * **接口里的路径不要写前导斜杠**：baseUrl 是 `https://www.dinnerparty.cc/backend/`，
 * 写 `@GET("/places")` 会把 `/backend` 整段替换掉，`@GET("places")` 才是往后追加。
 */
object ApiClient {

    private const val BASE_URL = "https://www.dinnerparty.cc/backend/"
    private const val TIMEOUT_SECONDS = 30L

    /**
     * 用 LENIENT 而不是默认严格模式：后端把 JSON 存在 TEXT 列里、库里还有历史脏数据，
     * 严格模式会因为一条坏值让整个列表解析失败。
     */
    private val gson: Gson by lazy {
        GsonBuilder()
            .setStrictness(Strictness.LENIENT)
            .create()
    }

    private val okHttp: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(RequestIdInterceptor())
            .apply {
                // 只在 debug 打印请求/响应体，release 打出来会把 token 之类的写进日志
                if (BuildConfig.DEBUG) {
                    addInterceptor(
                        HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.BODY
                        }
                    )
                }
            }
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttp)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    fun <T> createService(serviceClass: Class<T>): T = retrofit.create(serviceClass)
}

package cc.dinnerparty.compose.api

import cc.dinnerparty.compose.BuildConfig
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.Strictness
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** 全局唯一的 Retrofit 客户端。 */
object ApiClient {

    private const val BASE_URL = "https://www.dinnerparty.cc/backend/"
    private const val TIMEOUT_SECONDS = 30L

    /** 宽容模式解析 JSON。 */
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
                // 仅 debug 打印请求与响应体
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

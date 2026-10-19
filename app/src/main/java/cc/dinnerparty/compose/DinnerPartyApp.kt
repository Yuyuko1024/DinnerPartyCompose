package cc.dinnerparty.compose

import android.app.Application
import cc.dinnerparty.compose.api.AuthTokenHolder
import cc.dinnerparty.compose.data.AuthDataStore
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** 应用入口，启动时恢复本地登录凭证。 */
class DinnerPartyApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // 头像走 OkHttp 下载
        SingletonImageLoader.setSafe { context ->
            ImageLoader.Builder(context)
                .components { add(OkHttpNetworkFetcherFactory()) }
                .build()
        }

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            AuthDataStore(this@DinnerPartyApp).session.first()?.let {
                AuthTokenHolder.token = it.token
            }
        }
    }
}

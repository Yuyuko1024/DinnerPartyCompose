package cc.dinnerparty.compose.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * 同一个文件名的 DataStore 在进程内只能存在一个实例，否则会抛
 * `IllegalStateException: There are multiple DataStores active for the same file`，
 * 所以固定用 application context 取这一个委托实例。
 */
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * 本地设置持久化。
 *
 * 用 AndroidX DataStore 而不是 SharedPreferences：读写本身就是 Flow / 挂起函数，
 * 不需要手动切线程，也不会出现 apply 后立刻读拿到旧值的问题。
 */
class SettingsDataStore(context: Context) {

    private val dataStore = context.applicationContext.settingsDataStore

    /** 用户是否已同意隐私政策。首次启动为 false。 */
    val privacyAgreed: Flow<Boolean> = dataStore.data
        .catch { throwable ->
            // 文件损坏等 IO 异常按「未同意」处理，让用户重新走一遍授权，而不是直接崩掉
            if (throwable is IOException) emit(emptyPreferences()) else throw throwable
        }
        .map { preferences -> preferences[PrivacyAgreedKey] ?: false }

    /** 记录用户已同意隐私政策。 */
    suspend fun agreePrivacy() {
        dataStore.edit { preferences -> preferences[PrivacyAgreedKey] = true }
    }

    private companion object {
        val PrivacyAgreedKey = booleanPreferencesKey("privacy_agreed")
    }
}

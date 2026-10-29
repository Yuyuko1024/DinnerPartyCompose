package cc.dinnerparty.compose.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/** settings 文件的 DataStore 委托。 */
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** 本地设置持久化 */
class SettingsDataStore(context: Context) {

    private val dataStore = context.applicationContext.settingsDataStore

    private val prefs: Flow<Preferences> = dataStore.data
        .catch { throwable ->
            // IO 异常按空设置处理
            if (throwable is IOException) emit(emptyPreferences()) else throw throwable
        }

    /** 用户是否已同意隐私政策 首次启动为 false */
    val privacyAgreed: Flow<Boolean> = prefs
        .map { preferences -> preferences[PrivacyAgreedKey] ?: false }

    /** 是否启用液态玻璃 默认开启 */
    val liquidGlass: Flow<Boolean> = prefs
        .map { preferences -> preferences[LiquidGlassKey] ?: true }

    /** 记录用户已同意隐私政策 */
    suspend fun agreePrivacy() {
        dataStore.edit { preferences -> preferences[PrivacyAgreedKey] = true }
    }

    /** 记录液态玻璃开关 */
    suspend fun setLiquidGlass(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[LiquidGlassKey] = enabled }
    }

    private companion object {
        val PrivacyAgreedKey = booleanPreferencesKey("privacy_agreed")
        val LiquidGlassKey = booleanPreferencesKey("liquid_glass")
    }
}

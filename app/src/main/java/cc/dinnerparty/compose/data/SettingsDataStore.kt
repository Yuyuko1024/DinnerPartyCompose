package cc.dinnerparty.compose.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
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

    /** 亮暗是否跟随系统 默认跟随 */
    val followSystem: Flow<Boolean> = prefs
        .map { preferences -> preferences[FollowSystemKey] ?: true }

    /** 是否强制暗色 仅在未跟随系统时生效 */
    val darkTheme: Flow<Boolean> = prefs
        .map { preferences -> preferences[DarkThemeKey] ?: false }

    /** 主色 ARGB 为空表示使用品牌色 */
    val primary: Flow<Int?> = prefs
        .map { preferences -> preferences[PrimaryKey] }

    /** 辅色 ARGB 为空表示使用品牌色 */
    val secondary: Flow<Int?> = prefs
        .map { preferences -> preferences[SecondaryKey] }

    /** 记录用户已同意隐私政策 */
    suspend fun agreePrivacy() {
        dataStore.edit { preferences -> preferences[PrivacyAgreedKey] = true }
    }

    /** 记录液态玻璃开关 */
    suspend fun setLiquidGlass(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[LiquidGlassKey] = enabled }
    }

    /** 记录亮暗是否跟随系统 */
    suspend fun setFollowSystem(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[FollowSystemKey] = enabled }
    }

    /** 记录强制暗色开关 */
    suspend fun setDarkTheme(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[DarkThemeKey] = enabled }
    }

    /** 记录主色 传空则清除覆盖 */
    suspend fun setPrimary(argb: Int?) {
        dataStore.edit { preferences ->
            if (argb == null) {
                preferences.remove(PrimaryKey)
            } else {
                preferences[PrimaryKey] = argb
            }
        }
    }

    /** 记录辅色 传空则清除覆盖 */
    suspend fun setSecondary(argb: Int?) {
        dataStore.edit { preferences ->
            if (argb == null) {
                preferences.remove(SecondaryKey)
            } else {
                preferences[SecondaryKey] = argb
            }
        }
    }

    private companion object {
        val PrivacyAgreedKey = booleanPreferencesKey("privacy_agreed")
        val LiquidGlassKey = booleanPreferencesKey("liquid_glass")
        val FollowSystemKey = booleanPreferencesKey("follow_system")
        val DarkThemeKey = booleanPreferencesKey("dark_theme")
        val PrimaryKey = intPreferencesKey("theme_primary")
        val SecondaryKey = intPreferencesKey("theme_secondary")
    }
}

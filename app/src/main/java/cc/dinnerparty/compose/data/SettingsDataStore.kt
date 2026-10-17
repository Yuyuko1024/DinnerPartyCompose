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

/** 本地设置持久化。 */
class SettingsDataStore(context: Context) {

    private val dataStore = context.applicationContext.settingsDataStore

    /** 用户是否已同意隐私政策。首次启动为 false。 */
    val privacyAgreed: Flow<Boolean> = dataStore.data
        .catch { throwable ->
            // IO 异常按未同意处理
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

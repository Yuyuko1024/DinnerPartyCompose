package cc.dinnerparty.compose.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import cc.dinnerparty.compose.api.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/** auth 文件的 DataStore 委托。 */
private val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(name = "auth")

/** 登录态：凭证与住民资料。 */
data class AuthSession(
    val token: String,
    val user: User,
    val avatarVersion: Long = 0L,
)

/** 登录态的本地持久化。 */
class AuthDataStore(context: Context) {

    private val dataStore = context.applicationContext.authDataStore

    /** 已保存的登录态，未登录为 null。 */
    val session: Flow<AuthSession?> = dataStore.data
        .catch { throwable ->
            // IO 异常按未登录处理
            if (throwable is IOException) emit(emptyPreferences()) else throw throwable
        }
        .map { preferences ->
            val storedToken = preferences[TokenKey] ?: return@map null
            val token = TokenCipher.decrypt(storedToken) ?: return@map null
            val username = preferences[UsernameKey] ?: return@map null
            AuthSession(
                token = token,
                user = User(
                    id = preferences[UserIdKey],
                    username = username,
                    adminLevel = preferences[AdminLevelKey],
                    hasAvatar = preferences[HasAvatarKey] ?: false,
                ),
                avatarVersion = preferences[AvatarVersionKey] ?: 0L,
            )
        }

    /** 登录或注册成功后保存登录态。 */
    suspend fun save(token: String, user: User) {
        val encryptedToken = TokenCipher.encrypt(token)
        dataStore.edit { preferences ->
            preferences[TokenKey] = encryptedToken
            preferences.putUser(user)
        }
    }

    /** 只更新住民资料，保留凭证。 */
    suspend fun saveUser(user: User) {
        dataStore.edit { preferences ->
            preferences.putUser(user)
        }
    }

    /** 上传头像后保存资料并递增头像版本 */
    suspend fun saveAvatar(user: User) {
        dataStore.edit { preferences ->
            preferences.putUser(user)
            preferences[AvatarVersionKey] = (preferences[AvatarVersionKey] ?: 0L) + 1
        }
    }

    /** 清空登录态 保留头像版本 */
    suspend fun clear() {
        dataStore.edit { preferences ->
            val avatarVersion = preferences[AvatarVersionKey]
            preferences.clear()
            avatarVersion?.let { preferences[AvatarVersionKey] = it }
        }
    }

    private fun MutablePreferences.putUser(user: User) {
        this[UserIdKey] = user.id.orEmpty()
        this[UsernameKey] = user.username.orEmpty()
        user.adminLevel
            ?.let { this[AdminLevelKey] = it }
            ?: remove(AdminLevelKey)
        this[HasAvatarKey] = user.hasAvatar
    }

    private companion object {
        val TokenKey = stringPreferencesKey("token")
        val UserIdKey = stringPreferencesKey("user_id")
        val UsernameKey = stringPreferencesKey("username")
        val AdminLevelKey = stringPreferencesKey("admin_level")
        val HasAvatarKey = booleanPreferencesKey("has_avatar")
        val AvatarVersionKey = longPreferencesKey("avatar_version")
    }
}

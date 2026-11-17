package cc.dinnerparty.compose.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import cc.dinnerparty.compose.data.model.MapCamera
import cc.dinnerparty.compose.data.model.MapDefaults
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
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

    /** 是否启用液态玻璃 默认关闭 */
    val liquidGlass: Flow<Boolean> = prefs
        .map { preferences -> preferences[LiquidGlassKey] ?: false }

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

    /** 默认中心纬度 */
    val centerLat: Flow<Double> = prefs
        .map { preferences -> preferences[CenterLatKey] ?: MapDefaults.Latitude }

    /** 默认中心经度 */
    val centerLng: Flow<Double> = prefs
        .map { preferences -> preferences[CenterLngKey] ?: MapDefaults.Longitude }

    /** 默认缩放等级 */
    val mapZoom: Flow<Float> = prefs
        .map { preferences -> preferences[MapZoomKey] ?: MapDefaults.Zoom }

    /** 聚合标记动效 默认开启 */
    val clusterMotion: Flow<Boolean> = prefs
        .map { preferences -> preferences[ClusterMotionKey] ?: true }

    /** 弹窗与页面动效 默认开启 */
    val overlayMotion: Flow<Boolean> = prefs
        .map { preferences -> preferences[OverlayMotionKey] ?: true }

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

    /** 记录默认视角 三项一起写 */
    suspend fun setMapView(camera: MapCamera) {
        dataStore.edit { preferences ->
            preferences[CenterLatKey] = camera.latitude
            preferences[CenterLngKey] = camera.longitude
            preferences[MapZoomKey] = camera.zoom
        }
    }

    /** 记录聚合标记动效开关 */
    suspend fun setClusterMotion(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[ClusterMotionKey] = enabled }
    }

    /** 记录弹窗与页面动效开关 */
    suspend fun setOverlayMotion(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[OverlayMotionKey] = enabled }
    }

    /** 一次性读取已保存的视角 */
    suspend fun mapCamera(): MapCamera {
        val values = prefs.first()
        return MapCamera(
            latitude = values[CenterLatKey] ?: MapDefaults.Latitude,
            longitude = values[CenterLngKey] ?: MapDefaults.Longitude,
            zoom = values[MapZoomKey] ?: MapDefaults.Zoom,
        )
    }

    private companion object {
        val PrivacyAgreedKey = booleanPreferencesKey("privacy_agreed")
        val LiquidGlassKey = booleanPreferencesKey("liquid_glass")
        val FollowSystemKey = booleanPreferencesKey("follow_system")
        val DarkThemeKey = booleanPreferencesKey("dark_theme")
        val PrimaryKey = intPreferencesKey("theme_primary")
        val SecondaryKey = intPreferencesKey("theme_secondary")
        val CenterLatKey = doublePreferencesKey("center_lat")
        val CenterLngKey = doublePreferencesKey("center_lng")
        val MapZoomKey = floatPreferencesKey("map_zoom")
        val ClusterMotionKey = booleanPreferencesKey("cluster_motion")
        val OverlayMotionKey = booleanPreferencesKey("overlay_motion")
    }
}

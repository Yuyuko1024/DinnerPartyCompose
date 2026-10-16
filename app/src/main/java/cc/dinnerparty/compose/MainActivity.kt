package cc.dinnerparty.compose

import android.os.Bundle
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import cc.dinnerparty.compose.data.SettingsDataStore
import cc.dinnerparty.compose.ui.screen.PrivacyConsentDialog
import cc.dinnerparty.compose.ui.theme.DinnerPartyTheme
import cc.dinnerparty.compose.ui.view.MainView
import cc.dinnerparty.compose.ui.view.component.agreeAmapPrivacy
import com.moriafly.salt.ui.UnstableSaltUiApi
import kotlinx.coroutines.launch

@UnstableSaltUiApi
class MainActivity : ComponentActivity() {

    private val settings by lazy { SettingsDataStore(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            DinnerPartyTheme {
                StatusBarAppearance(window)
                AppRoot(
                    settings = settings,
                    onDeclinePrivacy = { finish() }
                )
            }
        }
    }
}

/**
 * 状态栏前景（图标、时间那一行）跟地图反着走：地图切夜视图时用浅色图标，浅色地图时用深色图标。
 *
 * 背景不动，始终是 edge-to-edge 透出底下的地图，所以能调的只有前景亮暗这一个属性。
 * 这里显式设置而不是吃 `enableEdgeToEdge()` 的默认值，是为了把「跟地图反着来」这条规则收在一处：
 * 将来地图若有了自己的明暗开关（比如可手动切样式），只改这里一处就够。
 */
@Composable
private fun StatusBarAppearance(window: Window) {
    val isDarkTheme = isSystemInDarkTheme()
    LaunchedEffect(isDarkTheme) {
        WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightStatusBars = !isDarkTheme
    }
}

/**
 * 应用入口门禁：先从本地读出隐私政策同意状态，同意之后才进地图。
 *
 * 高德要求隐私合规声明早于任何地图对象创建，而「用户同意」是应用层的决策、
 * 不该由地图组件自己判断，所以声明放在这里。
 */
@UnstableSaltUiApi
@Composable
private fun AppRoot(
    settings: SettingsDataStore,
    onDeclinePrivacy: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // null = 还没从磁盘读出来。先什么都不渲染，避免首帧闪一下隐私弹框。
    val privacyAgreed: Boolean? by settings.privacyAgreed.collectAsState(initial = null)

    when (privacyAgreed) {
        null -> Unit

        false -> PrivacyConsentDialog(
            onAgree = { scope.launch { settings.agreePrivacy() } },
            onDecline = onDeclinePrivacy
        )

        else -> {
            // 已同意的老用户直接进地图，但要先补高德隐私声明；重复调用无副作用。
            remember { agreeAmapPrivacy(context) }
            MainView()
        }
    }
}
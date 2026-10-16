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

/** 状态栏图标亮暗跟随系统主题。 */
@Composable
private fun StatusBarAppearance(window: Window) {
    val isDarkTheme = isSystemInDarkTheme()
    LaunchedEffect(isDarkTheme) {
        WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightStatusBars = !isDarkTheme
    }
}

/** 读取本地隐私政策同意状态，同意之后进入地图。 */
@UnstableSaltUiApi
@Composable
private fun AppRoot(
    settings: SettingsDataStore,
    onDeclinePrivacy: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // 读出结果之前为 null
    val privacyAgreed: Boolean? by settings.privacyAgreed.collectAsState(initial = null)

    when (privacyAgreed) {
        null -> Unit

        false -> PrivacyConsentDialog(
            onAgree = { scope.launch { settings.agreePrivacy() } },
            onDecline = onDeclinePrivacy
        )

        else -> {
            // 进入地图前补一次高德隐私声明
            remember { agreeAmapPrivacy(context) }
            MainView()
        }
    }
}
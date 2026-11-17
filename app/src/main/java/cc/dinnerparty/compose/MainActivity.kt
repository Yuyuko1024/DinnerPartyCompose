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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cc.dinnerparty.compose.data.SettingsDataStore
import cc.dinnerparty.compose.ui.view.component.PrivacyConsentDialog
import cc.dinnerparty.compose.ui.theme.DinnerPartyTheme
import cc.dinnerparty.compose.ui.view.MainView
import cc.dinnerparty.compose.ui.view.component.agreeAmapPrivacy
import cc.dinnerparty.compose.ui.viewmodel.SettingsViewModel
import com.moriafly.salt.ui.UnstableSaltUiApi
import kotlinx.coroutines.launch

@UnstableSaltUiApi
class MainActivity : ComponentActivity() {

    private val settings by lazy { SettingsDataStore(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            val settingsVm: SettingsViewModel = viewModel()
            val settingsState by settingsVm.uiState.collectAsStateWithLifecycle()
            val darkTheme = if (settingsState.followSystem) {
                isSystemInDarkTheme()
            } else {
                settingsState.darkTheme
            }
            val primary = settingsState.primary?.let { argb -> Color(argb) }
            val secondary = settingsState.secondary?.let { argb -> Color(argb) }
            DinnerPartyTheme(
                isDarkTheme = darkTheme,
                primary = primary,
                secondary = secondary
            ) {
                StatusBarAppearance(window = window, isDark = darkTheme)
                AppRoot(
                    settings = settings,
                    onDeclinePrivacy = { finish() }
                )
            }
        }
    }
}

/** 状态栏图标亮暗与当前主题一致。 */
@Composable
private fun StatusBarAppearance(window: Window, isDark: Boolean) {
    LaunchedEffect(isDark) {
        WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightStatusBars = !isDark
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
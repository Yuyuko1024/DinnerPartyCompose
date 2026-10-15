package cc.dinnerparty.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
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
                AppRoot(
                    settings = settings,
                    onDeclinePrivacy = { finish() }
                )
            }
        }
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
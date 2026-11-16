package cc.dinnerparty.compose.ui.nav

import androidx.activity.compose.BackHandler
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import cc.dinnerparty.compose.ui.page.AvatarPage
import cc.dinnerparty.compose.ui.page.SettingsPage
import com.moriafly.salt.ui.UnstableSaltUiApi

/** 页面转场时长 */
private const val PageMs = 260

/** 全屏页面宿主 栈底是不绘制也不接收触摸的地图路由 */
@UnstableSaltUiApi
@Composable
fun AppNavHost(
    nav: NavHostController,
    pages: AppPages,
    modifier: Modifier = Modifier,
) {
    // 非预测性返回回调 让转场按固定时长跑满
    val pop: () -> Unit = { nav.popBackStack() }

    NavHost(
        navController = nav,
        startDestination = AppRoutes.Map,
        modifier = modifier.fillMaxSize(),
        enterTransition = {
            slideInHorizontally(tween(PageMs)) { it } + fadeIn(tween(PageMs))
        },
        exitTransition = {
            slideOutHorizontally(tween(PageMs)) { -it } + fadeOut(tween(PageMs))
        },
        popEnterTransition = {
            slideInHorizontally(tween(PageMs)) { -it } + fadeIn(tween(PageMs))
        },
        popExitTransition = {
            slideOutHorizontally(tween(PageMs)) { it } + fadeOut(tween(PageMs))
        },
    ) {
        composable(
            route = AppRoutes.Map,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None },
        ) {
            Box(modifier = Modifier.fillMaxSize())
        }

        composable(route = AppRoutes.Settings) {
            BackHandler { pop() }
            SettingsPage(ui = pages.settings, ops = pages.settingsOps)
        }

        composable(route = AppRoutes.Avatar) {
            BackHandler { pop() }
            AvatarPage(ui = pages.avatar, ops = pages.avatarOps)
        }
    }
}

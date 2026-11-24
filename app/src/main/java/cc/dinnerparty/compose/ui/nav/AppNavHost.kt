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
import cc.dinnerparty.compose.ui.view.component.LocalMotion
import cc.dinnerparty.compose.ui.view.screen.AvatarPage
import cc.dinnerparty.compose.ui.view.screen.MapSettingsPage
import cc.dinnerparty.compose.ui.view.screen.PasswordPage
import cc.dinnerparty.compose.ui.view.screen.ProfilePage
import cc.dinnerparty.compose.ui.view.screen.SettingsPage
import cc.dinnerparty.compose.ui.view.screen.SkinPage
import cc.dinnerparty.compose.ui.view.screen.ThemePage
import cc.dinnerparty.compose.ui.view.screen.UsernamePage
import com.moriafly.salt.ui.UnstableSaltUiApi

/** 页面转场时长 */
private const val PageMs = 260

/** 全屏页面宿主 栈底是不绘制也不接收触摸的地图路由 */
@UnstableSaltUiApi
@Composable
fun AppNavHost(
    nav: NavHostController,
    data: AppPageData,
    ops: AppPageOps,
    modifier: Modifier = Modifier,
) {
    // 非预测性返回回调 让转场按固定时长跑满
    val pop: () -> Unit = { nav.popBackStack() }

    // 关闭动效时页面只淡入淡出
    val motion = LocalMotion.current

    NavHost(
        navController = nav,
        startDestination = AppRoutes.Map,
        modifier = modifier.fillMaxSize(),
        enterTransition = {
            if (motion) {
                slideInHorizontally(tween(PageMs)) { it } + fadeIn(tween(PageMs))
            } else {
                fadeIn(tween(PageMs))
            }
        },
        exitTransition = {
            if (motion) {
                slideOutHorizontally(tween(PageMs)) { -it } + fadeOut(tween(PageMs))
            } else {
                fadeOut(tween(PageMs))
            }
        },
        popEnterTransition = {
            if (motion) {
                slideInHorizontally(tween(PageMs)) { -it } + fadeIn(tween(PageMs))
            } else {
                fadeIn(tween(PageMs))
            }
        },
        popExitTransition = {
            if (motion) {
                slideOutHorizontally(tween(PageMs)) { it } + fadeOut(tween(PageMs))
            } else {
                fadeOut(tween(PageMs))
            }
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
            SettingsPage(ui = data.settings, ops = ops.settings)
        }

        composable(route = AppRoutes.Theme) {
            BackHandler { pop() }
            ThemePage(ui = data.theme, ops = ops.theme)
        }

        composable(route = AppRoutes.Skin) {
            BackHandler { pop() }
            SkinPage(ops = ops.skin)
        }

        composable(route = AppRoutes.MapSettings) {
            BackHandler { pop() }
            MapSettingsPage(ui = data.mapSettings, ops = ops.mapSettings)
        }

        composable(route = AppRoutes.Avatar) {
            BackHandler { pop() }
            AvatarPage(ui = data.avatar, ops = ops.avatar)
        }

        composable(route = AppRoutes.Username) {
            BackHandler { pop() }
            UsernamePage(ui = data.username, ops = ops.username)
        }

        composable(route = AppRoutes.Password) {
            BackHandler { pop() }
            PasswordPage(ops = ops.password)
        }

        composable(route = AppRoutes.Profile) {
            BackHandler { pop() }
            ProfilePage(ui = data.profile, ops = ops.profile)
        }
    }
}

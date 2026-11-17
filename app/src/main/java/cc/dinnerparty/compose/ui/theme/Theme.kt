package cc.dinnerparty.compose.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.moriafly.salt.ui.SaltConfigs
import com.moriafly.salt.ui.SaltDynamicColors
import com.moriafly.salt.ui.SaltTheme

/** 当前实际生效的亮暗 由设置决定 */
internal val LocalDarkTheme = staticCompositionLocalOf { false }

/** 当前生效的主题色 由设置决定 */
internal val LocalAccentColors = staticCompositionLocalOf {
    accentColors(dark = false, primary = null, secondary = null)
}

/** App 主题入口 基于 SaltUI 提供颜色与排版令牌 */
@Composable
fun DinnerPartyTheme(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    primary: Color? = null,
    secondary: Color? = null,
    content: @Composable () -> Unit
) {
    val light = remember(primary) { lightColors(primary) }
    val dark = remember(primary) { darkColors(primary) }
    val accents = remember(isDarkTheme, primary, secondary) {
        accentColors(isDarkTheme, primary, secondary)
    }

    CompositionLocalProvider(
        LocalDarkTheme provides isDarkTheme,
        LocalAccentColors provides accents
    ) {
        SaltTheme(
            configs = SaltConfigs.default(
                isDarkTheme = isDarkTheme,
                indication = ripple()
            ),
            dynamicColors = SaltDynamicColors(
                light = light,
                dark = dark
            ),
            content = content
        )
    }
}

/** 当前亮暗下的一套语义色 供评价档位与避雷标记使用 */
@Composable
@ReadOnlyComposable
fun dinnerPartySemanticColors(): DinnerPartySemanticColors =
    if (LocalDarkTheme.current) DinnerPartyDarkSemanticColors else DinnerPartyLightSemanticColors

/** 当前亮暗下的主题色 主色用于主要按钮与选中态 辅色用于标签与强调图标 */
@Composable
@ReadOnlyComposable
fun dinnerPartyAccents(): DinnerPartyAccentColors = LocalAccentColors.current

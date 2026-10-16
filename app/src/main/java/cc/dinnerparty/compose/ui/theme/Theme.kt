package cc.dinnerparty.compose.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.moriafly.salt.ui.SaltConfigs
import com.moriafly.salt.ui.SaltDynamicColors
import com.moriafly.salt.ui.SaltTheme

/** App 主题入口，基于 SaltUI，提供颜色与排版令牌。 */
@Composable
fun DinnerPartyTheme(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    SaltTheme(
        configs = SaltConfigs.default(
            isDarkTheme = isDarkTheme,
            indication = ripple()
        ),
        dynamicColors = SaltDynamicColors(
            light = DinnerPartyLightColors,
            dark = DinnerPartyDarkColors
        ),
        content = content
    )
}

/** 当前亮暗下的一套语义色，供评价档位与避雷标记使用。 */
@Composable
@ReadOnlyComposable
fun dinnerPartySemanticColors(): DinnerPartySemanticColors =
    if (isSystemInDarkTheme()) DinnerPartyDarkSemanticColors else DinnerPartyLightSemanticColors

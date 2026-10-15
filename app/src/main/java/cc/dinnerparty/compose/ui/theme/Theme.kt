package cc.dinnerparty.compose.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.moriafly.salt.ui.SaltConfigs
import com.moriafly.salt.ui.SaltDynamicColors
import com.moriafly.salt.ui.SaltTheme

/**
 * App 主题入口，基于 SaltUI。
 *
 * 系统栏由 `MainActivity` 的 `enableEdgeToEdge()` 负责，这里只提供颜色与排版令牌。
 *
 * 说明：目前未启用 SaltUI 的玻璃材质（`SaltMaterial`），因为地图尚未接入，
 * 没有 [com.moriafly.salt.ui.material.MaterialSource] 可模糊的对象。
 * M0 接入高德 `TextureMapView` 后，改用带 `material` 参数的重载并传入
 * `SaltMaterial.default(type = MaterialType.BlurryGlass)`，
 * 再把各处 `background(...)` 换成 `Modifier.material(...)` 即可得到玻璃效果。
 */
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

/**
 * 当前亮暗下的一套语义色（评价档位、避雷标记要用）。
 *
 * 和 [DinnerPartyTheme] 一样按系统亮暗取色——App 到目前只有「跟随系统」这一种主题策略，
 * 没有手动切换，所以不需要额外下发。
 */
@Composable
@ReadOnlyComposable
fun dinnerPartySemanticColors(): DinnerPartySemanticColors =
    if (isSystemInDarkTheme()) DinnerPartyDarkSemanticColors else DinnerPartyLightSemanticColors

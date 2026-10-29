@file:OptIn(ExperimentalHazeApi::class)

package cc.dinnerparty.compose.ui.view.component

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import com.moriafly.salt.ui.SaltTheme
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.glass.GlassStyle
import dev.chrisbanes.haze.glass.hazeGlass

/** 地图浮层玻璃的采样源 */
internal val LocalMapHaze = staticCompositionLocalOf<HazeState?> { null }

/** 是否启用玻璃 关闭时用 SaltUI 默认主题色 */
internal val LocalLiquidGlass = compositionLocalOf { false }

/** 液态玻璃需要的最低系统版本 */
private const val GlassMinSdk = 33

/** 当前系统是否支持液态玻璃 */
internal fun isGlassOn(): Boolean = Build.VERSION.SDK_INT >= GlassMinSdk

/** 玻璃底色不透明度 */
private const val GlassAlpha = 0.55f

/** 面板浮层玻璃 模糊较重 适合承载文字 */
@Composable
fun Modifier.mapGlass(outline: Shape): Modifier {
    val base = SaltTheme.colors.background
    val style = remember(base) {
        GlassStyle.regular.then { backgroundColor(base.copy(alpha = GlassAlpha)) }
    }
    return glass(outline, base, style)
}

/** 小控件玻璃 保留背景可见 */
@Composable
fun Modifier.clearGlass(outline: Shape): Modifier {
    val base = SaltTheme.colors.subBackground
    val style = remember(base) {
        GlassStyle.clear.then { backgroundColor(base.copy(alpha = GlassAlpha)) }
    }
    return glass(outline, base, style)
}

/** 弹框玻璃 底色取自弹层容器色 */
@Composable
fun Modifier.dialogGlass(outline: RoundedCornerShape): Modifier {
    val base = SaltTheme.colors.popup
    val style = remember(base) {
        GlassStyle.regular.then { backgroundColor(base.copy(alpha = GlassAlpha)) }
    }
    return glass(outline, base, style)
}

@Composable
private fun Modifier.glass(outline: Shape, base: Color, style: GlassStyle): Modifier {
    val haze = LocalMapHaze.current
    val clipped = clip(outline)
    if (haze == null || !LocalLiquidGlass.current) return clipped.background(base)
    if (!isGlassOn()) return clipped.hazeBlur(input = HazeInput.Sources(haze))
    val round = outline as RoundedCornerShape
    return clipped.hazeGlass(
        input = HazeInput.Sources(haze),
        style = style.then { shape(round) },
    )
}

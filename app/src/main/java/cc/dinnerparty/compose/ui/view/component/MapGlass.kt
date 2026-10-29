@file:OptIn(ExperimentalHazeApi::class)

package cc.dinnerparty.compose.ui.view.component

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
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
import dev.chrisbanes.haze.glass.GlassStyle
import dev.chrisbanes.haze.glass.hazeGlass

/** 地图浮层玻璃的采样源 */
internal val LocalMapHaze = staticCompositionLocalOf<HazeState?> { null }

/** 面板浮层玻璃 模糊较重 适合承载文字 */
@Composable
fun Modifier.mapGlass(outline: Shape): Modifier {
    val base = SaltTheme.colors.background
    val style = remember(base) {
        GlassStyle.regular.then { backgroundColor(base.copy(alpha = glassAlpha())) }
    }
    return glass(outline, base, style)
}

/** 小控件玻璃 保留背景可见 */
@Composable
fun Modifier.clearGlass(outline: Shape): Modifier {
    val base = SaltTheme.colors.subBackground
    val style = remember(base) {
        GlassStyle.clear.then { backgroundColor(base.copy(alpha = glassAlpha())) }
    }
    return glass(outline, base, style)
}

/** 弹框玻璃 底色取自弹层容器色 */
@Composable
fun Modifier.dialogGlass(outline: RoundedCornerShape): Modifier {
    val base = SaltTheme.colors.popup
    val style = remember(base) {
        GlassStyle.regular.then { backgroundColor(base.copy(alpha = glassAlpha())) }
    }
    return glass(outline, base, style)
}

@Composable
private fun Modifier.glass(outline: Shape, base: Color, style: GlassStyle): Modifier {
    val haze = LocalMapHaze.current
    if (haze == null) return background(base).clip(outline)
    val round = outline as RoundedCornerShape
    return hazeGlass(
        input = HazeInput.Sources(haze),
        style = style.then { shape(round) },
    ).clip(outline)
}

/** 玻璃底色不透明度 低于 Android 13 无折射与模糊 需要更实的底色 */
private fun glassAlpha(): Float = if (Build.VERSION.SDK_INT >= 33) 0.55f else 0.94f

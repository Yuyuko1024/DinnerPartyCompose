package cc.dinnerparty.compose.ui.adaptive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp

/** 手机与平板的宽度分界。 */
private val MediumWidth = 600.dp

/** 平板与大屏的宽度分界。 */
private val ExpandedWidth = 840.dp

/** 窗口可用宽度的档位，断点对齐 Material 3。 */
enum class WindowSizeClass {
    /** 宽度不足 600dp，竖屏手机、分屏下的平板都算这一档。 */
    Compact,

    /** 宽度 600dp 至 840dp，竖屏平板。 */
    Medium,

    /** 宽度 840dp 以上，横屏平板。 */
    Expanded
}

/** 当前是否按平板布局渲染，默认不是。 */
val LocalIsTablet = compositionLocalOf { false }

/** 当前窗口的档位，窗口尺寸变化时自动重组。 */
@Composable
fun currentWindowSizeClass(): WindowSizeClass {
    val width = LocalWindowInfo.current.containerDpSize.width
    return when {
        width < MediumWidth -> WindowSizeClass.Compact
        width < ExpandedWidth -> WindowSizeClass.Medium
        else -> WindowSizeClass.Expanded
    }
}

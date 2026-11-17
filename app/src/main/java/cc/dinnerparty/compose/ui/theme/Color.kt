package cc.dinnerparty.compose.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import com.moriafly.salt.ui.SaltColors
import com.moriafly.salt.ui.darkSaltColors
import com.moriafly.salt.ui.lightSaltColors

/**
 * 品牌色板
 */

/** 亮色：`--color-bg-base #F9F8FA` / `--color-primary #E2789F`。 */
val DinnerPartyLightColors = lightSaltColors(
    highlight = Color(0xFFE2789F),
    text = Color(0xFF2B2533),
    subText = Color(0xFF6E6578),
    background = Color(0xFFF9F8FA),
    subBackground = Color(0xFFFFFFFF),
    popup = Color(0xFFF2EEF5),
    stroke = Color(0xFFE6DCEB),
    onHighlight = Color.White,
)

/** 暗色：`--color-bg-base #121016` / `--color-primary #FF8FB1`。 */
val DinnerPartyDarkColors = darkSaltColors(
    highlight = Color(0xFFFF8FB1),
    text = Color(0xFFF3EFEF),
    subText = Color(0xFF9E97A9),
    background = Color(0xFF121016),
    subBackground = Color(0xFF1A1625),
    popup = Color(0xFF251F34),
    stroke = Color(0xFF382E47),
    onHighlight = Color.White,
)

/** 品牌主色 亮色 */
private val PrimaryLight = Color(0xFFE2789F)

/** 品牌辅色 亮色 */
private val SecondaryLight = Color(0xFF8C6BB1)

/** 品牌主色 暗色 */
private val PrimaryDark = Color(0xFFFF8FB1)

/** 品牌辅色 暗色 */
private val SecondaryDark = Color(0xFFA882DD)

/** 主题色 主色用于主要按钮与选中态 辅色用于标签与强调图标 */
data class DinnerPartyAccentColors(
    val primary: Color,
    val secondary: Color,
)

/** 按亮暗补齐主题色 传入为空取品牌默认 */
fun accentColors(dark: Boolean, primary: Color?, secondary: Color?): DinnerPartyAccentColors =
    if (dark) {
        DinnerPartyAccentColors(primary ?: PrimaryDark, secondary ?: SecondaryDark)
    } else {
        DinnerPartyAccentColors(primary ?: PrimaryLight, secondary ?: SecondaryLight)
    }

/** 亮色板 primary 非空时覆盖高亮色 */
fun lightColors(primary: Color?): SaltColors = if (primary == null) {
    DinnerPartyLightColors
} else {
    DinnerPartyLightColors.copy(highlight = primary, onHighlight = primary.contrastText())
}

/** 暗色板 primary 非空时覆盖高亮色 */
fun darkColors(primary: Color?): SaltColors = if (primary == null) {
    DinnerPartyDarkColors
} else {
    DinnerPartyDarkColors.copy(highlight = primary, onHighlight = primary.contrastText())
}

/** 高亮色上的文字色 取对比度更高的深色或白色 */
private fun Color.contrastText(): Color =
    if (contrastRatio(this, Color(0xFF2B2533)) >= contrastRatio(this, Color.White)) {
        Color(0xFF2B2533)
    } else {
        Color.White
    }

/** 两色对比度 WCAG 相对亮度比值 */
private fun contrastRatio(first: Color, second: Color): Float {
    val a = first.luminance()
    val b = second.luminance()
    return (maxOf(a, b) + 0.05f) / (minOf(a, b) + 0.05f)
}

/** 颜色转 #RRGGBB 文本 */
internal fun Color.hexText(): String =
    "#" + (toArgb() and 0xFFFFFF).toString(16).uppercase().padStart(6, '0')

/**
 * 语义色
 */
data class DinnerPartySemanticColors(
    /** 强烈推荐 */
    val strongYes: Color,
    /** 均线之上 */
    val aboveAverage: Color,
    /** 均线之下 */
    val belowAverage: Color,
    /** 避雷 */
    val danger: Color,
)

val DinnerPartyLightSemanticColors = DinnerPartySemanticColors(
    strongYes = Color(0xFF3D9970),
    aboveAverage = Color(0xFFE2789F),
    belowAverage = Color(0xFFE67E22),
    danger = Color(0xFFD9534F),
)

val DinnerPartyDarkSemanticColors = DinnerPartySemanticColors(
    strongYes = Color(0xFF5CD698),
    aboveAverage = Color(0xFFFF8FB1),
    belowAverage = Color(0xFFFFB049),
    danger = Color(0xFFFF6B6B),
)

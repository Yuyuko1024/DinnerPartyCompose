package cc.dinnerparty.compose.ui.theme

import androidx.compose.ui.graphics.Color
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

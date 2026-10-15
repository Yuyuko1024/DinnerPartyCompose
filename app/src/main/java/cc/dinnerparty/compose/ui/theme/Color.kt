package cc.dinnerparty.compose.ui.theme

import androidx.compose.ui.graphics.Color
import com.moriafly.salt.ui.darkSaltColors
import com.moriafly.salt.ui.lightSaltColors

/**
 * 品牌色板。
 *
 * 数值直接取自 www.dinnerparty.cc 首页内联样式里的 CSS 设计令牌
 * （`:root` 对应亮色，`:root[data-theme=dark]` 对应暗色），
 * 保证 App 与 Web 两端视觉一致，且后续改色只需对照同一份令牌。
 *
 * SaltUI 的 `SaltColors` 槽位与前端语义一一对应：
 *
 * | SaltUI        | CSS 令牌               |
 * |:--            |:--                     |
 * | highlight     | --color-primary        |
 * | text          | --color-text-primary   |
 * | subText       | --color-text-secondary |
 * | background    | --color-bg-base        |
 * | subBackground | --color-bg-surface     |
 * | popup         | --color-bg-overlay     |
 * | stroke        | --color-border         |
 * | onHighlight   | 主色上的前景色（白）     |
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
 * 语义色：评价四档各自代表的「好 / 一般 / 差 / 避雷」。
 *
 * SaltUI 的 `SaltColors` 只有主色与灰阶，没有成功 / 警告 / 危险槽位，
 * 而评价档位、避雷标记都要用它们，所以单独列一份。
 *
 * 数值同样取自 Web `index.html` 的 `--color-success / --color-warning / --color-danger`，
 * [aboveAverage] 用的是 `--theme-primary`，在 App 里即 `--color-primary`。
 */
data class DinnerPartySemanticColors(
    /** 强烈推荐 */
    val strongYes: Color,
    /** 均线之上 */
    val aboveAverage: Color,
    /** 均线之下 */
    val belowAverage: Color,
    /** 强烈拒绝，也就是「避雷」 */
    val danger: Color,
)

/** 亮色语义色：`#3D9970` / `#E2789F` / `#E67E22` / `#D9534F`。 */
val DinnerPartyLightSemanticColors = DinnerPartySemanticColors(
    strongYes = Color(0xFF3D9970),
    aboveAverage = Color(0xFFE2789F),
    belowAverage = Color(0xFFE67E22),
    danger = Color(0xFFD9534F),
)

/** 暗色语义色：`#5CD698` / `#FF8FB1` / `#FFB049` / `#FF6B6B`。 */
val DinnerPartyDarkSemanticColors = DinnerPartySemanticColors(
    strongYes = Color(0xFF5CD698),
    aboveAverage = Color(0xFFFF8FB1),
    belowAverage = Color(0xFFFFB049),
    danger = Color(0xFFFF6B6B),
)

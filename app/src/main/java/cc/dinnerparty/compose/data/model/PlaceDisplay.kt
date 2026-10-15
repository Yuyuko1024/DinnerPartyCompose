package cc.dinnerparty.compose.data.model

import cc.dinnerparty.compose.api.model.Place
import kotlin.math.floor

/**
 * 地点评价的四档，与后端 `modules/ratings/services/placeRatings.js` 算出的 `tier` 一一对应。
 *
 * 分数只用来算平均，界面上始终用评价词表达——「3.7 分」对社区用户没有意义，
 * 「均线之上」才有。所以这里不给分数，只给档位。
 */
enum class RatingTier {
    STRONG_YES,
    ABOVE_AVERAGE,
    BELOW_AVERAGE,
    STRONG_NO;

    companion object {
        /**
         * 平均分 → 最接近的一档，正好落在两档中间时取较好的那一档；没有评价时为 null。
         *
         * 与 Web `places/rating.js` 的 `tierForAverage` 完全一致（`floor(v + 0.5)` 再夹到 1~4）。
         */
        fun forAverage(average: Double?): RatingTier? {
            if (average == null || !average.isFinite()) return null
            return when (floor(average + 0.5).toInt().coerceIn(1, 4)) {
                4 -> STRONG_YES
                3 -> ABOVE_AVERAGE
                2 -> BELOW_AVERAGE
                else -> STRONG_NO
            }
        }
    }
}

/**
 * 是不是「避雷」地点。
 *
 * 有评价就看综合评价有没有落在均线之下；一条评价都没有时，退而看分类里有没有带「避雷」字样。
 * 地图上的图标和名称胶囊都按这个结论换色，与 Web `places/rating.js` 的 `isAvoidedPlace` 一致。
 */
fun Place.isAvoided(): Boolean {
    val count = ratingCount ?: 0
    if (count > 0) return (ratingAverage ?: 0.0) < AvoidBelowAverage
    return category.orEmpty().contains(AvoidTag)
}

/**
 * 用来算档位的平均分：**没人评价时不给分**。
 *
 * 后端对没有评价的地点会把 `rating_average` 算成 0 或 null，直接拿它去查档位会得到「强烈拒绝」，
 * 所以必须先按人数过一道。
 */
fun Place.ratingAverageOrNull(): Double? =
    if ((ratingCount ?: 0) > 0) ratingAverage else null

/** 「最近修改」里的修改人，优先用用户名，退化到用户 id。 */
fun Place.lastModifierName(): String? =
    updatedByName?.takeIf { it.isNotBlank() }
        ?: creatorName?.takeIf { it.isNotBlank() }
        ?: updatedBy?.takeIf { it.isNotBlank() }
        ?: creatorId?.takeIf { it.isNotBlank() }

/** 「最近修改」里的日期，只要到日。后端给的是 `YYYY-MM-DD HH:MM:SS`。 */
fun Place.lastModifiedDate(): String? {
    val raw = updatedTime ?: createdTime ?: return null
    return IsoDatePrefix.find(raw)?.value
}

private const val AvoidTag = "避雷"
private const val AvoidBelowAverage = 2.5

private val IsoDatePrefix = Regex("""^(\d{4}-\d{2}-\d{2})""")

package cc.dinnerparty.compose.data.model

import cc.dinnerparty.compose.api.model.Place
import cc.dinnerparty.compose.api.model.User
import kotlin.math.floor

/** 地点评价的四档，与后端 `placeRatings.js` 算出的 `tier` 一一对应。 */
enum class RatingTier {
    STRONG_YES,
    ABOVE_AVERAGE,
    BELOW_AVERAGE,
    STRONG_NO;

    companion object {
        /** 平均分 → 最接近的一档，正好落在两档中间时取较好的那一档；没有评价时为 null。 */
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

    /** 档位对应的分值。 */
    fun score(): Int = when (this) {
        STRONG_YES -> 4
        ABOVE_AVERAGE -> 3
        BELOW_AVERAGE -> 2
        STRONG_NO -> 1
    }
}

/** 分值 → 档位，超出范围按最近的一端算。 */
fun tierOf(score: Int): RatingTier = when (score.coerceIn(1, 4)) {
    4 -> RatingTier.STRONG_YES
    3 -> RatingTier.ABOVE_AVERAGE
    2 -> RatingTier.BELOW_AVERAGE
    else -> RatingTier.STRONG_NO
}

/** 新建地点的默认分，带避雷分类时为最低档。 */
fun defaultScore(tags: List<String>): Int =
    if (tags.any { it.contains(AvoidTag) }) RatingTier.STRONG_NO.score()
    else RatingTier.STRONG_YES.score()

/** 当前用户能否修改该地点。 */
fun Place.canManage(user: User?): Boolean {
    if (user == null) return false
    if ((user.adminLevel?.toIntOrNull() ?: 0) > 0) return true
    return creatorId != null && creatorId == user.id
}

/** 是不是「避雷」地点：有评价看综合评分是否低于均线，没评价就看分类名是否含「避雷」。 */
fun Place.isAvoided(): Boolean {
    val count = ratingCount ?: 0
    if (count > 0) return (ratingAverage ?: 0.0) < AvoidBelowAverage
    return category.orEmpty().contains(AvoidTag)
}

/** 平均分，没人评价时返回 null。 */
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

package cc.dinnerparty.compose.ui.view.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material.icons.outlined.SentimentDissatisfied
import androidx.compose.material.icons.outlined.SentimentSatisfied
import androidx.compose.material.icons.outlined.SentimentVeryDissatisfied
import androidx.compose.material.icons.outlined.SentimentVerySatisfied
import androidx.compose.material.icons.outlined.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.dinnerparty.compose.R
import cc.dinnerparty.compose.api.model.Place
import cc.dinnerparty.compose.data.model.RatingTier
import cc.dinnerparty.compose.data.model.lastModifiedDate
import cc.dinnerparty.compose.data.model.lastModifierName
import cc.dinnerparty.compose.data.model.ratingAverageOrNull
import cc.dinnerparty.compose.ui.theme.DinnerPartySemanticColors
import cc.dinnerparty.compose.ui.theme.dinnerPartySemanticColors
import com.moriafly.salt.ui.Icon
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.Text
import com.moriafly.salt.ui.noRippleClickable
import java.util.Locale

/** 地点卡片，位置由 [AmapView] 锚定在地图坐标正上方。 */
@Composable
fun PlaceCard(
    place: Place,
    modifier: Modifier = Modifier,
    onClose: () -> Unit = {}
) {
    val semanticColors = dinnerPartySemanticColors()

    Column(
        modifier = modifier
            .fillMaxWidth(CardWidthRatio)
            .widthIn(max = CardMaxWidth)
            .clip(CardShape)
            .background(SaltTheme.colors.subBackground)
            .border(1.dp, SaltTheme.colors.stroke, CardShape)
            // 拦截卡片上的点按
            .noRippleClickable { }
            .padding(CardPadding)
    ) {
        PlaceCardTitle(name = place.name.orEmpty(), onClose = onClose)
        PlaceCardDescription(place.description)
        PlaceCardMeta(category = place.category, perPersonCost = place.perPersonCost)
        PlaceCardRating(place = place, semanticColors = semanticColors)
        PlaceCardLastModified(place = place)
        PlaceCardActions()
    }
}

@Composable
private fun PlaceCardTitle(name: String, onClose: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = name,
            modifier = Modifier.weight(1f),
            color = SaltTheme.colors.text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Box(
            modifier = Modifier
                .size(CloseButtonSize)
                .clip(CircleShape)
                .noRippleClickable(onClick = onClose),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = stringResource(R.string.place_card_close),
                modifier = Modifier.size(18.dp),
                tint = SaltTheme.colors.subText
            )
        }
    }
}

/**
 * 简介。超过 [DescriptionLimit] 字就截断加省略号，后面接一句淡色提示。
 */
@Composable
private fun PlaceCardDescription(description: String?) {
    val normalized = remember(description) {
        description.orEmpty().replace(Whitespace, " ").trim()
    }
    if (normalized.isEmpty()) return

    val openDetails = stringResource(R.string.place_card_open_details)
    val hasMore = normalized.length > DescriptionLimit
    val body = if (hasMore) normalized.take(DescriptionLimit).trimEnd() + "…" else normalized
    val mutedColor = SaltTheme.colors.subText

    Spacer(modifier = Modifier.height(6.dp))
    Text(
        text = buildAnnotatedString {
            append(body)
            if (hasMore) {
                withStyle(SpanStyle(color = mutedColor)) {
                    append(" ")
                    append(openDetails)
                }
            }
        },
        color = SaltTheme.colors.text,
        fontSize = 13.sp,
        lineHeight = 20.sp
    )
}

@Composable
private fun PlaceCardMeta(category: String?, perPersonCost: Int?) {
    Spacer(modifier = Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            // 分类缺失时显示一个短横
            text = stringResource(R.string.place_card_category, category ?: "-"),
            color = SaltTheme.colors.subText,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (perPersonCost != null) {
            Text(
                text = stringResource(R.string.place_card_per_person_cost, perPersonCost),
                color = SaltTheme.colors.subText,
                fontSize = 12.sp,
                maxLines = 1
            )
        }
    }
}

/** 评价摘要行，右侧是「去评价」入口。 */
@Composable
private fun PlaceCardRating(place: Place, semanticColors: DinnerPartySemanticColors) {
    val average = place.ratingAverageOrNull()
    val tier = RatingTier.forAverage(average)
    val tierLabel = ratingTierLabel(tier)
    val count = place.ratingCount ?: 0

    val summary = if (tierLabel != null && average != null) {
        stringResource(R.string.rating_summary, tierLabel, formatAverage(average), count)
    } else {
        stringResource(R.string.rating_summary_empty)
    }

    Spacer(modifier = Modifier.height(8.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = RatingRowMinHeight)
            .clip(RatingRowShape)
            .border(1.dp, SaltTheme.colors.stroke, RatingRowShape)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (tier != null) ratingTierIcon(tier) else Icons.Outlined.RateReview,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = if (tier != null) ratingTierColor(tier, semanticColors) else SaltTheme.colors.subText
        )
        Spacer(modifier = Modifier.width(CardGap))
        Text(
            text = summary,
            modifier = Modifier.weight(1f),
            color = SaltTheme.colors.text,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.width(CardGap))
        Text(
            text = stringResource(R.string.rating_action_rate),
            color = SaltTheme.colors.highlight,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = SaltTheme.colors.subText
        )
    }
}

@Composable
private fun PlaceCardLastModified(place: Place) {
    val modifierName = place.lastModifierName() ?: "-"
    val date = place.lastModifiedDate() ?: "-"

    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = stringResource(R.string.place_card_last_modified, "$modifierName · $date"),
        color = SaltTheme.colors.subText,
        fontSize = 12.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun PlaceCardActions() {
    Spacer(modifier = Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        PlaceCardAction(
            icon = Icons.Outlined.NearMe,
            contentDescription = stringResource(R.string.place_action_navigate),
            label = stringResource(R.string.place_action_navigate)
        )
        PlaceCardAction(
            icon = Icons.Outlined.Share,
            contentDescription = stringResource(R.string.place_action_share),
            label = stringResource(R.string.place_action_share)
        )
        // 后面三个只出图标
        PlaceCardAction(
            icon = Icons.Outlined.FavoriteBorder,
            contentDescription = stringResource(R.string.action_favorites)
        )
        PlaceCardAction(
            icon = Icons.Outlined.Edit,
            contentDescription = stringResource(R.string.place_action_edit)
        )
        PlaceCardAction(
            icon = Icons.Outlined.MoreHoriz,
            contentDescription = stringResource(R.string.action_more)
        )
    }
}

@Composable
private fun PlaceCardAction(
    icon: ImageVector,
    contentDescription: String,
    label: String? = null
) {
    Row(
        modifier = Modifier
            .heightIn(min = ActionButtonMinSize)
            .widthIn(min = ActionButtonMinSize)
            .clip(ActionButtonShape)
            .border(1.dp, SaltTheme.colors.stroke, ActionButtonShape)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(20.dp),
            tint = SaltTheme.colors.text
        )
        if (label != null) {
            Text(
                text = label,
                color = SaltTheme.colors.text,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

/** 档位词，无档位时返回 null。 */
@Composable
private fun ratingTierLabel(tier: RatingTier?): String? = when (tier) {
    null -> null
    RatingTier.STRONG_YES -> stringResource(R.string.rating_tier_strong_yes)
    RatingTier.ABOVE_AVERAGE -> stringResource(R.string.rating_tier_above_average)
    RatingTier.BELOW_AVERAGE -> stringResource(R.string.rating_tier_below_average)
    RatingTier.STRONG_NO -> stringResource(R.string.rating_tier_strong_no)
}

private fun ratingTierIcon(tier: RatingTier): ImageVector = when (tier) {
    RatingTier.STRONG_YES -> Icons.Outlined.SentimentVerySatisfied
    RatingTier.ABOVE_AVERAGE -> Icons.Outlined.SentimentSatisfied
    RatingTier.BELOW_AVERAGE -> Icons.Outlined.SentimentDissatisfied
    RatingTier.STRONG_NO -> Icons.Outlined.SentimentVeryDissatisfied
}

private fun ratingTierColor(tier: RatingTier, colors: DinnerPartySemanticColors): Color = when (tier) {
    RatingTier.STRONG_YES -> colors.strongYes
    RatingTier.ABOVE_AVERAGE -> colors.aboveAverage
    RatingTier.BELOW_AVERAGE -> colors.belowAverage
    RatingTier.STRONG_NO -> colors.danger
}

/** 固定一位小数 */
private fun formatAverage(average: Double): String = String.format(Locale.US, "%.1f", average)

private val CardShape = RoundedCornerShape(12.dp)
private val RatingRowShape = RoundedCornerShape(8.dp)
private val ActionButtonShape = RoundedCornerShape(8.dp)

private val CardPadding = 10.dp
private val CardGap = 6.dp
private val CloseButtonSize = 24.dp
private val RatingRowMinHeight = 40.dp
private val ActionButtonMinSize = 44.dp

private const val CardWidthRatio = 0.92f
private val CardMaxWidth = 320.dp

/** 简介预览的字数上限 */
private const val DescriptionLimit = 80

private val Whitespace = Regex("\\s+")

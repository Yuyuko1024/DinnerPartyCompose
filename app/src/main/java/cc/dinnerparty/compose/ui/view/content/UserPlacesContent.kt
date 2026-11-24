package cc.dinnerparty.compose.ui.view.content

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PersonPinCircle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.dinnerparty.compose.R
import cc.dinnerparty.compose.api.model.Place
import cc.dinnerparty.compose.data.model.isAvoided
import cc.dinnerparty.compose.ui.theme.dinnerPartySemanticColors
import cc.dinnerparty.compose.ui.view.component.mapGlass
import cc.dinnerparty.compose.ui.viewmodel.UserPlacesUiState
import com.moriafly.salt.ui.Button
import com.moriafly.salt.ui.ButtonAppearance
import com.moriafly.salt.ui.Icon
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.Text
import com.moriafly.salt.ui.UnstableSaltUiApi

/** 住民地点列表与筛选条的操作 */
class UserPlacesOps(
    /** 打开某个地点 与住民主页共用同一个入口 */
    val onOpenPlace: (Long) -> Unit = {},
    /** 打开这位住民的住民主页 */
    val onProfile: () -> Unit = {},
    /** 退出筛选 地图恢复显示附近地点 */
    val onExit: () -> Unit = {},
    val onRetry: () -> Unit = {},
    /** 从筛选条重新打开列表 */
    val onOpenList: () -> Unit = {},
)

/** 住民地点列表 收起列表不退出筛选 */
@OptIn(UnstableSaltUiApi::class)
@Composable
fun UserPlacesContent(
    state: UserPlacesUiState,
    ops: UserPlacesOps,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.user_places_title),
                modifier = Modifier.weight(1f).semantics { heading() },
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.user_places_collapse),
                    modifier = Modifier.size(18.dp),
                    tint = SaltTheme.colors.subText,
                )
            }
        }
        val summary = summaryText(state)
        if (summary.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = summary, color = SaltTheme.colors.subText, fontSize = 13.sp)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = ops.onProfile,
                text = stringResource(R.string.user_places_profile),
                modifier = Modifier.weight(1f),
                appearance = ButtonAppearance.Subtle,
                maxLines = 1,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = ops.onExit,
                text = stringResource(R.string.user_places_exit),
                modifier = Modifier.weight(1f),
                appearance = ButtonAppearance.Subtle,
                maxLines = 1,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        val places = state.places.orEmpty()
        when {
            state.loading -> UserPlacesHint(stringResource(R.string.user_places_loading))
            state.error != null -> UserPlacesError(state.error, ops.onRetry)
            places.isEmpty() -> UserPlacesHint(stringResource(R.string.user_places_empty))
            else -> places.forEach { place -> UserPlaceRow(place, ops.onOpenPlace) }
        }
    }
}

/** 地图上的住民筛选条 点文字重新打开列表 点叉退出筛选 */
@OptIn(UnstableSaltUiApi::class)
@Composable
fun UserPlacesChip(
    state: UserPlacesUiState,
    ops: UserPlacesOps,
    modifier: Modifier = Modifier,
) {
    val shape = SaltTheme.shapes.large
    Row(
        modifier = modifier
            .height(44.dp)
            .widthIn(max = 280.dp)
            .mapGlass(shape),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f, fill = false)
                .height(44.dp)
                .clickable(onClick = ops.onOpenList)
                .padding(start = 14.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.PersonPinCircle,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = SaltTheme.colors.highlight,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = chipText(state),
                color = SaltTheme.colors.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .clickable(onClick = ops.onExit),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = stringResource(R.string.user_places_exit),
                modifier = Modifier.size(18.dp),
                tint = SaltTheme.colors.subText,
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
    }
}

/** 列表里的一行地点 */
@OptIn(UnstableSaltUiApi::class)
@Composable
private fun UserPlaceRow(place: Place, onOpen: (Long) -> Unit) {
    val id = place.id
    val tone = when {
        place.isAvoided() -> dinnerPartySemanticColors().danger
        else -> SaltTheme.colors.text
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(enabled = id != null, role = Role.Button) { id?.let(onOpen) }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = place.name?.takeIf { it.isNotBlank() }
                    ?: stringResource(R.string.cluster_unnamed),
                color = tone,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = placeDetailText(place),
                color = if (place.isHidden()) SaltTheme.colors.warning else SaltTheme.colors.subText,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (place.isRejected()) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.user_places_rejected),
                modifier = Modifier
                    .clip(CircleShape)
                    .background(SaltTheme.colors.subBackground)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                color = SaltTheme.colors.error,
                fontSize = 11.sp,
                maxLines = 1,
            )
        }
    }
}

/** 加载中或没有地点时的说明 */
@Composable
private fun UserPlacesHint(text: String) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        color = SaltTheme.colors.subText,
        fontSize = 13.sp,
    )
}

/** 加载失败的说明与重试 */
@OptIn(UnstableSaltUiApi::class)
@Composable
private fun UserPlacesError(message: String, onRetry: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
        Text(text = message, color = SaltTheme.colors.error, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(10.dp))
        Button(
            onClick = onRetry,
            text = stringResource(R.string.user_places_retry),
            appearance = ButtonAppearance.Subtle,
        )
    }
}

/** 标题下方的地点数量说明 */
@Composable
private fun summaryText(state: UserPlacesUiState): String {
    if (!state.ready || state.total <= 0) return ""
    val shown = state.places.orEmpty().size
    return if (state.total > shown) {
        stringResource(R.string.user_places_summary_limited, state.total, shown)
    } else {
        stringResource(R.string.user_places_summary, state.total)
    }
}

/** 筛选条文字 */
@Composable
private fun chipText(state: UserPlacesUiState): String = when {
    state.loading -> stringResource(R.string.user_places_chip_loading)
    state.error != null -> stringResource(R.string.user_places_failed)
    else -> stringResource(R.string.user_places_chip, state.total)
}

/** 一行的次要信息 审核中 分类 人均 创建日期 */
@Composable
private fun placeDetailText(place: Place): String {
    val category = place.category?.takeIf { it.isNotBlank() }
        ?: stringResource(R.string.cluster_uncategorized)
    val day = place.createdTime
        ?.substringBefore(' ')
        ?.takeIf { it.isNotBlank() }
        ?.let { stringResource(R.string.user_places_created, it) }
    val reviewing = if (place.isHidden() && !place.isRejected()) {
        stringResource(R.string.user_places_reviewing)
    } else {
        null
    }
    val cost = place.perPersonCost
        ?.let { stringResource(R.string.place_card_per_person_cost, it) }
    return listOfNotNull(reviewing, category, cost, day).joinToString(" · ")
}

/** 自己创建 只有自己看得到的地点 */
private fun Place.isHidden(): Boolean {
    val status = moderationStatus ?: return false
    return status.isNotBlank() && status != ModerationVisible
}

/** 未通过审核的地点 */
private fun Place.isRejected(): Boolean = moderationStatus == ModerationRejected

private const val ModerationVisible = "visible"

private const val ModerationRejected = "rejected"

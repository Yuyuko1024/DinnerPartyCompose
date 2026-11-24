package cc.dinnerparty.compose.ui.view.content

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
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
import cc.dinnerparty.compose.api.model.ProfilePlace
import cc.dinnerparty.compose.ui.viewmodel.FavoritesUiState
import com.moriafly.salt.ui.Button
import com.moriafly.salt.ui.ButtonAppearance
import com.moriafly.salt.ui.Icon
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.Text
import com.moriafly.salt.ui.UnstableSaltUiApi

/** 收藏夹的操作 */
class FavoritesOps(
    /** 打开收藏夹 未登录时由外部转去登录 */
    val onOpen: () -> Unit = {},
    /** 打开某个地点 与住民主页共用同一个入口 */
    val onOpenPlace: (Long) -> Unit = {},
    val onRetry: () -> Unit = {},
)

/** 收藏夹列表 点地点打开详情 */
@OptIn(UnstableSaltUiApi::class)
@Composable
fun FavoritesContent(
    state: FavoritesUiState,
    ops: FavoritesOps,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.profile_widgets_collection_mine),
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
                    contentDescription = stringResource(R.string.favorites_close),
                    modifier = Modifier.size(18.dp),
                    tint = SaltTheme.colors.subText,
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        val places = state.places.orEmpty()
        when {
            state.loading -> FavoritesHint(stringResource(R.string.favorites_loading))
            state.error != null -> FavoritesError(state.error, ops.onRetry)
            places.isEmpty() -> FavoritesHint(
                stringResource(R.string.profile_widgets_collection_empty)
            )
            else -> places.forEach { place ->
                key(place.id) { FavoriteRow(place, ops.onOpenPlace) }
            }
        }
    }
}

/** 收藏夹里的一行地点 */
@OptIn(UnstableSaltUiApi::class)
@Composable
private fun FavoriteRow(place: ProfilePlace, onOpen: (Long) -> Unit) {
    val id = place.id
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(enabled = id != null, role = Role.Button) { id?.let(onOpen) }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.FavoriteBorder,
            contentDescription = null,
            tint = SaltTheme.colors.highlight,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = place.name?.takeIf { it.isNotBlank() }
                    ?: stringResource(R.string.cluster_unnamed),
                color = SaltTheme.colors.text,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = place.category?.takeIf { it.isNotBlank() }
                    ?: stringResource(R.string.cluster_uncategorized),
                color = SaltTheme.colors.subText,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** 加载中或没有收藏时的说明 */
@Composable
private fun FavoritesHint(text: String) {
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
private fun FavoritesError(message: String, onRetry: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
        Text(text = message, color = SaltTheme.colors.error, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(10.dp))
        Button(
            onClick = onRetry,
            text = stringResource(R.string.favorites_retry),
            appearance = ButtonAppearance.Subtle,
        )
    }
}

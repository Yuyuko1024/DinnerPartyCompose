package cc.dinnerparty.compose.ui.view.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Public
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.dinnerparty.compose.R
import cc.dinnerparty.compose.data.model.SearchItem
import cc.dinnerparty.compose.ui.viewmodel.SearchUiState
import com.moriafly.salt.ui.Icon
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.Text
import com.moriafly.salt.ui.UnstableSaltUiApi
import java.util.Locale
import kotlin.math.roundToInt

/** 结果面板最大高度 */
internal val PanelMaxHeight = 360.dp

/** 搜索结果面板 站内结果与高德结果分区展示 */
@UnstableSaltUiApi
@Composable
fun SearchPanel(
    state: SearchUiState,
    actions: MapSearchActions,
    modifier: Modifier = Modifier,
) {
    val dismiss = rememberDismiss()
    val blank = remember { MutableInteractionSource() }
    val panelShape = SaltTheme.shapes.medium
    val select: (SearchItem) -> Unit = { item ->
        dismiss()
        actions.selectItem(item)
    }
    val findAmap: () -> Unit = {
        dismiss()
        actions.amapClick()
    }

    Column(
        modifier = modifier
            .mapGlass(panelShape)
            .clickable(
                interactionSource = blank,
                indication = null,
                onClick = dismiss,
            )
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp)
        ) {
            SectionTitle(text = stringResource(R.string.search_site_section))
            if (state.isLoading) {
                HintText(text = stringResource(R.string.search_loading))
            }
            if (!state.isLoading && state.siteResults.isEmpty()) {
                HintText(text = stringResource(R.string.search_empty_site))
            }
            state.siteResults.forEach { item ->
                ResultRow(item = item, onSelect = select)
            }
            Spacer(modifier = Modifier.height(8.dp))
            SectionTitle(text = stringResource(R.string.search_amap_section))
            when {
                state.isAmapLoading -> HintText(
                    text = stringResource(R.string.search_loading)
                )

                state.amapSearched && state.amapResults.isEmpty() -> HintText(
                    text = stringResource(R.string.search_empty_amap)
                )

                state.amapSearched -> state.amapResults.forEach { item ->
                    ResultRow(item = item, onSelect = select)
                }

                else -> AmapActionRow(onClick = findAmap)
            }
            state.errorMessage?.let { message ->
                HintText(text = message)
            }
        }
    }
}

/** 分区标题 */
@UnstableSaltUiApi
@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = SaltTheme.colors.subText,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
    )
}

/** 提示文字 */
@UnstableSaltUiApi
@Composable
private fun HintText(text: String) {
    Text(
        text = text,
        color = SaltTheme.colors.subText,
        fontSize = 13.sp,
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
    )
}

/** 触发高德未收录地点检索 */
@UnstableSaltUiApi
@Composable
private fun AmapActionRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Public,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = SaltTheme.colors.highlight
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = stringResource(R.string.search_find_unlisted),
            color = SaltTheme.colors.highlight,
            fontSize = 14.sp
        )
    }
}

/** 单条搜索结果 */
@UnstableSaltUiApi
@Composable
private fun ResultRow(item: SearchItem, onSelect: (SearchItem) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(item) }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Place,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = SaltTheme.colors.subText
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                color = SaltTheme.colors.text,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val address = item.address
            if (!address.isNullOrBlank()) {
                Text(
                    text = address,
                    color = SaltTheme.colors.subText,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        item.distanceMeters?.let { meters ->
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = distanceText(meters),
                color = SaltTheme.colors.subText,
                fontSize = 12.sp
            )
        }
    }
}

/** 距离展示 小于一公里用米 */
@Composable
private fun distanceText(meters: Double): String =
    if (meters < 1000) {
        stringResource(R.string.search_distance_m, meters.roundToInt())
    } else {
        stringResource(
            R.string.search_distance_km,
            String.format(Locale.getDefault(), "%.1f", meters / 1000)
        )
    }

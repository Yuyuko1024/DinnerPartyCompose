package cc.dinnerparty.compose.ui.view.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.dinnerparty.compose.R
import com.moriafly.salt.ui.ItemEdit
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.Text
import com.moriafly.salt.ui.UnstableSaltUiApi

/** 搜索条高度。 */
private val SearchBarHeight = 44.dp

/** 地图页搜索条，左侧胶囊输入框加右侧圆形搜索按钮。 */
@UnstableSaltUiApi
@Composable
fun MapSearchBar(
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    onSearchClick: () -> Unit = {}
) {
    val hint = placeholder ?: stringResource(R.string.map_search_hint)

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(SearchBarHeight)
                .clip(RoundedCornerShape(24.dp))
                .background(SaltTheme.colors.subBackground)
                .border(1.dp, SaltTheme.colors.stroke, RoundedCornerShape(24.dp))
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            /*Text(
                text = hint,
                color = SaltTheme.colors.subText,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )*/
            ItemEdit(
                text = "",
                onChange = { query ->

                },
                hint = hint,
                hintColor = SaltTheme.colors.text,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        CircleIconButton(
            imageVector = Icons.Outlined.Search,
            contentDescription = stringResource(R.string.action_search),
            onClick = onSearchClick,
            containerColor = SaltTheme.colors.highlight,
            contentColor = SaltTheme.colors.onHighlight,
            borderColor = null
        )
    }
}

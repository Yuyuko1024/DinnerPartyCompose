package cc.dinnerparty.compose.ui.view.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.dinnerparty.compose.R
import cc.dinnerparty.compose.data.model.SearchItem
import cc.dinnerparty.compose.ui.viewmodel.SearchUiState
import com.moriafly.salt.ui.Icon
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.UnstableSaltUiApi
import com.moriafly.salt.ui.Text

/** 搜索框与结果面板共用的事件 */
@Immutable
class MapSearchActions(
    val queryChange: (String) -> Unit,
    val searchClick: () -> Unit,
    val clearClick: () -> Unit,
    val amapClick: () -> Unit,
    val selectItem: (SearchItem) -> Unit,
)

/** 搜索条高度 */
private val SearchBarHeight = 44.dp

/** 地图页搜索条 左侧胶囊输入框加右侧圆形搜索按钮 */
@UnstableSaltUiApi
@Composable
fun MapSearchBar(
    state: SearchUiState,
    actions: MapSearchActions,
    modifier: Modifier = Modifier,
) {
    val focus = LocalFocusManager.current
    val dismiss = rememberDismiss()
    val barShape = SaltTheme.shapes.large

    // 键盘收起后同步清除焦点
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    var imeShown by remember { mutableStateOf(false) }
    LaunchedEffect(imeVisible) {
        if (imeVisible) {
            imeShown = true
            return@LaunchedEffect
        }
        if (imeShown) {
            imeShown = false
            focus.clearFocus()
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(SearchBarHeight)
                .clip(barShape)
                .background(SaltTheme.colors.subBackground)
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = state.query,
                    onValueChange = actions.queryChange,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    textStyle = SaltTheme.textStyles.main.copy(
                        color = SaltTheme.colors.text,
                        fontSize = 16.sp,
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(SaltTheme.colors.highlight),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            dismiss()
                            actions.searchClick()
                        }
                    ),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier.fillMaxHeight(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (state.query.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.map_search_hint),
                                    color = SaltTheme.colors.subText,
                                    fontSize = 16.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            innerTextField()
                        }
                    }
                )
                if (state.query.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clickable(onClick = actions.clearClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = stringResource(R.string.search_clear),
                            modifier = Modifier.size(18.dp),
                            tint = SaltTheme.colors.subText
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        CircleIconButton(
            imageVector = when {
                !state.submitted -> Icons.Outlined.Search
                state.resultsVisible -> Icons.Outlined.ExpandLess
                else -> Icons.Outlined.ExpandMore
            },
            contentDescription = stringResource(
                when {
                    !state.submitted -> R.string.action_search
                    state.resultsVisible -> R.string.search_collapse
                    else -> R.string.search_expand
                }
            ),
            onClick = {
                dismiss()
                actions.searchClick()
            },
            containerColor = SaltTheme.colors.highlight,
            contentColor = SaltTheme.colors.onHighlight
        )
    }
}

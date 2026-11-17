package cc.dinnerparty.compose.ui.view.content.pad

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cc.dinnerparty.compose.ui.view.content.MapMenuList

/** 菜单面板，内容为菜单项列表。 */
@Composable
fun MapMenuPanel(
    modifier: Modifier = Modifier,
    expanded: Boolean = false,
    onRandomClick: () -> Unit = {},
    onDiningClick: () -> Unit = {},
    onExportPostClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onAboutClick: () -> Unit = {},
    onDismissRequest: () -> Unit = {}
) {
    PadPanel(visible = expanded, modifier = modifier) {
        MapMenuList(
            modifier = Modifier.padding(vertical = 8.dp),
            onClose = onDismissRequest,
            onRandomClick = onRandomClick,
            onDiningClick = onDiningClick,
            onExportPostClick = onExportPostClick,
            onSettingsClick = onSettingsClick,
            onAboutClick = onAboutClick
        )
    }
}


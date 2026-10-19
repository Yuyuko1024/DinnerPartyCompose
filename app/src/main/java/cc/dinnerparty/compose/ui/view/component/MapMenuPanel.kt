package cc.dinnerparty.compose.ui.view.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moriafly.salt.ui.Icon
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.Text

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

/** 菜单项列表，点任一项先收起再执行对应操作。 */
@Composable
internal fun MapMenuList(
    modifier: Modifier = Modifier,
    onClose: () -> Unit = {},
    onRandomClick: () -> Unit = {},
    onDiningClick: () -> Unit = {},
    onExportPostClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onAboutClick: () -> Unit = {}
) {
    val actions = padMenuActions(
        onRandomClick = {
            onClose()
            onRandomClick()
        },
        onDiningClick = {
            onClose()
            onDiningClick()
        },
        onExportPostClick = {
            onClose()
            onExportPostClick()
        },
        onSettingsClick = {
            onClose()
            onSettingsClick()
        },
        onAboutClick = {
            onClose()
            onAboutClick()
        }
    )

    Column(modifier = modifier) {
        actions.forEach { action -> MapMenuItem(action) }
    }
}

@Composable
private fun MapMenuItem(action: MapAction) {
    val color = if (action.highlighted) {
        SaltTheme.colors.highlight
    } else {
        SaltTheme.colors.text
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = action.onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        action.imageVector?.let { imageVector ->
            Icon(
                imageVector = imageVector,
                contentDescription = null,
                modifier = Modifier.size(action.iconSize),
                tint = color
            )
        }
        Text(
            text = action.label,
            color = color,
            fontSize = 15.sp,
            maxLines = 1
        )
    }
}

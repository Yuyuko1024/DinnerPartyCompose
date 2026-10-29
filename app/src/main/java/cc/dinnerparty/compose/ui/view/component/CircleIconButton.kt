package cc.dinnerparty.compose.ui.view.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.moriafly.salt.ui.Icon
import com.moriafly.salt.ui.SaltTheme

/**
 * 圆形图标按钮，地图页各处悬浮操作按钮共用。
 */
@Composable
fun CircleIconButton(
    imageVector: ImageVector? = null,
    painterRes: Int? = null,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonSize: Dp = 44.dp,
    containerColor: Color = Color.Transparent,
    contentColor: Color = SaltTheme.colors.text
) {
    Box(
        modifier = modifier
            .size(buttonSize)
            .clearGlass(CircleShape)
            .background(containerColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        imageVector?.let {
            Icon(
                imageVector = it,
                contentDescription = contentDescription,
                modifier = Modifier.size(buttonSize / 2),
                tint = contentColor
            )
        }
        painterRes?.let {
            Icon(
                imageVector = ImageVector.vectorResource(it),
                contentDescription = contentDescription,
                modifier = Modifier.size(buttonSize / 2),
                tint = contentColor
            )
        }
    }
}

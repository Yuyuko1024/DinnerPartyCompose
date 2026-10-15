package cc.dinnerparty.compose.ui.view.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.moriafly.salt.ui.Icon
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.noRippleClickable

/**
 * 圆形图标按钮，地图页各处的悬浮操作按钮共用。
 *
 * 默认外观对应 Web 端 `.fm-map-account` 一类玻璃圆形按钮：
 * 表面色 + 1dp 描边；主色实心按钮（如搜索）把 [borderColor] 传 `null` 即可。
 *
 * Web 参考：`.fm-map-search__submit` 44×44、`border-radius:50%`。
 */
@Composable
fun CircleIconButton(
    imageVector: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonSize: Dp = 44.dp,
    containerColor: Color = SaltTheme.colors.subBackground,
    contentColor: Color = SaltTheme.colors.text,
    borderColor: Color? = SaltTheme.colors.stroke
) {
    Box(
        modifier = modifier
            .size(buttonSize)
            .clip(CircleShape)
            .background(containerColor)
            .then(
                if (borderColor != null) {
                    Modifier.border(1.dp, borderColor, CircleShape)
                } else {
                    Modifier
                }
            )
            .noRippleClickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            modifier = Modifier.size(buttonSize / 2),
            tint = contentColor
        )
    }
}

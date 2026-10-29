package cc.dinnerparty.compose.ui.view.component

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.dinnerparty.compose.R
import com.moriafly.salt.ui.Icon
import com.moriafly.salt.ui.ItemSwitcher
import com.moriafly.salt.ui.RoundedColumn
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.Text
import com.moriafly.salt.ui.UnstableSaltUiApi

/** 设置页内容，当前只有浮层玻璃开关 */
@OptIn(UnstableSaltUiApi::class)
@Composable
fun SettingsContent(
    modifier: Modifier = Modifier,
    liquidGlass: Boolean,
    onGlassChange: (Boolean) -> Unit,
    onClose: () -> Unit = {},
) {
    val context = LocalContext.current
    val glassOn = isGlassOn()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(SettingsPadding)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.settings_title),
                modifier = Modifier.weight(1f),
                color = SaltTheme.colors.text,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Box(
                modifier = Modifier
                    .size(CloseSize)
                    .clip(CircleShape)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.settings_close),
                    modifier = Modifier.size(18.dp),
                    tint = SaltTheme.colors.subText
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        RoundedColumn(
            modifier = Modifier.fillMaxWidth(),
            paddingValues = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
            color = Color.Transparent
        ) {
            ItemSwitcher(
                state = liquidGlass,
                onChange = { enabled ->
                    if (enabled && !glassOn) {
                        Toast.makeText(
                            context,
                            R.string.settings_glass_unsupported,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    onGlassChange(enabled)
                },
                text = stringResource(
                    if (glassOn) R.string.settings_glass else R.string.settings_blur
                ),
            )
        }
    }
}

private val SettingsPadding = 16.dp
private val CloseSize = 24.dp

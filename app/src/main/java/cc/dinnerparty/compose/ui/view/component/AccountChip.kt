package cc.dinnerparty.compose.ui.view.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.dinnerparty.compose.R
import cc.dinnerparty.compose.api.model.User
import cc.dinnerparty.compose.api.userAvatarUrl
import coil3.compose.AsyncImage
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.Text

/** 圆形住民头像，没有头像时显示住民名首字。 */
@Composable
fun UserAvatar(
    user: User,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    avatarVersion: Long = 0L
) {
    val avatarUrl = userAvatarUrl(user, avatarVersion)

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(SaltTheme.colors.highlight),
        contentAlignment = Alignment.Center
    ) {
        if (avatarUrl != null) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Image(
                painter = painterResource(R.drawable.default_avatar),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

/** Pad 端 Header 的账号药丸，头像与住民名水平居中。 */
@Composable
fun AccountChip(
    user: User,
    modifier: Modifier = Modifier,
    avatarVersion: Long = 0L,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(user = user, avatarVersion = avatarVersion)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = user.username.orEmpty(),
            fontSize = 14.sp,
            maxLines = 1
        )
    }
}

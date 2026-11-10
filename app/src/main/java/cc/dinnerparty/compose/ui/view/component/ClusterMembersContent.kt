package cc.dinnerparty.compose.ui.view.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Place
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.dinnerparty.compose.R
import cc.dinnerparty.compose.api.model.Place
import cc.dinnerparty.compose.data.model.isAvoided
import cc.dinnerparty.compose.ui.theme.dinnerPartySemanticColors
import com.moriafly.salt.ui.Icon
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.Text
import com.moriafly.salt.ui.UnstableSaltUiApi

@OptIn(UnstableSaltUiApi::class)
@Composable
fun ClusterMembersContent(
    places: List<Place>,
    onSelect: (Place) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.cluster_members_title, places.size),
                modifier = Modifier.weight(1f).semantics { heading() },
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            CircleIconButton(
                imageVector = Icons.Outlined.Close,
                contentDescription = stringResource(R.string.place_card_close),
                onClick = onClose,
                buttonSize = 40.dp,
            )
        }
        Text(
            text = stringResource(R.string.cluster_members_hint),
            color = SaltTheme.colors.subText,
            fontSize = 13.sp,
        )
        Spacer(modifier = Modifier.height(8.dp))
        places.forEach { place ->
            key(place.id) { ClusterMemberRow(place, onSelect) }
        }
    }
}

@OptIn(UnstableSaltUiApi::class)
@Composable
private fun ClusterMemberRow(place: Place, onSelect: (Place) -> Unit) {
    val color = if (place.isAvoided()) dinnerPartySemanticColors().danger else SaltTheme.colors.text
    val category = place.category?.takeIf { it.isNotBlank() }
        ?: stringResource(R.string.cluster_uncategorized)
    val cost = place.perPersonCost
    val subtitle = if (cost == null) category
        else stringResource(R.string.cluster_member_cost, category, cost)
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
            .clickable(role = Role.Button) { onSelect(place) }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Place,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = place.name?.takeIf { it.isNotBlank() }
                    ?: stringResource(R.string.cluster_unnamed),
                color = color,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(text = subtitle, color = SaltTheme.colors.subText, fontSize = 12.sp)
            place.address?.takeIf { it.isNotBlank() }?.let { address ->
                Text(text = address, color = SaltTheme.colors.subText, fontSize = 12.sp,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@file:OptIn(UnstableSaltUiApi::class, ExperimentalLayoutApi::class)

package cc.dinnerparty.compose.ui.view.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachIndexed
import cc.dinnerparty.compose.R
import cc.dinnerparty.compose.api.ApiClient
import cc.dinnerparty.compose.api.model.ProfileCollection
import cc.dinnerparty.compose.api.model.ProfilePlace
import cc.dinnerparty.compose.api.model.TasteAxis
import cc.dinnerparty.compose.api.model.User
import cc.dinnerparty.compose.api.model.UserProfile
import cc.dinnerparty.compose.data.model.RatingTier
import cc.dinnerparty.compose.ui.theme.dinnerPartyAccents
import cc.dinnerparty.compose.ui.view.component.UserAvatar
import cc.dinnerparty.compose.ui.view.component.YesNoContentDialog
import cc.dinnerparty.compose.ui.view.content.ratingTierLabel
import coil3.compose.AsyncImage
import com.moriafly.salt.ui.Button
import com.moriafly.salt.ui.ButtonAppearance
import com.moriafly.salt.ui.Icon
import com.moriafly.salt.ui.ItemSwitcher
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.Switcher
import com.moriafly.salt.ui.Text
import com.moriafly.salt.ui.UnstableSaltUiApi
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** 住民主页的操作 */
class ProfilePageOps(
    val onBack: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onOpenPlace: (Long) -> Unit = {},
    val edit: ProfileEditOps = ProfileEditOps(),
)

/** 住民主页的写入操作 */
class ProfileEditOps(
    val onTaste: (String, Int?) -> Unit = { _, _ -> },
    val onWidgets: (List<String>) -> Unit = {},
    val onVisibility: (Boolean) -> Unit = {},
)

/** 住民主页的数据 */
class ProfilePageUi(
    val profile: UserProfile? = null,
    val avatarVersion: Long = 0L,
    val busy: Boolean = false,
    val error: String? = null,
)

/** 住民主页全屏页 */
@Composable
fun ProfilePage(
    ui: ProfilePageUi,
    ops: ProfilePageOps,
    modifier: Modifier = Modifier,
) {
    PageScaffold(
        info = PageInfo(
            path = "",
            backText = stringResource(R.string.profile_back),
            title = stringResource(R.string.profile_page_title),
        ),
        onBack = ops.onBack,
        modifier = modifier,
    ) {
        val profile = ui.profile
        when {
            profile != null -> ProfileBody(ui = ui, profile = profile, ops = ops)
            ui.error != null -> ProfileError(
                message = ui.error,
                busy = ui.busy,
                onRetry = ops.onRetry,
            )

            else -> ProfileLoading()
        }
    }
}

/** 主页正文 */
@Composable
private fun ProfileBody(ui: ProfilePageUi, profile: UserProfile, ops: ProfilePageOps) {
    val self = profile.relation?.self == true
    val blocked = profile.relation?.blocked == true
    val axes = remember(profile.taste) { normalizedAxes(profile.taste) }
    val hidden = remember(profile.hiddenWidgets) { profile.hiddenWidgets.orEmpty() }
    var selectedKey by remember(profile.user?.id) { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf(false) }
    var draftHidden by remember(hidden) { mutableStateOf(hidden) }
    val selectedAxis = axes.firstOrNull { it.key == selectedKey }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SaltTheme.shapes.large)
            .background(SaltTheme.colors.subBackground)
            .padding(CardPad)
    ) {
        IdentityRow(user = profile.user, city = profile.city, avatarVersion = ui.avatarVersion)
        Spacer(modifier = Modifier.height(TitleGap))
        TasteRadar(
            axes = axes,
            selectedKey = selectedKey,
            onSelect = { key -> selectedKey = if (selectedKey == key) null else key },
        )
        if (self) {
            Text(
                text = stringResource(R.string.profile_taste_hint),
                modifier = Modifier.fillMaxWidth(),
                color = SaltTheme.colors.subText,
                fontSize = 12.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
        if (selectedAxis != null) {
            AxisDetail(axis = selectedAxis, busy = ui.busy, onTaste = ops.edit.onTaste)
        }
    }

    if (blocked) {
        Spacer(modifier = Modifier.height(SectionGap))
        Text(
            text = stringResource(R.string.profile_blocked_note),
            color = SaltTheme.colors.subText,
            fontSize = 13.sp,
        )
        return
    }

    Spacer(modifier = Modifier.height(SectionGap))
    if (self) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            Button(
                onClick = {
                    draftHidden = hidden
                    editing = true
                },
                text = stringResource(R.string.profile_widgets_edit),
                appearance = ButtonAppearance.Subtle,
            )
        }
        Spacer(modifier = Modifier.height(TitleGap))
    }

    val content = profile.content
    content?.collection?.firstOrNull()?.let { collection ->
        CollectionCard(
            ui = CollectionUi(
                collection = collection,
                self = self,
                isPublic = profile.collectionVisibility,
                busy = ui.busy,
            ),
            ops = CollectionOps(
                onVisibility = ops.edit.onVisibility,
                places = PlaceRowOps(
                    onOpen = ops.onOpenPlace,
                    meta = { place -> averageMeta(place) },
                ),
            ),
        )
        Spacer(modifier = Modifier.height(SectionGap))
    }

    val recent = content?.recent
    if (recent != null) {
        PlaceWidgetCard(
            title = stringResource(
                if (self) R.string.profile_widgets_recent else R.string.profile_widgets_recent_other
            ),
            empty = stringResource(R.string.profile_widgets_recent_empty),
            places = recent,
            ops = PlaceRowOps(
                onOpen = ops.onOpenPlace,
                meta = { place -> averageMeta(place) },
            ),
        )
        Spacer(modifier = Modifier.height(SectionGap))
    }

    val favorite = content?.favorite
    if (!hidden.contains(FavoriteWidget) && favorite != null) {
        PlaceWidgetCard(
            title = stringResource(R.string.profile_widgets_favorite),
            empty = stringResource(R.string.profile_widgets_favorite_empty),
            places = favorite,
            ops = PlaceRowOps(
                onOpen = ops.onOpenPlace,
                meta = { place -> favoriteMeta(place) },
            ),
        )
        Spacer(modifier = Modifier.height(SectionGap))
    }

    Spacer(modifier = Modifier.height(PagePadding))

    if (editing) {
        WidgetsDialog(
            ui = WidgetsDialogUi(hidden = draftHidden, busy = ui.busy),
            ops = WidgetsDialogOps(
                onDismiss = { editing = false },
                onChange = { visible ->
                    draftHidden = if (visible) {
                        draftHidden - FavoriteWidget
                    } else {
                        draftHidden + FavoriteWidget
                    }
                },
                onSave = {
                    editing = false
                    ops.edit.onWidgets(draftHidden)
                },
            ),
        )
    }
}

/** 头像与上次定位 */
@Composable
private fun IdentityRow(user: User?, city: String?, avatarVersion: Long) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (user != null) {
            UserAvatar(user = user, size = AvatarSize, avatarVersion = avatarVersion)
        } else {
            Box(
                modifier = Modifier
                    .size(AvatarSize)
                    .clip(CircleShape)
                    .background(SaltTheme.colors.highlight)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(
                text = user?.username ?: stringResource(R.string.profile_anonymous),
                color = SaltTheme.colors.text,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = SaltTheme.colors.subText,
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = city?.takeIf { it.isNotBlank() }
                        ?.let { stringResource(R.string.profile_city, it) }
                        ?: stringResource(R.string.profile_city_unknown),
                    color = SaltTheme.colors.subText,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** 六轴喜好雷达 */
@Composable
private fun TasteRadar(
    axes: List<TasteAxis>,
    selectedKey: String?,
    onSelect: (String?) -> Unit,
) {
    val accent = dinnerPartyAccents().primary
    val lineColor = SaltTheme.colors.stroke

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val side = maxWidth.coerceAtMost(RadarSide)
        Box(modifier = Modifier.size(side).align(Alignment.Center)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.minDimension * RadiusRatio

                fun point(index: Int, ratio: Float): Offset {
                    val angle = -PI / 2 + index * 2 * PI / AxisKeys.size
                    return Offset(
                        x = center.x + cos(angle).toFloat() * radius * ratio,
                        y = center.y + sin(angle).toFloat() * radius * ratio,
                    )
                }

                Rings.forEach { ratio ->
                    val path = Path()
                    axes.indices.forEach { index ->
                        val offset = point(index, ratio)
                        if (index == 0) path.moveTo(offset.x, offset.y)
                        else path.lineTo(offset.x, offset.y)
                    }
                    path.close()
                    drawPath(
                        path = path,
                        color = lineColor,
                        style = Stroke(
                            width = if (ratio == 0.5f) 1.2.dp.toPx() else 0.8.dp.toPx(),
                            pathEffect = if (ratio == 0.5f) {
                                PathEffect.dashPathEffect(
                                    floatArrayOf(3.dp.toPx(), 3.dp.toPx())
                                )
                            } else {
                                null
                            },
                        ),
                    )
                }

                axes.fastForEachIndexed { index, axis ->
                    val next = axes[(index + 1) % axes.size]
                    val start = axisRatio(axis) ?: return@fastForEachIndexed
                    val end = axisRatio(next) ?: return@fastForEachIndexed
                    val first = point(index, start)
                    val second = point((index + 1) % axes.size, end)
                    val triangle = Path().apply {
                        moveTo(center.x, center.y)
                        lineTo(first.x, first.y)
                        lineTo(second.x, second.y)
                        close()
                    }
                    drawPath(path = triangle, color = accent.copy(alpha = 0.2f))
                    drawLine(
                        color = accent.copy(alpha = 0.65f),
                        start = first,
                        end = second,
                        strokeWidth = 2.5.dp.toPx(),
                    )
                }

                axes.fastForEachIndexed { index, axis ->
                    val ratio = axisRatio(axis) ?: return@fastForEachIndexed
                    val offset = point(index, ratio)
                    drawCircle(color = accent, radius = 3.5.dp.toPx(), center = offset)
                }
            }

            Layout(
                content = {
                    axes.forEach { axis ->
                        val key = axis.key.orEmpty()
                        val active = key == selectedKey
                        Text(
                            text = axisLabel(key),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { onSelect(if (active) null else key) }
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            color = if (active) accent else SaltTheme.colors.text,
                            fontSize = 15.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        )
                    }
                },
                modifier = Modifier.matchParentSize(),
            ) { measurables, constraints ->
                val width = constraints.maxWidth
                val height = constraints.maxHeight
                val labelRadius = minOf(width, height) * LabelRadiusRatio
                val placeables = measurables.map { it.measure(Constraints()) }
                layout(width, height) {
                    placeables.fastForEachIndexed { index, placeable ->
                        val angle = -PI / 2 + index * 2 * PI / AxisKeys.size
                        val x = width / 2f + cos(angle).toFloat() * labelRadius
                        val y = height / 2f + sin(angle).toFloat() * labelRadius
                        placeable.place(
                            x = (x - placeable.width / 2f).roundToInt(),
                            y = (y - placeable.height / 2f).roundToInt(),
                        )
                    }
                }
            }
        }
    }
}

/** 选中轴的数值与手动喜好 */
@Composable
private fun AxisDetail(axis: TasteAxis, busy: Boolean, onTaste: (String, Int?) -> Unit) {
    val key = axis.key.orEmpty()
    Spacer(modifier = Modifier.height(TitleGap))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = axisLabel(key),
            color = SaltTheme.colors.text,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = axisValueWord(axis),
            color = SaltTheme.colors.subText,
            fontSize = 13.sp,
        )
    }
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        text = stringResource(R.string.profile_taste_self_title),
        color = SaltTheme.colors.subText,
        fontSize = 12.sp,
    )
    Spacer(modifier = Modifier.height(6.dp))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        SelfLevels.forEach { level ->
            LevelChip(
                text = selfLevelLabel(level),
                selected = axis.selfLevel == level,
                enabled = !busy,
                onClick = { onTaste(key, level) },
            )
        }
        LevelChip(
            text = stringResource(R.string.profile_taste_auto),
            selected = axis.selfLevel == null,
            enabled = !busy,
            onClick = { onTaste(key, null) },
        )
    }
}

/** 喜好档位按钮 */
@Composable
private fun LevelChip(text: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val accent = dinnerPartyAccents().primary
    val shape = CircleShape
    Box(
        modifier = Modifier
            .clip(shape)
            .background(if (selected) accent.copy(alpha = 0.12f) else SaltTheme.colors.popup)
            .border(1.dp, if (selected) accent else SaltTheme.colors.stroke, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(
            text = text,
            color = if (selected) accent else SaltTheme.colors.text,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

/** 收藏夹卡片 */
@Composable
private fun CollectionCard(ui: CollectionUi, ops: CollectionOps) {
    val collection = ui.collection
    val total = collection.total ?: collection.places.orEmpty().size
    val title = if (ui.self && ui.isPublic == false) {
        stringResource(R.string.profile_widgets_collection_mine)
    } else {
        stringResource(R.string.profile_widgets_collection)
    }
    val subtitle = if (ui.self) {
        stringResource(
            R.string.profile_widgets_collection_count,
            stringResource(R.string.profile_widgets_collection_mine),
            total,
        )
    } else {
        stringResource(R.string.profile_widgets_places_count, total)
    }
    val rows = collection.places.orEmpty()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SaltTheme.shapes.large)
            .background(SaltTheme.colors.subBackground)
            .padding(vertical = WidgetPadY),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = CardPad),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = SaltTheme.colors.text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = SaltTheme.colors.subText,
                    fontSize = 12.sp,
                )
            }
            if (ui.self && ui.isPublic != null) {
                Text(
                    text = stringResource(R.string.profile_widgets_public),
                    color = SaltTheme.colors.subText,
                    fontSize = 12.sp,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Switcher(
                    state = ui.isPublic,
                    onChange = { if (!ui.busy) ops.onVisibility(it) },
                )
            }
        }
        Spacer(modifier = Modifier.height(TitleGap))
        if (rows.isEmpty()) {
            Text(
                text = stringResource(R.string.profile_widgets_collection_empty),
                modifier = Modifier.padding(horizontal = CardPad),
                color = SaltTheme.colors.subText,
                fontSize = 13.sp,
            )
        } else {
            PlaceRow(places = rows, ops = ops.places)
        }
    }
}

/** 地点预览卡片 */
@Composable
private fun PlaceWidgetCard(
    title: String,
    empty: String,
    places: List<ProfilePlace>,
    ops: PlaceRowOps,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SaltTheme.shapes.large)
            .background(SaltTheme.colors.subBackground)
            .padding(vertical = WidgetPadY),
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = CardPad),
            color = SaltTheme.colors.text,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(TitleGap))
        if (places.isEmpty()) {
            Text(
                text = empty,
                modifier = Modifier.padding(horizontal = CardPad),
                color = SaltTheme.colors.subText,
                fontSize = 13.sp,
            )
        } else {
            PlaceRow(places = places, ops = ops)
        }
    }
}

/** 横向排列的地点缩略图 */
@Composable
private fun PlaceRow(places: List<ProfilePlace>, ops: PlaceRowOps) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = CardPad),
    ) {
        items(places) { place ->
            PlaceTile(place = place, onOpen = ops.onOpen, meta = ops.meta)
        }
    }
}

/** 单个地点缩略图 */
@Composable
private fun PlaceTile(
    place: ProfilePlace,
    onOpen: (Long) -> Unit,
    meta: @Composable (ProfilePlace) -> String,
) {
    val id = place.id
    Column(
        modifier = Modifier
            .width(TileWidth)
            .clickable(enabled = id != null) { id?.let(onOpen) },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(TileHeight)
                .clip(SaltTheme.shapes.medium)
                .background(SaltTheme.colors.popup),
            contentAlignment = Alignment.Center,
        ) {
            val image = place.image
            if (image.isNullOrBlank()) {
                Icon(
                    imageVector = Icons.Outlined.Restaurant,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = SaltTheme.colors.subText,
                )
            } else {
                AsyncImage(
                    model = ApiClient.assetUrl(image),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                )
            }
        }
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = place.name.orEmpty(),
            color = SaltTheme.colors.text,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        val metaText = meta(place)
        if (metaText.isNotEmpty()) {
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = metaText,
                color = SaltTheme.colors.subText,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** 小组件调整弹窗 */
@Composable
private fun WidgetsDialog(ui: WidgetsDialogUi, ops: WidgetsDialogOps) {
    YesNoContentDialog(
        title = stringResource(R.string.profile_widgets_title),
        onDismissRequest = ops.onDismiss,
        onConfirm = ops.onSave,
        enabled = !ui.busy,
    ) {
        ItemSwitcher(
            state = true,
            onChange = {},
            text = stringResource(R.string.profile_widgets_collection_mine),
            enabled = false,
            sub = stringResource(R.string.profile_widgets_keep),
        )
        ItemSwitcher(
            state = true,
            onChange = {},
            text = stringResource(R.string.profile_widgets_recent),
            enabled = false,
            sub = stringResource(R.string.profile_widgets_keep),
        )
        ItemSwitcher(
            state = !ui.hidden.contains(FavoriteWidget),
            onChange = ops.onChange,
            text = stringResource(R.string.profile_widgets_favorite),
            enabled = !ui.busy,
        )
    }
}

/** 加载中 */
@Composable
private fun ProfileLoading() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = LoadingOffset),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.profile_loading),
            color = SaltTheme.colors.subText,
            fontSize = 13.sp,
        )
    }
}

/** 首次加载失败 */
@Composable
private fun ProfileError(message: String, busy: Boolean, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = LoadingOffset),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            color = SaltTheme.colors.subText,
            fontSize = 13.sp,
        )
        Spacer(modifier = Modifier.height(TitleGap))
        Button(
            onClick = onRetry,
            text = stringResource(R.string.profile_retry),
            enabled = !busy,
        )
    }
}

/** 地点行的跳转与副标题 */
private class PlaceRowOps(
    val onOpen: (Long) -> Unit,
    val meta: @Composable (ProfilePlace) -> String,
)

/** 收藏夹卡片数据 */
private class CollectionUi(
    val collection: ProfileCollection,
    val self: Boolean,
    val isPublic: Boolean?,
    val busy: Boolean,
)

/** 收藏夹卡片操作 */
private class CollectionOps(
    val onVisibility: (Boolean) -> Unit,
    val places: PlaceRowOps,
)

/** 小组件弹窗状态 */
private class WidgetsDialogUi(val hidden: List<String>, val busy: Boolean)

/** 小组件弹窗操作 */
private class WidgetsDialogOps(
    val onDismiss: () -> Unit,
    val onChange: (Boolean) -> Unit,
    val onSave: () -> Unit,
)

private val AxisKeys = listOf("homestyle", "noodles", "hotpotBbq", "dessert", "drinks", "foreign")

private val Rings = listOf(0.25f, 0.5f, 0.75f, 1f)

private val SelfLevels = listOf(4, 3, 2, 1)

private const val FavoriteWidget = "favorite"

private val RadarSide = 300.dp

private const val RadiusRatio = 80f / 300f

private const val LabelRadiusRatio = 102f / 300f

private val AvatarSize = 72.dp

private val CardPad = 16.dp

private val WidgetPadY = 14.dp

private val TitleGap = 10.dp

private val SectionGap = 20.dp

private val TileWidth = 112.dp

private val TileHeight = 84.dp

private val LoadingOffset = 64.dp

/** 补齐六轴 缺的轴按待了解处理 */
private fun normalizedAxes(list: List<TasteAxis>?): List<TasteAxis> {
    val byKey = list.orEmpty().associateBy { it.key }
    return AxisKeys.map { key -> byKey[key] ?: TasteAxis(key = key) }
}

/** 轴在雷达上的半径比例 */
private fun axisRatio(axis: TasteAxis): Float? {
    val value = axis.value ?: return null
    if (!value.isFinite()) return null
    return value.coerceIn(0.0, 1.0).toFloat()
}

/** 轴名 */
@Composable
private fun axisLabel(key: String): String = when (key) {
    "homestyle" -> stringResource(R.string.profile_taste_axis_homestyle)
    "noodles" -> stringResource(R.string.profile_taste_axis_noodles)
    "hotpotBbq" -> stringResource(R.string.profile_taste_axis_hotpot_bbq)
    "dessert" -> stringResource(R.string.profile_taste_axis_dessert)
    "drinks" -> stringResource(R.string.profile_taste_axis_drinks)
    "foreign" -> stringResource(R.string.profile_taste_axis_foreign)
    else -> key
}

/** 自己填的档位文案 */
@Composable
private fun selfLevelLabel(level: Int): String = when (level) {
    4 -> stringResource(R.string.profile_taste_love)
    3 -> stringResource(R.string.profile_taste_like)
    2 -> stringResource(R.string.profile_taste_neutral)
    else -> stringResource(R.string.profile_taste_dislike)
}

/** 轴值的简短说法 */
@Composable
private fun axisValueWord(axis: TasteAxis): String {
    val level = axis.selfLevel
    if (axis.basis == "self" && level != null) return selfLevelLabel(level)
    val value = axis.value
    if (value == null || !value.isFinite()) {
        return if (axis.unavailableReason == "service_unavailable") {
            stringResource(R.string.profile_taste_unavailable)
        } else {
            stringResource(R.string.profile_taste_unknown)
        }
    }
    return when {
        value < 0.6 -> stringResource(R.string.profile_taste_neutral)
        value >= 0.8 -> stringResource(R.string.profile_taste_love)
        else -> stringResource(R.string.profile_taste_like)
    }
}

/** 集合与最近地点用的档位加平均分 */
@Composable
private fun averageMeta(place: ProfilePlace): String {
    val average = place.ratingAverage
    val tier = if ((place.ratingCount ?: 0) > 0) RatingTier.forAverage(average) else null
    val label = ratingTierLabel(tier)
    return when {
        label != null && average != null -> stringResource(
            R.string.profile_place_rating,
            label,
            String.format(Locale.US, "%.1f", average),
        )

        !place.category.isNullOrBlank() -> place.category.orEmpty()
        else -> ""
    }
}

/** 最喜欢卡片只显示自己的档位 */
@Composable
private fun favoriteMeta(place: ProfilePlace): String =
    ratingTierLabel(RatingTier.forAverage(place.ownScore)).orEmpty()

package cc.dinnerparty.compose.ui.view.content

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsBike
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.TripOrigin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.dinnerparty.compose.R
import cc.dinnerparty.compose.api.model.Place
import cc.dinnerparty.compose.data.model.AlongRouteMode
import cc.dinnerparty.compose.data.model.AlongRoutePlan
import cc.dinnerparty.compose.data.model.RouteEndpoint
import cc.dinnerparty.compose.ui.viewmodel.AlongRouteStep
import cc.dinnerparty.compose.ui.viewmodel.AlongRouteUiState
import cc.dinnerparty.compose.ui.viewmodel.RouteTarget
import com.moriafly.salt.ui.Button
import com.moriafly.salt.ui.ButtonAppearance
import com.moriafly.salt.ui.Icon
import com.moriafly.salt.ui.ItemEdit
import com.moriafly.salt.ui.RoundedColumn
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.Text
import com.moriafly.salt.ui.UnstableSaltUiApi
import java.util.Locale
import kotlin.math.roundToInt

/** 顺路吃的交互回调 */
class AlongRouteOps(
    val onPick: (RouteTarget) -> Unit = {},
    val onCancelPick: () -> Unit = {},
    val onEdit: (RouteTarget) -> Unit = {},
    val onStopEdit: (RouteTarget) -> Unit = {},
    val onInput: (String) -> Unit = {},
    val onTip: (RouteEndpoint) -> Unit = {},
    val onNext: () -> Unit = {},
    val onBack: () -> Unit = {},
    val onMode: (AlongRouteMode) -> Unit = {},
    val onQuery: (String) -> Unit = {},
    val onTagInput: (String) -> Unit = {},
    val onToggleCategory: (String) -> Unit = {},
    val onCorridor: (Int) -> Unit = {},
    val onPlan: () -> Unit = {},
    val onChooseRoute: (Int) -> Unit = {},
    val onOpenPlace: (Long) -> Unit = {},
    val onRetry: () -> Unit = {},
)

/** 顺路吃面板 先选起终点 再筛沿线 */
@OptIn(UnstableSaltUiApi::class, ExperimentalLayoutApi::class)
@Composable
fun AlongRouteContent(
    state: AlongRouteUiState,
    ops: AlongRouteOps,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(ContentPadding)
    ) {
        AlongRouteHeader(state, onClose, ops.onCancelPick)
        Spacer(modifier = Modifier.height(StepGap))
        when (state.step) {
            AlongRouteStep.Endpoints -> EndpointStep(state, ops)
            AlongRouteStep.Options -> OptionsStep(state, ops)
            AlongRouteStep.Routes -> RoutesStep(state, ops)
        }
    }
}

/** 第一步 只输入或选取起终点 */
@Composable
private fun EndpointStep(state: AlongRouteUiState, ops: AlongRouteOps) {
    val focus = LocalFocusManager.current
    val ready = state.origin != null && state.destination != null
    Column(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(SaltTheme.shapes.medium)
                .background(SaltTheme.colors.subBackground)
        ) {
            EndpointInput(
                EndpointSpec(
                    target = RouteTarget.Origin,
                    icon = Icons.Outlined.TripOrigin,
                    label = stringResource(R.string.along_route_origin),
                    value = state.origin,
                    hint = stringResource(R.string.along_route_origin_hint),
                    editing = state.editing == RouteTarget.Origin,
                    picking = state.picking == RouteTarget.Origin,
                    input = state.input,
                    ops = ops,
                )
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = EndpointLineInset)
                    .height(1.dp)
                    .background(SaltTheme.colors.stroke)
            )
            EndpointInput(
                EndpointSpec(
                    target = RouteTarget.Destination,
                    icon = Icons.Outlined.Place,
                    label = stringResource(R.string.along_route_destination),
                    value = state.destination,
                    hint = stringResource(R.string.along_route_destination_hint),
                    editing = state.editing == RouteTarget.Destination,
                    picking = state.picking == RouteTarget.Destination,
                    input = state.input,
                    ops = ops,
                )
            )
        }
        if (state.tips.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            TipList(
                tips = state.tips,
                onTip = { tip ->
                    ops.onTip(tip)
                    focus.clearFocus()
                },
            )
        }
        Spacer(modifier = Modifier.height(StepGap))
        Button(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.along_route_next),
            enabled = ready,
            onClick = {
                focus.clearFocus()
                ops.onNext()
            },
        )
        state.error?.let { message ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = message, color = SaltTheme.colors.error, fontSize = 13.sp)
        }
    }
}

/** 第二步 已选起终点只读展示 点一下回到第一步 */
@Composable
private fun OptionsStep(state: AlongRouteUiState, ops: AlongRouteOps) {
    Column(modifier = Modifier.fillMaxWidth()) {
        EndpointSummary(state, ops.onBack)
        Spacer(modifier = Modifier.height(StepGap))
        ModeRow(state.mode, ops.onMode)
        Spacer(modifier = Modifier.height(StepGap))
        QueryField(state.query, ops.onQuery)
        Spacer(modifier = Modifier.height(StepGap))
        CategorySection(state, ops)
        Spacer(modifier = Modifier.height(StepGap))
        CorridorRow(state.corridorMeters, ops.onCorridor)
        Spacer(modifier = Modifier.height(StepGap))
        PlanButton(state, ops.onPlan)
        state.error?.let { message ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = message, color = SaltTheme.colors.error, fontSize = 13.sp)
        }
    }
}

/** 第三步 方案横向排列 下方是结果与回退 整体固定高度内滚动 */
@Composable
private fun RoutesStep(state: AlongRouteUiState, ops: AlongRouteOps) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(LocalConfiguration.current.screenHeightDp.dp * RoutesHeightRatio)
            .verticalScroll(rememberScrollState())
    ) {
        if (state.routes.isNotEmpty()) {
            RouteOptions(state.routes, state.chosenRoute, ops.onChooseRoute)
            Spacer(modifier = Modifier.height(StepGap))
        }
        Button(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.along_route_prev_step),
            appearance = ButtonAppearance.Subtle,
            onClick = ops.onBack,
        )
        if (state.busy) {
            Spacer(modifier = Modifier.height(StepGap))
            Text(
                text = stringResource(R.string.along_route_busy),
                color = SaltTheme.colors.subText,
                fontSize = 13.sp,
            )
        }
        state.error?.let { message ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = message, color = SaltTheme.colors.error, fontSize = 13.sp)
        }
        state.places?.let { places ->
            Spacer(modifier = Modifier.height(StepGap))
            ResultSection(
                places = places,
                busy = state.busy,
                onOpenPlace = ops.onOpenPlace,
                onRetry = ops.onRetry,
            )
        }
    }
}

/** 起终点的只读摘要 */
@Composable
private fun EndpointSummary(state: AlongRouteUiState, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SaltTheme.shapes.medium)
            .background(SaltTheme.colors.subBackground)
            .clickable(role = Role.Button, onClick = onBack)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            SummaryRow(
                icon = Icons.Outlined.TripOrigin,
                label = stringResource(R.string.along_route_origin),
                value = state.origin?.name,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = EndpointIconInset, top = 6.dp, bottom = 6.dp)
                    .height(1.dp)
                    .background(SaltTheme.colors.stroke)
            )
            SummaryRow(
                icon = Icons.Outlined.Place,
                label = stringResource(R.string.along_route_destination),
                value = state.destination?.name,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = Icons.Outlined.Edit,
            contentDescription = stringResource(R.string.along_route_edit_endpoints),
            modifier = Modifier.size(18.dp),
            tint = SaltTheme.colors.subText,
        )
    }
}

@Composable
private fun SummaryRow(icon: ImageVector, label: String, value: String?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = SaltTheme.colors.subText,
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, color = SaltTheme.colors.subText, fontSize = 11.sp)
            Text(
                text = value.orEmpty(),
                color = SaltTheme.colors.text,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** 端点输入行的展示信息 */
private class EndpointSpec(
    val target: RouteTarget,
    val icon: ImageVector,
    val label: String,
    val value: RouteEndpoint?,
    val hint: String,
    val editing: Boolean,
    val picking: Boolean,
    val input: String,
    val ops: AlongRouteOps,
)

/** 左侧图标 中间输入框 右侧地图取点 */
@OptIn(UnstableSaltUiApi::class)
@Composable
private fun EndpointInput(spec: EndpointSpec) {
    val focus = LocalFocusManager.current
    val active = spec.editing || spec.picking
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = spec.icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = if (active) SaltTheme.colors.highlight else SaltTheme.colors.subText,
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = spec.label, color = SaltTheme.colors.subText, fontSize = 11.sp)
            EndpointField(spec)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(MapButtonSize)
                .clip(CircleShape)
                .clickable {
                    focus.clearFocus()
                    spec.ops.onPick(spec.target)
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Map,
                contentDescription = stringResource(R.string.along_route_pick_on_map),
                modifier = Modifier.size(18.dp),
                tint = if (spec.picking) SaltTheme.colors.highlight else SaltTheme.colors.subText,
            )
        }
    }
}

/** 端点名称输入框 未输入时显示提示 */
@Composable
private fun EndpointField(spec: EndpointSpec) {
    val shown = if (spec.editing) spec.input else spec.value?.name.orEmpty()
    BasicTextField(
        value = shown,
        onValueChange = spec.ops.onInput,
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { event ->
                if (event.isFocused) spec.ops.onEdit(spec.target) else spec.ops.onStopEdit(spec.target)
            },
        textStyle = SaltTheme.textStyles.main.copy(
            color = SaltTheme.colors.text,
            fontSize = 15.sp,
        ),
        singleLine = true,
        cursorBrush = SolidColor(SaltTheme.colors.highlight),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        decorationBox = { innerTextField ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (shown.isEmpty()) {
                    Text(
                        text = spec.hint,
                        color = SaltTheme.colors.subText,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                innerTextField()
            }
        },
    )
}

/** 高德输入提示的候选 */
@Composable
private fun TipList(tips: List<RouteEndpoint>, onTip: (RouteEndpoint) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        tips.forEach { tip ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(TipShape)
                    .clickable(role = Role.Button) { onTip(tip) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Place,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = SaltTheme.colors.subText,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = tip.name,
                    color = SaltTheme.colors.text,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** 标题与当前状态 */
@OptIn(UnstableSaltUiApi::class)
@Composable
private fun AlongRouteHeader(
    state: AlongRouteUiState,
    onClose: () -> Unit,
    onCancelPick: () -> Unit,
) {
    val subtitle = when (state.picking) {
        RouteTarget.Origin -> stringResource(R.string.along_route_picking_origin)
        RouteTarget.Destination -> stringResource(R.string.along_route_picking_destination)
        null -> stringResource(R.string.along_route_subtitle)
    }
    Row(verticalAlignment = Alignment.Top) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.along_route_title),
                modifier = Modifier.semantics { heading() },
                color = SaltTheme.colors.text,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = SaltTheme.colors.subText,
                fontSize = 12.sp,
            )
        }
        if (state.picking != null) {
            Text(
                text = stringResource(R.string.along_route_cancel_pick),
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .clickable(onClick = onCancelPick)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                color = SaltTheme.colors.highlight,
                fontSize = 13.sp,
            )
        }
        Box(
            modifier = Modifier
                .size(CloseSize)
                .clip(CircleShape)
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = stringResource(R.string.along_route_close),
                modifier = Modifier.size(18.dp),
                tint = SaltTheme.colors.subText,
            )
        }
    }
}

/** 四种出行方式 */
@Composable
private fun ModeRow(mode: AlongRouteMode, onMode: (AlongRouteMode) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        AlongRouteMode.entries.forEach { item ->
            ModeChip(
                mode = item,
                selected = item == mode,
                onClick = { onMode(item) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ModeChip(
    mode: AlongRouteMode,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(ChipShape)
            .background(if (selected) SaltTheme.colors.highlight.copy(alpha = ChipToneAlpha) else Color.Transparent)
            .border(
                1.dp,
                if (selected) SaltTheme.colors.highlight else SaltTheme.colors.stroke,
                ChipShape,
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = modeIcon(mode),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = if (selected) SaltTheme.colors.highlight else SaltTheme.colors.subText,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = modeLabel(mode),
            color = if (selected) SaltTheme.colors.highlight else SaltTheme.colors.text,
            fontSize = 11.sp,
            maxLines = 1,
        )
    }
}

/** 想吃什么 */
@OptIn(UnstableSaltUiApi::class)
@Composable
private fun QueryField(value: String, onChange: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionLabel(stringResource(R.string.along_route_want))
        RoundedColumn(
            modifier = Modifier.fillMaxWidth(),
            paddingValues = PaddingValues(0.dp),
            color = Color.Transparent,
        ) {
            ItemEdit(
                text = value,
                onChange = onChange,
                hint = stringResource(R.string.along_route_want_hint),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            )
        }
    }
}

/** 已选分类与可选分类 */
@OptIn(UnstableSaltUiApi::class, ExperimentalLayoutApi::class)
@Composable
private fun CategorySection(state: AlongRouteUiState, ops: AlongRouteOps) {
    val names = remember(state.categories) {
        state.categories.mapNotNull { it.name?.takeIf { name -> name.isNotBlank() } }.distinct()
    }
    val common = remember(state.categories) {
        state.categories.filter { it.isCommon }.mapNotNull { it.name }.toSet()
    }
    val input = state.tagInput.trim()
    val suggestions = remember(names, common, input, state.chosenCategories) {
        val pool = if (input.isEmpty()) names.filter { it in common } else names.filter { it.contains(input) }
        pool.filter { it !in state.chosenCategories }.take(SuggestionLimit)
    }
    val chosen = names.filter { it in state.chosenCategories }

    Column(modifier = Modifier.fillMaxWidth()) {
        SectionLabel(stringResource(R.string.along_route_tags))
        RoundedColumn(
            modifier = Modifier.fillMaxWidth(),
            paddingValues = PaddingValues(0.dp),
            color = Color.Transparent,
        ) {
            ItemEdit(
                text = state.tagInput,
                onChange = ops.onTagInput,
                hint = stringResource(R.string.along_route_tag_search),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            )
        }
        if (chosen.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.along_route_tags_selected),
                color = SaltTheme.colors.subText,
                fontSize = 12.sp,
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                chosen.forEach { name ->
                    SelectedTag(name = name, onClick = { ops.onToggleCategory(name) })
                }
            }
        }
        if (suggestions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(
                    if (input.isEmpty()) R.string.along_route_tags_common else R.string.along_route_tags_matched
                ),
                color = SaltTheme.colors.subText,
                fontSize = 12.sp,
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                suggestions.forEach { name ->
                    SuggestTag(name = name, onClick = { ops.onToggleCategory(name) })
                }
            }
        } else if (input.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.along_route_tags_empty),
                color = SaltTheme.colors.subText,
                fontSize = 11.sp,
            )
        }
    }
}

/** 离路线的最大距离 */
@Composable
private fun CorridorRow(meters: Int, onCorridor: (Int) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionLabel(stringResource(R.string.along_route_corridor))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CorridorOptions.forEach { option ->
                CorridorChip(
                    meters = option,
                    selected = option == meters,
                    onClick = { onCorridor(option) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun CorridorChip(
    meters: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(ChipShape)
            .background(if (selected) SaltTheme.colors.highlight.copy(alpha = ChipToneAlpha) else Color.Transparent)
            .border(
                1.dp,
                if (selected) SaltTheme.colors.highlight else SaltTheme.colors.stroke,
                ChipShape,
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.along_route_corridor_value, meters),
            color = if (selected) SaltTheme.colors.highlight else SaltTheme.colors.text,
            fontSize = 12.sp,
            maxLines = 1,
        )
    }
}

/** 规划路线或重新检索 */
@Composable
private fun PlanButton(state: AlongRouteUiState, onPlan: () -> Unit) {
    val enabled = !state.busy && state.origin != null && state.destination != null
    val label = when {
        state.busy -> stringResource(R.string.along_route_busy)
        state.chosenRoute != null -> stringResource(R.string.along_route_search)
        else -> stringResource(R.string.along_route_plan)
    }
    Button(
        modifier = Modifier.fillMaxWidth(),
        text = label,
        enabled = enabled,
        onClick = onPlan,
    )
}

/** 多条方案时横向排列 左右滑动选择 */
@Composable
private fun RouteOptions(routes: List<AlongRoutePlan>, chosen: Int?, onChoose: (Int) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionLabel(stringResource(R.string.along_route_choose_route, routes.size))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            routes.forEachIndexed { position, plan ->
                RouteOptionCard(
                    plan = plan,
                    selected = position == chosen,
                    onClick = { onChoose(position) },
                )
            }
        }
    }
}

@Composable
private fun RouteOptionCard(
    plan: AlongRoutePlan,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val optionShape = SaltTheme.shapes.medium
    Column(
        modifier = Modifier
            .width(RouteCardWidth)
            .clip(optionShape)
            .background(if (selected) SaltTheme.colors.highlight.copy(alpha = ChipToneAlpha) else Color.Transparent)
            .border(
                1.dp,
                if (selected) SaltTheme.colors.highlight else SaltTheme.colors.stroke,
                optionShape,
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.along_route_route_label, plan.index + 1),
                modifier = Modifier.weight(1f),
                color = SaltTheme.colors.text,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            if (selected) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = SaltTheme.colors.highlight,
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(
                R.string.along_route_route_detail,
                distanceText(plan.distanceMeters),
                durationText(plan.durationSeconds),
            ),
            color = SaltTheme.colors.subText,
            fontSize = 12.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 沿线检索到的地点 */
@Composable
private fun ResultSection(
    places: List<Place>,
    busy: Boolean,
    onOpenPlace: (Long) -> Unit,
    onRetry: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (!busy && places.isNotEmpty()) {
            Text(
                text = stringResource(R.string.along_route_results, places.size),
                color = SaltTheme.colors.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (places.isEmpty()) {
            Text(
                text = stringResource(R.string.along_route_results_empty),
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                color = SaltTheme.colors.subText,
                fontSize = 13.sp,
            )
            Button(
                onClick = onRetry,
                text = stringResource(R.string.along_route_retry),
                appearance = ButtonAppearance.Subtle,
            )
            return@Column
        }
        places.forEachIndexed { position, place ->
            AlongRoutePlaceRow(
                place = place,
                position = position + 1,
                onOpen = onOpenPlace,
            )
        }
    }
}

@Composable
private fun AlongRoutePlaceRow(place: Place, position: Int, onOpen: (Long) -> Unit) {
    val id = place.id
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(enabled = id != null, role = Role.Button) { id?.let(onOpen) }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(RankSize)
                .clip(CircleShape)
                .background(SaltTheme.colors.highlight.copy(alpha = ChipToneAlpha)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = position.toString(),
                color = SaltTheme.colors.highlight,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = place.name?.takeIf { it.isNotBlank() }
                    ?: stringResource(R.string.cluster_unnamed),
                color = SaltTheme.colors.text,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = placeMeta(place),
                color = SaltTheme.colors.subText,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = Icons.Outlined.Map,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = SaltTheme.colors.subText,
        )
    }
}

/** 已选分类 点一下取消 */
@Composable
private fun SelectedTag(name: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(ChipShape)
            .background(SaltTheme.colors.highlight.copy(alpha = ChipToneAlpha))
            .border(1.dp, SaltTheme.colors.highlight, ChipShape)
            .clickable(onClick = onClick)
            .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = name,
            color = SaltTheme.colors.highlight,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = stringResource(R.string.along_route_remove_tag),
                modifier = Modifier.size(14.dp),
                tint = SaltTheme.colors.highlight,
            )
        }
    }
}

/** 可选分类 */
@Composable
private fun SuggestTag(name: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(ChipShape)
            .border(1.dp, SaltTheme.colors.stroke, ChipShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Add,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = SaltTheme.colors.subText,
        )
        Text(
            text = name,
            modifier = Modifier.padding(start = 4.dp),
            color = SaltTheme.colors.text,
            fontSize = 13.sp,
            maxLines = 1,
        )
    }
}

/** 段落标题 */
@Composable
private fun SectionLabel(text: String) {
    Text(text = text, color = SaltTheme.colors.subText, fontSize = 13.sp)
    Spacer(modifier = Modifier.height(6.dp))
}

/** 距离文案 不足一公里时按米显示 */
@Composable
private fun distanceText(meters: Float): String =
    if (meters < 1000f) {
        stringResource(R.string.along_route_meter, meters.roundToInt())
    } else {
        stringResource(R.string.along_route_km, String.format(Locale.US, "%.1f", meters / 1000f))
    }

/** 时长文案 超过一小时按小时与分钟显示 */
@Composable
private fun durationText(seconds: Long): String {
    val minutes = (seconds / 60f).roundToInt()
    return if (minutes < 60) {
        stringResource(R.string.along_route_minute, minutes)
    } else {
        stringResource(R.string.along_route_hour_minute, minutes / 60, minutes % 60)
    }
}

/** 结果行的地方文案 含分类 人均与离路线距离 */
@Composable
private fun placeMeta(place: Place): String {
    val parts = buildList {
        add(
            place.category?.takeIf { it.isNotBlank() }
                ?: stringResource(R.string.cluster_uncategorized)
        )
        place.perPersonCost?.let { add(stringResource(R.string.along_route_cost, it)) }
        place.distanceToRoute?.let { add(stringResource(R.string.along_route_off_route, it.roundToInt())) }
    }
    return parts.joinToString(Separator)
}

@Composable
private fun modeLabel(mode: AlongRouteMode): String = stringResource(
    when (mode) {
        AlongRouteMode.Driving -> R.string.along_route_mode_driving
        AlongRouteMode.Transit -> R.string.along_route_mode_transit
        AlongRouteMode.Walking -> R.string.along_route_mode_walking
        AlongRouteMode.Riding -> R.string.along_route_mode_riding
    }
)

private fun modeIcon(mode: AlongRouteMode): ImageVector = when (mode) {
    AlongRouteMode.Driving -> Icons.Outlined.DirectionsCar
    AlongRouteMode.Transit -> Icons.Outlined.DirectionsBus
    AlongRouteMode.Walking -> Icons.AutoMirrored.Outlined.DirectionsWalk
    AlongRouteMode.Riding -> Icons.AutoMirrored.Outlined.DirectionsBike
}

private val CorridorOptions = listOf(500, 1000, 2000, 3000)

private val ContentPadding = 16.dp
private val StepGap = 12.dp
private val CloseSize = 24.dp
private val RankSize = 30.dp
private val MapButtonSize = 32.dp
private val RouteCardWidth = 120.dp
private val ChipShape = RoundedCornerShape(50)
private val TipShape = RoundedCornerShape(8.dp)
private val EndpointLineInset = 40.dp
private val EndpointIconInset = 28.dp
private const val ChipToneAlpha = 0.16f
private const val RoutesHeightRatio = 0.4f
private const val SuggestionLimit = 12
private const val Separator = " · "

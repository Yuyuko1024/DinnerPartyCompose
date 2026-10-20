package cc.dinnerparty.compose.ui.view.component

import android.widget.Toast
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.dinnerparty.compose.R
import cc.dinnerparty.compose.api.ApiClient
import cc.dinnerparty.compose.data.model.RatingTier
import cc.dinnerparty.compose.ui.theme.dinnerPartySemanticColors
import cc.dinnerparty.compose.ui.viewmodel.PlaceFormMode
import cc.dinnerparty.compose.ui.viewmodel.PlaceFormState
import cc.dinnerparty.compose.ui.viewmodel.PlaceImageKind
import cc.dinnerparty.compose.utils.decodeImage
import cc.dinnerparty.compose.utils.toJpegBytes
import coil3.compose.AsyncImage
import com.moriafly.salt.ui.Button
import com.moriafly.salt.ui.ButtonType
import com.moriafly.salt.ui.Icon
import com.moriafly.salt.ui.ItemEdit
import com.moriafly.salt.ui.RoundedColumn
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.Text
import com.moriafly.salt.ui.UnstableSaltUiApi
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 表单交互回调。 */
class PlaceFormEvents(
    val onName: (String) -> Unit = {},
    val onDesc: (String) -> Unit = {},
    val onCost: (String) -> Unit = {},
    val onTagInput: (String) -> Unit = {},
    val onAddTag: (String) -> Unit = {},
    val onRemoveTag: (String) -> Unit = {},
    val onRating: (Int) -> Unit = {},
    val onUpload: (PlaceImageKind, ByteArray) -> Unit = { _, _ -> },
    val onRemoveImage: (PlaceImageKind, String) -> Unit = { _, _ -> },
    val onSubmit: () -> Unit = {},
    val onClose: () -> Unit = {},
)

/** 表单状态与回调的组合。 */
data class PlaceFormUi(
    val state: PlaceFormState = PlaceFormState(),
    val events: PlaceFormEvents = PlaceFormEvents(),
)

/** 手机端表单外层，限制高度后可滚动。 */
@UnstableSaltUiApi
@Composable
fun PlaceFormSheet(
    state: PlaceFormState,
    events: PlaceFormEvents,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight * SheetMaxRatio)
                .verticalScroll(rememberScrollState())
        ) {
            PlaceFormContent(state = state, events = events)
        }
    }
}

/** 地点表单内容。 */
@OptIn(UnstableSaltUiApi::class, ExperimentalLayoutApi::class)
@Composable
fun PlaceFormContent(
    state: PlaceFormState,
    events: PlaceFormEvents,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pickKind by remember { mutableStateOf(PlaceImageKind.Exterior) }

    val upload: (PlaceImageKind, Uri) -> Unit = { kind, uri ->
        scope.launch {
            runCatching {
                val bitmap = decodeImage(context, uri)
                withContext(Dispatchers.IO) { bitmap.toJpegBytes() }
            }
                .onSuccess { bytes -> events.onUpload(kind, bytes) }
                .onFailure {
                    Toast.makeText(
                        context,
                        R.string.place_form_image_failed,
                        Toast.LENGTH_SHORT,
                    ).show()
                }
        }
    }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) upload(pickKind, uri)
    }

    val startPick: (PlaceImageKind) -> Unit = { kind ->
        pickKind = kind
        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(FormPadding)
    ) {
        FormTitle(editing = state.mode == PlaceFormMode.Edit, onClose = events.onClose)

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = String.format(Locale.US, "%.6f, %.6f", state.latitude, state.longitude),
            color = SaltTheme.colors.subText,
            fontSize = 12.sp,
        )

        Spacer(modifier = Modifier.height(12.dp))
        FormField(
            spec = FieldSpec(
                label = stringResource(R.string.place_form_name),
                hint = stringResource(R.string.place_form_name_hint),
                value = state.name,
                onChange = events.onName,
            )
        )

        Spacer(modifier = Modifier.height(12.dp))
        TagSection(state = state, events = events)

        Spacer(modifier = Modifier.height(12.dp))
        FormField(
            spec = FieldSpec(
                label = stringResource(R.string.place_form_desc),
                hint = stringResource(R.string.place_form_desc_hint),
                value = state.desc,
                onChange = events.onDesc,
            )
        )

        Spacer(modifier = Modifier.height(12.dp))
        FormField(
            spec = FieldSpec(
                label = stringResource(R.string.place_form_cost),
                hint = stringResource(R.string.place_form_cost_hint),
                value = state.cost,
                onChange = events.onCost,
                number = true,
            )
        )

        if (state.mode == PlaceFormMode.Create) {
            Spacer(modifier = Modifier.height(12.dp))
            RatingSection(state = state, events = events)
        }

        Spacer(modifier = Modifier.height(12.dp))
        ImageSection(
            title = stringResource(R.string.place_form_exterior),
            kind = PlaceImageKind.Exterior,
            state = state,
            events = events,
            onPick = startPick,
        )

        Spacer(modifier = Modifier.height(12.dp))
        ImageSection(
            title = stringResource(R.string.place_form_menu),
            kind = PlaceImageKind.Menu,
            state = state,
            events = events,
            onPick = startPick,
        )

        state.error?.let { message ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = message, color = ErrorColor, fontSize = 13.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))
        SubmitRow(state = state, events = events)
    }
}

/** 表单顶部标题与关闭按钮。 */
@Composable
private fun FormTitle(editing: Boolean, onClose: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(
                if (editing) R.string.place_form_edit_title else R.string.place_form_add_title
            ),
            modifier = Modifier.weight(1f),
            color = SaltTheme.colors.text,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
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
                contentDescription = stringResource(R.string.place_card_close),
                modifier = Modifier.size(18.dp),
                tint = SaltTheme.colors.subText,
            )
        }
    }
}

/** 单个文本输入项的描述。 */
private class FieldSpec(
    val label: String,
    val hint: String,
    val value: String,
    val onChange: (String) -> Unit,
    val number: Boolean = false,
    val onDone: () -> Unit = {},
)

@OptIn(UnstableSaltUiApi::class)
@Composable
private fun FormField(spec: FieldSpec, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(text = spec.label, color = SaltTheme.colors.subText, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(6.dp))
        RoundedColumn(
            modifier = Modifier.fillMaxWidth(),
            paddingValues = PaddingValues(0.dp)
        ) {
            ItemEdit(
                text = spec.value,
                onChange = spec.onChange,
                hint = spec.hint,
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (spec.number) KeyboardType.Number else KeyboardType.Text,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { spec.onDone() }),
            )
        }
    }
}

/** 分类输入、已选分类与常用分类建议。 */
@OptIn(UnstableSaltUiApi::class, ExperimentalLayoutApi::class)
@Composable
private fun TagSection(state: PlaceFormState, events: PlaceFormEvents) {
    var focused by remember { mutableStateOf(false) }
    var tooLong by remember { mutableStateOf(false) }
    val suggestions = remember(state.common, state.tags) {
        state.common.filter { it !in state.tags }
    }

    val commit: (String) -> Unit = { raw ->
        tooLong = tagWeight(raw) > TagWeightLimit
        if (!tooLong) events.onAddTag(raw)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.place_form_category),
            color = SaltTheme.colors.subText,
            fontSize = 13.sp,
        )
        Spacer(modifier = Modifier.height(6.dp))
        RoundedColumn(
            modifier = Modifier.fillMaxWidth(),
            paddingValues = PaddingValues(0.dp)
        ) {
            ItemEdit(
                modifier = Modifier.onFocusChanged { focused = it.isFocused },
                text = state.tagInput,
                onChange = events.onTagInput,
                hint = stringResource(R.string.place_form_tag_hint),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { commit(state.tagInput) }
                ),
            )
        }

        if (tooLong) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.place_form_tag_too_long),
                color = ErrorColor,
                fontSize = 11.sp,
            )
        }

        if (state.tags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                state.tags.forEach { tag ->
                    TagChip(text = tag, onRemove = { events.onRemoveTag(tag) })
                }
            }
        }

        if (focused && suggestions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.place_form_common),
                color = SaltTheme.colors.subText,
                fontSize = 12.sp,
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                suggestions.forEach { tag ->
                    TagSuggestion(text = tag, onClick = { commit(tag) })
                }
            }
        }
    }
}

/** 已选分类，尾部可删除。 */
@Composable
private fun TagChip(text: String, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(TagShape)
            .background(SaltTheme.colors.popup)
            .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            color = SaltTheme.colors.text,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = stringResource(R.string.place_form_remove_tag),
                modifier = Modifier.size(14.dp),
                tint = SaltTheme.colors.subText,
            )
        }
    }
}

/** 常用分类建议。 */
@Composable
private fun TagSuggestion(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(TagShape)
            .border(1.dp, SaltTheme.colors.stroke, TagShape)
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
            text = text,
            modifier = Modifier.padding(start = 4.dp),
            color = SaltTheme.colors.text,
            fontSize = 13.sp,
            maxLines = 1,
        )
    }
}

/** 四档评价选择。 */
@Composable
private fun RatingSection(state: PlaceFormState, events: PlaceFormEvents) {
    val colors = dinnerPartySemanticColors()

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.place_form_rating),
            color = SaltTheme.colors.subText,
            fontSize = 13.sp,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            RatingTier.entries.forEach { tier ->
                RatingOption(
                    tier = tier,
                    color = ratingTierColor(tier, colors),
                    selected = tier.score() == state.rating,
                    onClick = { events.onRating(tier.score()) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.place_form_rating_tip),
            color = SaltTheme.colors.subText,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun RatingOption(
    tier: RatingTier,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(OptionShape)
            .background(if (selected) color.copy(alpha = 0.16f) else SaltTheme.colors.popup)
            .border(1.dp, if (selected) color else SaltTheme.colors.stroke, OptionShape)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = ratingTierIcon(tier),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = color,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = ratingTierLabel(tier).orEmpty(),
            color = if (selected) color else SaltTheme.colors.text,
            fontSize = 11.sp,
            maxLines = 2,
        )
    }
}

/** 一组图片，尾部是添加入口。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ImageSection(
    title: String,
    kind: PlaceImageKind,
    state: PlaceFormState,
    events: PlaceFormEvents,
    onPick: (PlaceImageKind) -> Unit,
) {
    val urls = when (kind) {
        PlaceImageKind.Exterior -> state.exterior
        PlaceImageKind.Menu -> state.menu
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = title, color = SaltTheme.colors.subText, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(6.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            urls.forEach { url ->
                ImageTile(url = url, onRemove = { events.onRemoveImage(kind, url) })
            }
            ImageAddTile(
                uploading = state.uploading > 0,
                kind = kind,
                onPick = onPick,
            )
        }
    }
}

@Composable
private fun ImageTile(url: String, onRemove: () -> Unit) {
    Box(
        modifier = Modifier
            .size(TileSize)
            .clip(TileShape)
            .border(1.dp, SaltTheme.colors.stroke, TileShape)
    ) {
        AsyncImage(
            model = ApiClient.assetUrl(url),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(2.dp)
                .size(20.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = stringResource(R.string.place_form_remove_image),
                modifier = Modifier.size(14.dp),
                tint = Color.White,
            )
        }
    }
}

@Composable
private fun ImageAddTile(
    uploading: Boolean,
    kind: PlaceImageKind,
    onPick: (PlaceImageKind) -> Unit,
) {
    Box {
        Column(
            modifier = Modifier
                .size(TileSize)
                .clip(TileShape)
                .border(1.dp, SaltTheme.colors.stroke, TileShape)
                .clickable(enabled = !uploading) { onPick(kind) },
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (uploading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = SaltTheme.colors.highlight,
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = SaltTheme.colors.subText,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.place_form_photo),
                    color = SaltTheme.colors.subText,
                    fontSize = 10.sp,
                )
            }
        }
    }
}

/** 取消与提交。 */
@Composable
private fun SubmitRow(state: PlaceFormState, events: PlaceFormEvents) {
    val enabled = state.name.isNotBlank() && !state.busy && state.uploading == 0

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            modifier = Modifier.weight(1f),
            type = ButtonType.Sub,
            text = stringResource(R.string.action_cancel),
            onClick = events.onClose,
        )
        Button(
            modifier = Modifier.weight(1f),
            text = if (state.busy) {
                stringResource(R.string.place_form_submitting)
            } else {
                stringResource(
                    if (state.mode == PlaceFormMode.Edit) {
                        R.string.action_save
                    } else {
                        R.string.place_form_submit
                    }
                )
            },
            enabled = enabled,
            onClick = events.onSubmit,
        )
    }
}

/** 分类名权重，汉字算 2 其余算 1，与后端长度校验一致。 */
private fun tagWeight(tag: String): Int =
    tag.trim().sumOf { if (it.code > 0x7f) 2 else 1 }

private val FormPadding = 16.dp
private val CloseSize = 24.dp
private val TileSize = 76.dp
private val TileShape = RoundedCornerShape(10.dp)
private val TagShape = RoundedCornerShape(50)
private val OptionShape = RoundedCornerShape(10.dp)
private val ErrorColor = Color(0xFFD9534F)
private const val SheetMaxRatio = 0.92f
private const val TagWeightLimit = 16

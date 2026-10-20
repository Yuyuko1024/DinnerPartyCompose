package cc.dinnerparty.compose.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cc.dinnerparty.compose.api.model.Place
import cc.dinnerparty.compose.api.model.PlaceDraft
import cc.dinnerparty.compose.data.model.defaultScore
import cc.dinnerparty.compose.data.repo.MediaRepo
import cc.dinnerparty.compose.data.repo.PlaceRepo
import com.google.gson.JsonNull
import com.google.gson.JsonPrimitive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 表单用途。 */
enum class PlaceFormMode { Create, Edit }

/** 可上传的图片分组。 */
enum class PlaceImageKind { Exterior, Menu }

/** 添加或修改地点的表单状态。 */
data class PlaceFormState(
    val mode: PlaceFormMode = PlaceFormMode.Create,
    val placeId: Long? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val address: String = "",
    val name: String = "",
    val tags: List<String> = emptyList(),
    val tagInput: String = "",
    val common: List<String> = emptyList(),
    val desc: String = "",
    val cost: String = "",
    val rating: Int = 4,
    val ratingPicked: Boolean = false,
    val exterior: List<String> = emptyList(),
    val menu: List<String> = emptyList(),
    val uploading: Int = 0,
    val busy: Boolean = false,
    val error: String? = null,
    val done: Boolean = false,
)

/** 维护地点表单的字段、分类建议与提交。 */
class PlaceFormViewModel(
    private val repo: PlaceRepo = PlaceRepo(),
    private val media: MediaRepo = MediaRepo(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaceFormState())
    val uiState: StateFlow<PlaceFormState> = _uiState.asStateFlow()

    /** 以拾取到的坐标开始新建，name 与 address 可能为空。 */
    fun openCreate(lat: Double, lng: Double, name: String, address: String) {
        _uiState.value = PlaceFormState(
            latitude = lat,
            longitude = lng,
            name = name,
            address = address,
            common = _uiState.value.common,
        )
        loadCommon()
    }

    /** 以已有地点开始修改。 */
    fun openEdit(place: Place) {
        _uiState.value = PlaceFormState(
            mode = PlaceFormMode.Edit,
            placeId = place.id ?: error("place id missing"),
            latitude = place.latitude ?: error("latitude missing"),
            longitude = place.longitude ?: error("longitude missing"),
            name = place.name.orEmpty(),
            tags = splitTags(place.category),
            desc = place.description.orEmpty(),
            cost = place.perPersonCost?.toString().orEmpty(),
            ratingPicked = true,
            exterior = place.exteriorImages.orEmpty(),
            menu = place.menuImages.orEmpty(),
            common = _uiState.value.common,
        )
        loadCommon()
    }

    /** 收起时清掉一次性标记。 */
    fun close() {
        _uiState.update { it.copy(done = false, error = null) }
    }

    fun updateName(value: String) {
        _uiState.update { it.copy(name = value) }
    }

    fun updateDesc(value: String) {
        _uiState.update { it.copy(desc = value) }
    }

    fun updateCost(value: String) {
        val digits = value.filter { it.isDigit() }.take(CostLimit)
        _uiState.update { it.copy(cost = digits) }
    }

    fun updateTagInput(value: String) {
        _uiState.update { it.copy(tagInput = value) }
    }

    /** 输入框回车时追加分类，带避雷分类会自动切到最低档。 */
    fun addTag(raw: String) {
        val tag = raw.trim().replace(",", "").replace("，", "")
        if (tag.isEmpty()) return

        _uiState.update { state ->
            if (tag in state.tags) {
                state.copy(tagInput = "")
            } else {
                val tags = state.tags + tag
                state.copy(
                    tags = tags,
                    tagInput = "",
                    rating = if (state.ratingPicked) state.rating else defaultScore(tags),
                )
            }
        }
    }

    fun removeTag(tag: String) {
        _uiState.update { state ->
            val tags = state.tags - tag
            state.copy(
                tags = tags,
                rating = if (state.ratingPicked) state.rating else defaultScore(tags),
            )
        }
    }

    fun chooseRating(score: Int) {
        _uiState.update { it.copy(rating = score, ratingPicked = true) }
    }

    /** 上传一张图片，成功后就近追加到对应分组。 */
    fun upload(kind: PlaceImageKind, bytes: ByteArray) {
        _uiState.update { it.copy(uploading = it.uploading + 1, error = null) }
        viewModelScope.launch {
            runCatching { media.upload(bytes) }
                .onSuccess { url ->
                    _uiState.update { state ->
                        when (kind) {
                            PlaceImageKind.Exterior -> state.copy(exterior = state.exterior + url)
                            PlaceImageKind.Menu -> state.copy(menu = state.menu + url)
                        }
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message) }
                }
            _uiState.update { it.copy(uploading = (it.uploading - 1).coerceAtLeast(0)) }
        }
    }

    fun removeImage(kind: PlaceImageKind, url: String) {
        _uiState.update { state ->
            when (kind) {
                PlaceImageKind.Exterior -> state.copy(exterior = state.exterior - url)
                PlaceImageKind.Menu -> state.copy(menu = state.menu - url)
            }
        }
    }

    /** 提交表单，成功后置 done。 */
    fun submit() {
        val state = _uiState.value
        if (state.busy) return

        _uiState.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            runCatching {
                val draft = state.toDraft()
                when (state.mode) {
                    PlaceFormMode.Create -> repo.createPlace(draft)
                    PlaceFormMode.Edit -> repo.updatePlace(
                        state.placeId ?: error("place id missing"),
                        draft,
                    )
                }
            }
                .onSuccess {
                    _uiState.update { it.copy(busy = false, done = true) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(busy = false, error = error.message) }
                }
        }
    }

    /** 拉一次常用分类，作为分类建议。 */
    private fun loadCommon() {
        if (_uiState.value.common.isNotEmpty()) return

        viewModelScope.launch {
            runCatching { repo.getCategories() }
                .onSuccess { list ->
                    val names = list
                        .filter { it.isCommon == true }
                        .sortedBy { it.sortOrder ?: Int.MAX_VALUE }
                        .mapNotNull { it.name?.takeIf(String::isNotBlank) }
                        .distinct()
                    _uiState.update { it.copy(common = names) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(error = error.message) }
                }
        }
    }
}

private fun splitTags(category: String?): List<String> =
    category.orEmpty()
        .split(TagSplit)
        .map { it.trim() }
        .filter { it.isNotEmpty() }

private fun PlaceFormState.toDraft(): PlaceDraft = PlaceDraft(
    name = name.trim(),
    category = tags.joinToString(", "),
    description = desc.trim(),
    latitude = latitude,
    longitude = longitude,
    perPersonCost = cost.toIntOrNull()?.let(::JsonPrimitive) ?: JsonNull.INSTANCE,
    address = address.takeIf { it.isNotBlank() },
    rating = if (mode == PlaceFormMode.Create) rating else null,
    exteriorImages = exterior,
    menuImages = menu,
)

private const val CostLimit = 6

private val TagSplit = Regex("[,，]")

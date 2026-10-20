package cc.dinnerparty.compose.api.model

import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.annotations.SerializedName

/** 新建或修改地点的请求体。 */
data class PlaceDraft(
    val name: String,
    val category: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    @SerializedName("per_person_cost")
    val perPersonCost: JsonElement = JsonNull.INSTANCE,
    val address: String? = null,
    val rating: Int? = null,
    @SerializedName("exterior_images")
    val exteriorImages: List<String> = emptyList(),
    @SerializedName("menu_images")
    val menuImages: List<String> = emptyList(),
)

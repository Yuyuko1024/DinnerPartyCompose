package cc.dinnerparty.compose.api.model

/** 某位住民的收藏夹 最近收藏的在前 */
data class UserFavoritesResponse(
    val places: List<ProfilePlace>? = null,
)

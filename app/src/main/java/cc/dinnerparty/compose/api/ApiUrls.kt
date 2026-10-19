package cc.dinnerparty.compose.api

import cc.dinnerparty.compose.api.model.User

/** 住民头像地址，没上传过头像或缺少 id 时为 null。 */
internal fun userAvatarUrl(user: User, avatarVersion: Long = 0L): String? {
    if (!user.hasAvatar) return null
    val id = user.id ?: return null
    return "${ApiClient.BASE_URL}users/$id/avatar?v=$avatarVersion"
}

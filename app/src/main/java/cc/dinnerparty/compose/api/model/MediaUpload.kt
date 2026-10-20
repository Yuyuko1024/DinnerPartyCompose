package cc.dinnerparty.compose.api.model

/** 图片上传结果，url 可能是相对路径。 */
data class MediaUpload(
    val url: String? = null,
)

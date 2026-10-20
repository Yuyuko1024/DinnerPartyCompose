package cc.dinnerparty.compose.data.repo

import cc.dinnerparty.compose.api.ApiClient
import cc.dinnerparty.compose.api.apiCall
import cc.dinnerparty.compose.api.apis.MediaApi
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

/** 图片上传入口，返回可直接展示的完整地址。 */
class MediaRepo {

    private val mediaApi: MediaApi by lazy { ApiClient.createService(MediaApi::class.java) }

    suspend fun upload(bytes: ByteArray): String {
        val body = bytes.toRequestBody(JpegType)
        val part = MultipartBody.Part.createFormData("image", "place.jpg", body)
        val url = apiCall { mediaApi.upload(part) }.url
            ?: error("upload url missing")
        return ApiClient.assetUrl(url)
    }
}

private val JpegType = "image/jpeg".toMediaType()
